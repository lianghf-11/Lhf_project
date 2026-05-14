package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀商品实体类（对应 seckill_goods 表）
 *
 * BigDecaimal 用于金额：浮点数（float/double）计算会有精度问题，
 * 0.1 + 0.2 可能等于 0.30000000000000004，
 * 金额必须用 BigDecimal，避免算错钱
 */
@Data
@TableName("seckill_goods")
public class SeckillGoods {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    /** 秒杀价格，BigDecimal 保证精度 */
    private BigDecimal price;
    /** 剩余库存数量 */
    private Integer stock;
    /** 秒杀开始时间 */
    private LocalDateTime startTime;
    /** 秒杀结束时间 */
    private LocalDateTime endTime;

    /**
     * 判断当前时间是否在秒杀时间窗口内
     */
    public boolean isInTimeWindow() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startTime) && now.isBefore(endTime);
    }
}
