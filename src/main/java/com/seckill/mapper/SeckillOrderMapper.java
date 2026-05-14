package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 秒杀订单 Mapper
 * 继承 BaseMapper 自动获得 insert/selectById/updateById/deleteById 等方法
 */
@Mapper
public interface SeckillOrderMapper extends BaseMapper<SeckillOrder> {
}
