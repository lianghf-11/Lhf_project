package com.seckill.service;

import com.seckill.dto.Result;

/**
 * 秒杀业务接口
 */
public interface SeckillService {

    /** 执行秒杀（布隆过滤 → Lua原子扣库存 → 异步下单） */
    Result<String> seckill(Long goodsId, Long userId);

    /** 将商品库存从 MySQL 预热到 Redis */
    void loadStockToRedis(Long goodsId);

    /** 查询商品详情（带三级缓存：缓存查 → 互斥锁 → 查库回填） */
    Result<?> getGoodsDetail(Long goodsId);
}
