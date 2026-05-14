package com.seckill.service;

import com.seckill.dto.Result;

public interface SeckillService {

    Result<String> seckill(Long goodsId, Long userId);

    void loadStockToRedis(Long goodsId);

    Result<?> getGoodsDetail(Long goodsId);
}
