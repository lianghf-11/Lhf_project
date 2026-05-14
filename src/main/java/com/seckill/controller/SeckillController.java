package com.seckill.controller;

import com.seckill.dto.Result;
import com.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    /**
     * 秒杀接口
     * 实际项目中 userId 从 JWT 中获取，这里用请求头模拟
     */
    @PostMapping("/{goodsId}")
    public Result<String> seckill(
            @PathVariable Long goodsId,
            @RequestHeader("X-User-Id") Long userId) {
        return seckillService.seckill(goodsId, userId);
    }

    /**
     * 商品详情（带缓存）
     */
    @GetMapping("/goods/{goodsId}")
    public Result<?> getGoodsDetail(@PathVariable Long goodsId) {
        return seckillService.getGoodsDetail(goodsId);
    }

    /**
     * 库存预热（管理员调用，将商品库存加载到 Redis）
     */
    @PostMapping("/admin/load/{goodsId}")
    public Result<String> loadStock(@PathVariable Long goodsId) {
        seckillService.loadStockToRedis(goodsId);
        return Result.ok("库存预热成功");
    }
}
