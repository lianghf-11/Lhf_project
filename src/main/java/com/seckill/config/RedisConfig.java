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

@Configuration
public class RedisConfig {

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

    @Bean
    public DefaultRedisScript<Long> seckillScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/seckill.lua"));
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 基于 Fastjson2 的 Redis 序列化器，原生支持 LocalDateTime 等 Java 8 时间类型
     */
    static class FastJson2RedisSerializer implements RedisSerializer<Object> {

        @Override
        public byte[] serialize(Object obj) {
            if (obj == null) return new byte[0];
            String className = obj.getClass().getName();
            return JSON.toJSONBytes(new Object[]{className, obj});
        }

        @Override
        public Object deserialize(byte[] bytes) {
            if (bytes == null || bytes.length == 0) return null;
            Object[] arr = JSON.parseObject(bytes, Object[].class);
            if (arr == null || arr.length < 2) return null;
            Class<?> clazz;
            try {
                clazz = Class.forName((String) arr[0]);
            } catch (ClassNotFoundException e) {
                return arr[1];
            }
            return JSON.parseObject(arr[1].toString(), clazz);
        }
    }
}
