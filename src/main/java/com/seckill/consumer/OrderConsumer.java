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
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private final StringRedisTemplate stringRedisTemplate;
    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final RedissonClient redissonClient;

    private static final String ORDER_QUEUE = "seckill:order:queue";
    private volatile boolean running = true;

    @PostConstruct
    public void startConsumer() {
        new Thread(this::consume, "order-consumer").start();
    }

    public void consume() {
        while (running) {
            try {
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

    private void processOrder(SeckillOrder order) {
        RLock lock = redissonClient.getLock("lock:order:" + order.getUserId() + ":" + order.getGoodsId());
        try {
            if (!lock.tryLock(3, 5, TimeUnit.SECONDS)) {
                log.warn("获取锁失败，订单重入队");
                stringRedisTemplate.opsForList().rightPush(ORDER_QUEUE, JSON.toJSONString(order));
                return;
            }

            // 检查库存 key 是否存在（防止 Redis 数据过期后仍下单）
            String stock = stringRedisTemplate.opsForValue()
                    .get("seckill:stock:" + order.getGoodsId());
            if (stock == null) {
                log.warn("库存 key 已过期，秒杀已结束 userId={}", order.getUserId());
                return;
            }

            // 落库 MySQL
            int affected = seckillGoodsMapper.deductStock(order.getGoodsId());
            if (affected == 0) {
                log.warn("数据库库存不足 userId={}", order.getUserId());
                return;
            }

            seckillOrderMapper.insert(order);
            log.info("订单入库成功 orderId={} userId={} goodsId={}",
                    order.getId(), order.getUserId(), order.getGoodsId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("订单处理失败 userId={}", order.getUserId(), e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
