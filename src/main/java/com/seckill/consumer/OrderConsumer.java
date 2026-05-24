package com.seckill.consumer;

import com.alibaba.fastjson2.JSON;
import com.seckill.entity.SeckillOrder;
import com.seckill.mapper.SeckillGoodsMapper;
import com.seckill.mapper.SeckillOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * 订单消费者——从 Redis 队列中异步取出订单，写入 MySQL
 *
 * 为什么需要异步消费？
 * 秒杀的瞬时并发极高（比如 10 万人抢 100 件商品），如果每个请求都在秒杀接口里
 * 同步写 MySQL，数据库连接池会瞬间被打满，整个系统不可用。
 * 解决方案：
 * 1. 秒杀接口只做 Redis 操作（极快），把订单推入 Redis List 队列
 * 2. 这个消费者在后台独立线程中，从队列里一个一个取出订单，慢慢写入 MySQL
 * 3. 用 Redisson 分布式锁保证同一用户同一商品不会重复落库
 *
 * 启动方式：@PostConstruct 自动启动消费线程，随 Spring 容器启动
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private final StringRedisTemplate stringRedisTemplate;
    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final RedissonClient redissonClient;

    private static final String ORDER_QUEUE = "seckill:order:queue";
    /** volatile 保证多线程之间的可见性，需要停止时设为 false */
    private volatile boolean running = true;
    private Thread consumerThread;

    /**
     * Spring Bean 初始化后自动启动消费线程
     * 注意：这里直接 new Thread 是为了简化，生产环境请用线程池
     */
    @PostConstruct
    public void startConsumer() {
        consumerThread = new Thread(this::consume, "order-consumer");
        consumerThread.start();
    }

    /** 应用关闭时先停消费线程，再关闭 Redis 连接，避免 "Connection closed" 报错 */
    @PreDestroy
    public void stopConsumer() {
        running = false;
        if (consumerThread != null) {
            consumerThread.interrupt();
            try {
                consumerThread.join(3000);  // 最多等 3 秒
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 消费主循环——阻塞读取 Redis List
     * leftPop 的 timeout=2秒：等 2 秒还没新订单就返回 null，
     * 然后继续下一轮循环。这样停服务时线程能及时退出
     */
    public void consume() {
        while (running) {
            try {
                // 从队列左端阻塞取出订单（先进先出），最多等 2 秒
                String json = stringRedisTemplate.opsForList()
                        .leftPop(ORDER_QUEUE, 2, TimeUnit.SECONDS);
                if (json == null) continue;

                SeckillOrder order = JSON.parseObject(json, SeckillOrder.class);
                processOrder(order);

            } catch (Exception e) {
                log.error("消费订单异常", e);
            }
        }
    }
    /**
     * 处理单个订单——加锁 → 校验 → 扣库存 → 写订单
     * 加锁原因：虽然 Lua 脚本已经做了去重（user Set），但 Redis 数据有过期时间。
     * 万一库存 Redis key 到期了但订单还没消费完，用户可能再次秒杀同一商品。
     * 这里用分布式锁做数据库层面的兜底，保证同一用户同一商品只入库一次。
     */
    private void processOrder(SeckillOrder order) {
        // Redisson 分布式锁：lock:order:{userId}:{goodsId}
        RLock lock = redissonClient.getLock("lock:order:" + order.getUserId() + ":" + order.getGoodsId());
        try {
            // tryLock：最多等 3 秒，锁持有时间 5 秒
            if (!lock.tryLock(3, 5, TimeUnit.SECONDS)) {
                log.warn("获取锁失败，订单重入队");
                // 没抢到锁说明同一订单正在处理，重新放回队列
                stringRedisTemplate.opsForList().rightPush(ORDER_QUEUE, JSON.toJSONString(order));
                return;
            }

            // 二次校验：检查库存 key 是否还存在（可能已过期）
            String stock = stringRedisTemplate.opsForValue()
                    .get("seckill:stock:" + order.getGoodsId());
            if (stock == null) {
                log.warn("库存 key 已过期，秒杀已结束 userId={}", order.getUserId());
                return;
            }

            // 扣减 MySQL 库存（乐观锁：UPDATE ... SET stock = stock - 1 WHERE stock > 0）
            int affected = seckillGoodsMapper.deductStock(order.getGoodsId());
            if (affected == 0) {
                log.warn("数据库库存不足 userId={}", order.getUserId());
                return;
            }

            // 写入订单记录
            seckillOrderMapper.insert(order);
            log.info("订单入库成功 orderId={} userId={} goodsId={}",
                    order.getId(), order.getUserId(), order.getGoodsId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("订单处理失败 userId={}", order.getUserId(), e);
        } finally {
            // 解锁——务必在 finally 里，防止异常导致死锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
