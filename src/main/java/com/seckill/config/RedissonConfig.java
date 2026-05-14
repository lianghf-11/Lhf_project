package com.seckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置——Redis 的高级客户端
 *
 * Redisson 在 Redis 基础命令之上封装了：
 * - 分布式锁（RLock）——订单处理的幂等保障
 * - 布隆过滤器（RBloomFilter）——拒绝已售罄/不存在的商品
 * - 连接池管理——Netty 连接池，高性能异步通信
 *
 * Spring Data Redis（Lettuce）负责基础操作（get/set/decr/sadd/lpush）
 * Redisson 负责高级功能（锁、布隆过滤器）
 * 两者互补，可以同时使用
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private String port;

    // 没有密码时默认为空字符串
    @Value("${spring.redis.password:}")
    private String password;

    /**
     * 创建 Redisson 客户端
     * 单机模式：适合开发环境和中小规模部署
     * 集群环境需要改用 config.useClusterServers()
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        // redis://localhost:6379
        String address = "redis://" + host + ":" + port;
        config.useSingleServer().setAddress(address);
        if (password != null && !password.isEmpty()) {
            config.useSingleServer().setPassword(password);
        }
        return Redisson.create(config);
    }
}
