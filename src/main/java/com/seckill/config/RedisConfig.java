package com.seckill.config;

import com.alibaba.fastjson2.JSON;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 *
 * 序列化方案：
 * - Key 用 StringRedisSerializer（可读性高）
 * - Value 用 Fastjson2 序列化（原生支持 LocalDateTime 等 Java 8 时间类型，
 *   不需要额外配置 Jackson 的 JavaTimeModule）
 *
 * 为什么不用 GenericJackson2JsonRedisSerializer？
 * 它内置的 ObjectMapper 没有注册 JavaTimeModule，序列化 LocalDateTime 会报错。
 * 需要 Spring Data Redis 3.x（Spring Boot 3.x）才能通过构造函数传入自定义 ObjectMapper。
 */
@Configuration
public class RedisConfig {

    /**
     * 自定义 RedisTemplate：Fastjson2 序列化，专门用于对象缓存
     * 适用场景：SeckillGoods 实体的缓存，字段包含 LocalDateTime、BigDecimal
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new FastJson2RedisSerializer());
        template.setHashValueSerializer(new FastJson2RedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Lua 脚本 Bean
     * 脚本路径：resources/lua/seckill.lua
     * 返回类型：Long（-1=重复购买  0=库存不足  1=秒杀成功）
     * 执行时用 StringRedisTemplate，确保传给 Redis 的是原始字符串
     */
    @Bean
    public DefaultRedisScript<Long> seckillScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/seckill.lua"));
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 基于 Fastjson2 的 Redis 序列化器
     *
     * 序列化格式：[className, jsonData]
     * 包含类名是为了反序列化时能找到正确的类型
     *
     * 注意：这个序列化器仅用于 RedisTemplate<String, Object>（对象缓存），
     * 库存、已购集合、订单队列等 Lua 脚本操作的 key 必须走 StringRedisTemplate，
     * 因为 Lua 脚本需要读到原始的字符串/数字
     */
    static class FastJson2RedisSerializer implements RedisSerializer<Object> {

        /** 序列化：把 Java 对象转成 Redis 字节 */
        @Override
        public byte[] serialize(Object obj) {
            if (obj == null) return new byte[0];
            // 把类名和 JSON 数据打包成一个数组，反序列化时才知道目标类型
            String className = obj.getClass().getName();
            return JSON.toJSONBytes(new Object[]{className, obj});
        }

        /** 反序列化：把 Redis 字节还原为 Java 对象 */
        @Override
        public Object deserialize(byte[] bytes) {
            if (bytes == null || bytes.length == 0) return null;
            Object[] arr = JSON.parseObject(bytes, Object[].class);
            if (arr == null || arr.length < 2) return null;
            Class<?> clazz;
            try {
                clazz = Class.forName((String) arr[0]);
            } catch (ClassNotFoundException e) {
                return arr[1]; // 类找不到时返回原始 JSON 对象
            }
            return JSON.parseObject(arr[1].toString(), clazz);
        }
    }
}
