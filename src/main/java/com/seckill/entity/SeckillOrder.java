package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀订单实体类（对应 seckill_order 表）
 *
 * 注意：订单表里有 goods_name 和 price 字段做快照，
 * 防止商品信息变更后历史订单数据被污染。
 * 当前秒杀流程中没有填入这两个字段，是一个可优化的点
 */
@Data
@TableName("seckill_order")
public class SeckillOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 购买用户 ID */
    private Long userId;
    /** 秒杀商品 ID */
    private Long goodsId;
    /** 商品名称快照（防止商品改名后历史订单数据变化） */
    private String goodsName;
    /** 价格快照 */
    private BigDecimal price;
    /** 订单状态：1=已下单  2=已支付  3=已取消 */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createTime;
}
