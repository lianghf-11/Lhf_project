package com.seckill.controller;

import com.seckill.dto.Result;
import com.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 秒杀接口控制器
 *
 * 三个核心接口：
 * 1. POST /api/seckill/{goodsId}        — 执行秒杀（需要 X-User-Id 请求头模拟用户身份）
 * 2. GET  /api/seckill/goods/{goodsId}  — 查看商品详情（走三级缓存）
 * 3. POST /api/seckill/admin/load/{goodsId} — 管理员预热商品库存到 Redis
 */
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    /**
     * 秒杀接口
     * 请求方式：POST（浏览器地址栏发的是 GET，所以要用 curl 或 Postman 测试）
     * 实际项目中 userId 从 JWT 令牌中获取，这里用请求头模拟
     */
    @PostMapping("/{goodsId}")
    public Result<String> seckill(
            @PathVariable Long goodsId,
            @RequestHeader("X-User-Id") Long userId) {
        return seckillService.seckill(goodsId, userId);
    }

    /**
     * 商品详情接口（GET 请求，浏览器可直接访问）
     * 缓存策略：先查 Redis → 缓存未命中则加互斥锁查 MySQL → 回填缓存
     */
    @GetMapping("/goods/{goodsId}")
    public Result<?> getGoodsDetail(@PathVariable Long goodsId) {
        return seckillService.getGoodsDetail(goodsId);
    }

    /**
     * 库存预热接口（管理员调用）
     * 将 MySQL 中的商品库存加载到 Redis，秒杀时才走 Redis 不走数据库
     */
    @PostMapping("/admin/load/{goodsId}")
    public Result<String> loadStock(@PathVariable Long goodsId) {
        seckillService.loadStockToRedis(goodsId);
        return Result.ok("库存预热成功");
    }
}
