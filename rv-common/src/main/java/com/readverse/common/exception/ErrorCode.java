package com.readverse.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(0, "ok"),
    BAD_REQUEST(40000, "请求参数不合法"),
    UNAUTHORIZED(40100, "未登录或登录已过期"),
    INVALID_CREDENTIALS(40101, "用户名或密码错误"),
    FORBIDDEN(40300, "没有权限执行该操作"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "数据冲突"),
    TOO_MANY_REQUESTS(42901, "请求过于频繁，请稍后再试"),
    INTERNAL_ERROR(50000, "系统繁忙，请稍后再试"),
    BUSINESS_ERROR(60000, "业务处理失败"),
    CHAPTER_LOCKED(60001, "本章节需要解锁后阅读"),
    POINTS_NOT_ENOUGH(60002, "积分不足"),
    SIGN_IN_DUPLICATED(60003, "今天已经签到过了"),
    EXCHANGE_OUT_OF_STOCK(60004, "商品库存不足");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
