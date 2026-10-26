package com.readverse.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("points_exchange_order")
public class PointsExchangeOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long goodsId;
    private Integer pointsCost;
    private String status;
    private String couponCode;
    private String idempotencyKey;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
