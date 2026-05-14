package com.seckill.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果类
 *
 * 前端收到的 JSON 格式：{"code": 200, "msg": "success", "data": {...}}
 *
 * code: 200=成功  400=业务错误（重复秒杀/库存不足）  500=系统异常
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    /** 状态码：200 成功，400 业务错误，500 系统异常 */
    private Integer code;
    /** 提示信息 */
    private String msg;
    /** 返回数据 */
    private T data;

    /** 成功 — 带数据 */
    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    /** 成功 — 不带数据 */
    public static <T> Result<T> ok() {
        return new Result<>(200, "success", null);
    }

    /** 失败 — 默认 500 */
    public static <T> Result<T> fail(String msg) {
        return new Result<>(500, msg, null);
    }

    /** 失败 — 自定义状态码 */
    public static <T> Result<T> fail(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }
}
