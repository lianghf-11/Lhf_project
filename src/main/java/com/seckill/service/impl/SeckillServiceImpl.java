package com.seckill.service.impl;

import com.alibaba.fastjson2.JSON;
import com.seckill.dto.Result;
import com.seckill.entity.SeckillGoods;
import com.seckill.entity.SeckillOrder;
import com.seckill.mapper.SeckillGoodsMapper;
import com.seckill.mapper.SeckillOrderMapper;
import com.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 秒杀核心业务实现
 *
 * 整体流程：
 * 1. 管理员预热库存 → 把 MySQL 商品库存加载到 Redis（String 类型）
 * 2. 用户秒杀请求 → 布隆过滤器快速拒绝 → Lua 脚本原子扣库存 → 异步推入订单队列
 * 3. 订单消费者 → 从队列拉取订单 → 分布式锁防重 → 落库 MySQL
 *
 * 缓存穿透：布隆过滤器，不存在的商品 ID 直接拒绝
 * 缓存击穿：互斥锁（setIfAbsent），同一时刻只有一个线程能查库
 * 缓存雪崩：随机 TTL，避免大批缓存同时过期
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    // RedisTemplate：用于对象缓存（走 Fastjson2 序列化，支持 LocalDateTime）
    private final RedisTemplate<String, Object> redisTemplate;
    // StringRedisTemplate：用于 Lua 脚本、库存、队列等需要纯字符串的场景
    private final StringRedisTemplate stringRedisTemplate;
    // Lua 脚本：原子操作（查库存 + 去重 + 扣减），在 Redis 服务端单线程执行
    private final DefaultRedisScript<Long> seckillScript;
    // Redisson 客户端：布隆过滤器 + 分布式锁
    private final RedissonClient redissonClient;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final SeckillOrderMapper seckillOrderMapper;

    // Redis Key 前缀常量
    private static final String STOCK_KEY = "seckill:stock:";       // 库存（String，Lua 脚本读取）
    private static final String USER_KEY = "seckill:user:";         // 已购用户集合（Set，Lua 脚本操作）
    private static final String ORDER_QUEUE = "seckill:order:queue"; // 订单队列（List）
    private static final String GOODS_CACHE = "seckill:goods:";      // 商品详情缓存（Object，Fastjson2 序列化）
    private static final String BLOOM_KEY = "seckill:bloom:soldout"; // 售罄商品的布隆过滤器

    /** 布隆过滤器：记录已售罄的商品 ID，拦截无效请求，防止缓存穿透 */
    private RBloomFilter<Long> soldOutBloomFilter;

    /** 项目启动时初始化布隆过滤器（误判率 3%，容量 1000） */
    @PostConstruct
    public void initBloomFilter() {
        soldOutBloomFilter = redissonClient.getBloomFilter(BLOOM_KEY);
        // tryInit = 只有当这个 key 不存在时才初始化，防止重启后覆盖已有的过滤器
        soldOutBloomFilter.tryInit(1000, 0.03);
    }

    /**
     * 秒杀核心方法
     *
     * 调用链：布隆过滤 → Lua 原子操作 → 异步下单（推入 Redis List）
     *
     * 为什么不直接在 Java 里"查库存 → 判断 → 扣减"？
     * 因为"查→判→扣"是三步独立的 Redis 操作，并发时 A 查完还没扣，B 也查了同样的库存，
     * 两个人都判断有库存，都扣减 → 超卖。Lua 脚本在 Redis 服务端单线程执行，三步合一，天然原子。
     */
    @Override
    public Result<String> seckill(Long goodsId, Long userId) {
        // 第一步：布隆过滤器快速拒绝已售罄的商品（避免穿透到 Redis/MySQL）
        if (soldOutBloomFilter.contains(goodsId)) {
            return Result.fail(400, "商品已售罄");
        }

        // 第二步：执行 Lua 脚本——检查是否重复购买 + 检查库存 + 扣减库存 + 记录用户
        // KEYS[1] = seckill:stock:{goodsId}   —— 库存值
        // KEYS[2] = seckill:user:{goodsId}    —— 已购用户的 Set
        // ARGV[1] = userId                     —— 当前用户 ID
        Long result = stringRedisTemplate.execute(
                seckillScript,
                Arrays.asList(STOCK_KEY + goodsId, USER_KEY + goodsId),
                String.valueOf(userId)
        );

        // Lua 脚本返回值：-1=已购买过  0=库存不足  1=秒杀成功
        if (result == null || result == -1) {
            return Result.fail(400, "您已参与过该秒杀");
        }
        if (result == 0) {
            // 库存刚好归零时，加入布隆过滤器，后续请求直接拒绝
            soldOutBloomFilter.add(goodsId);
            return Result.fail(400, "库存不足，秒杀已结束");
        }

        // 第三步：Redis 扣减成功后，将订单推入队列，由消费者异步落库
        // 为什么不直接写 MySQL？因为秒杀瞬时并发极高，如果每个请求都等数据库 IO，
        // 数据库连接池瞬间打满，整个系统崩溃。先写 Redis 队列，让数据库慢慢消费
        SeckillOrder order = new SeckillOrder();
        order.setUserId(userId);
        order.setGoodsId(goodsId);
        order.setCreateTime(LocalDateTime.now());
        stringRedisTemplate.opsForList().rightPush(ORDER_QUEUE, JSON.toJSONString(order));

        log.info("秒杀成功 userId={} goodsId={}", userId, goodsId);
        return Result.ok("秒杀成功，订单生成中...");
    }

    /**
     * 库存预热：将 MySQL 中的商品库存加载到 Redis
     *
     * 为什么需要预热？秒杀请求只读 Redis 不读 MySQL，如果 Redis 里没有库存数据，
     * Lua 脚本会读到 0（key 不存在时 tonumber 返回 0），所有请求都会被拒绝
     */
    @Override
    public void loadStockToRedis(Long goodsId) {
        SeckillGoods goods = seckillGoodsMapper.selectById(goodsId);
        if (goods == null) return;
        // 库存用 String 类型存储，这样 Lua 脚本的 tonumber() 才能正确读取
        stringRedisTemplate.opsForValue().set(STOCK_KEY + goodsId,
                String.valueOf(goods.getStock()), Duration.ofHours(2));
        // 清理同一商品之前的已购用户集合（防止上一场秒杀的数据残留）
        stringRedisTemplate.delete(USER_KEY + goodsId);
        log.info("库存预热 goodsId={} stock={}", goodsId, goods.getStock());
    }

    /**
     * 查询商品详情——三级缓存策略
     *
     * 第一级：查 Redis 缓存（命中率 > 95%）
     * 第二级：缓存未命中，加互斥锁（setIfAbsent），拿到锁的线程查 MySQL
     * 第三级：查不到说明商品不存在，直接返回错误
     *
     * 缓存穿透防护：如果商品在 MySQL 也不存在，原本不缓存的话每次都会查库。
     *                所以这里直接返回"商品不存在"（不缓存空值，依靠布隆过滤器防穿透）
     * 缓存击穿防护：热点 key 过期瞬间，大量请求同时查库 → 用 setIfAbsent 互斥锁，
     *               只有一个线程能拿到锁去查数据库，其余等待或快速失败
     * 缓存雪崩防护：设置缓存 TTL 时加上随机值（30min + 0~10min 随机），
     *               避免大量缓存同时过期瞬间压垮数据库
     */
    @Override
    public Result<SeckillGoods> getGoodsDetail(Long goodsId) {
        // 第一级：先查 Redis 缓存（用 Fastjson2 反序列化，LocalDateTime 能正确还原）
        SeckillGoods goods = (SeckillGoods) redisTemplate.opsForValue()
                .get(GOODS_CACHE + goodsId);
        if (goods != null) {
            return Result.ok(goods);
        }

        // 第二级：缓存未命中，使用 setIfAbsent（Redis SETNX）做互斥锁
        // 只有第一个线程能设置成功（返回 true），其他线程直接返回"系统繁忙"
        String lockKey = "lock:goods:" + goodsId;
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(5));
        if (Boolean.TRUE.equals(locked)) {
            try {
                // 双重检查：拿到锁后再查一次缓存（可能上一个线程已经查过库并回填了）
                goods = (SeckillGoods) redisTemplate.opsForValue()
                        .get(GOODS_CACHE + goodsId);
                if (goods != null) {
                    return Result.ok(goods);
                }

                // 第三级：查 MySQL
                goods = seckillGoodsMapper.selectById(goodsId);
                if (goods == null) {
                    return Result.fail("商品不存在");
                }

                // 回填缓存 + 随机 TTL（防雪崩）
                // TTL = 30 分钟 + 随机 0~10 分钟，让缓存不会集中在同一秒过期
                redisTemplate.opsForValue().set(
                        GOODS_CACHE + goodsId, goods,
                        Duration.ofMinutes(30 + (long) (Math.random() * 10))
                );
                return Result.ok(goods);
            } finally {
                // 必须在 finally 中释放锁，即使查库异常也不能死锁
                stringRedisTemplate.delete(lockKey);
            }
        }
        // 没抢到锁，直接返回"系统繁忙"（防止大量请求阻塞等待锁）
        log.warn("获取缓存锁失败 goodsId={}", goodsId);
        return Result.fail("系统繁忙，请稍后重试");
    }
}
