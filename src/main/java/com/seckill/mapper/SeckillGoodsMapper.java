package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.SeckillGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 秒杀商品 Mapper
 *
 * MyBatis-Plus 的 BaseMapper 自带常用增删改查方法：
 * selectById / selectList / insert / updateById / deleteById 等
 *
 * 自定义 SQL 写在注解里（适合简单 SQL）或 XML 文件里（适合复杂 SQL）
 */
@Mapper
public interface SeckillGoodsMapper extends BaseMapper<SeckillGoods> {

    /**
     * 乐观锁扣减库存
     * SQL: UPDATE seckill_goods SET stock = stock - 1 WHERE id = #{id} AND stock > 0
     *
     * 返回值 = 受影响的行数：
     *   1 → 扣减成功
     *   0 → 库存为 0（被其他线程抢完了，不会出现负数库存）
     *
     * 这是数据库层面的最后防线，配合 Redis 的 Lua 脚本双层保底
     */
    @Update("UPDATE seckill_goods SET stock = stock - 1 WHERE id = #{id} AND stock > 0")
    int deductStock(Long id);
}
