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

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> seckillScript;
    private final RedissonClient redissonClient;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final SeckillOrderMapper seckillOrderMapper;

    private static final String STOCK_KEY = "seckill:stock:";
    private static final String USER_KEY = "seckill:user:";
    private static final String ORDER_QUEUE = "seckill:order:queue";
    private static final String GOODS_CACHE = "seckill:goods:";
    private static final String BLOOM_KEY = "seckill:bloom:soldout";

    private RBloomFilter<Long> soldOutBloomFilter;

    @PostConstruct
    public void initBloomFilter() {
        soldOutBloomFilter = redissonClient.getBloomFilter(BLOOM_KEY);
        soldOutBloomFilter.tryInit(1000, 0.03);
    }

    @Override
    public Result<String> seckill(Long goodsId, Long userId) {
        if (soldOutBloomFilter.contains(goodsId)) {
            return Result.fail(400, "商品已售罄");
        }

        // Lua 脚本走 StringRedisTemplate，确保 Redis 里存的是纯字符串
        Long result = stringRedisTemplate.execute(
                seckillScript,
                Arrays.asList(STOCK_KEY + goodsId, USER_KEY + goodsId),
                String.valueOf(userId)
        );

        if (result == null || result == -1) {
            return Result.fail(400, "您已参与过该秒杀");
        }
        if (result == 0) {
            soldOutBloomFilter.add(goodsId);
            return Result.fail(400, "库存不足，秒杀已结束");
        }

        SeckillOrder order = new SeckillOrder();
        order.setUserId(userId);
        order.setGoodsId(goodsId);
        order.setCreateTime(LocalDateTime.now());
        stringRedisTemplate.opsForList().rightPush(ORDER_QUEUE, JSON.toJSONString(order));

        log.info("秒杀成功 userId={} goodsId={}", userId, goodsId);
        return Result.ok("秒杀成功，订单生成中...");
    }

    @Override
    public void loadStockToRedis(Long goodsId) {
        SeckillGoods goods = seckillGoodsMapper.selectById(goodsId);
        if (goods == null) return;
        // 库存存 String 类型，Lua 脚本 tonumber 可读取
        stringRedisTemplate.opsForValue().set(STOCK_KEY + goodsId,
                String.valueOf(goods.getStock()), Duration.ofHours(2));
        stringRedisTemplate.delete(USER_KEY + goodsId);
        log.info("库存预热 goodsId={} stock={}", goodsId, goods.getStock());
    }

    @Override
    public Result<SeckillGoods> getGoodsDetail(Long goodsId) {
        SeckillGoods goods = (SeckillGoods) redisTemplate.opsForValue()
                .get(GOODS_CACHE + goodsId);
        if (goods != null) {
            return Result.ok(goods);
        }

        // 互斥锁（setIfAbsent）
        String lockKey = "lock:goods:" + goodsId;
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(5));
        if (Boolean.TRUE.equals(locked)) {
            try {
                goods = (SeckillGoods) redisTemplate.opsForValue()
                        .get(GOODS_CACHE + goodsId);
                if (goods != null) {
                    return Result.ok(goods);
                }

                goods = seckillGoodsMapper.selectById(goodsId);
                if (goods == null) {
                    return Result.fail("商品不存在");
                }

                // 缓存对象用 RedisTemplate（Fastjson2 序列化）
                redisTemplate.opsForValue().set(
                        GOODS_CACHE + goodsId, goods,
                        Duration.ofMinutes(30 + (long) (Math.random() * 10))
                );
                return Result.ok(goods);
            } finally {
                stringRedisTemplate.delete(lockKey);
            }
        }
        log.warn("获取缓存锁失败 goodsId={}", goodsId);
        return Result.fail("系统繁忙，请稍后重试");
    }
}
