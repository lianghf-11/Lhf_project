package com.seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 秒杀系统启动类
 *
 * @EnableAsync  启用异步处理（下单后异步落库）
 * @EnableScheduling  启用定时任务（可用于定时预热库存）
 * @MapperScan   扫描 MyBatis-Plus 的 Mapper 接口
 */
@EnableAsync
@EnableScheduling
@MapperScan("com.seckill.mapper")
@SpringBootApplication
public class SeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }
}
