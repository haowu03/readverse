package com.readverse.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("stat_daily_book")
public class StatDailyBook {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private Long bookId;
    private Integer readCount;
    private Integer uniqueReaders;
    private Integer favoriteCount;
    private BigDecimal scoreAvg;
}
