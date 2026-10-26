package com.readverse.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("stat_daily_platform")
public class StatDailyPlatform {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private Integer newUsers;
    private Integer activeUsers;
    private Integer readCount;
    private Integer newBooks;
    private BigDecimal crawlSuccessRate;
}
