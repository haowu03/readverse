package com.readverse.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reader_points_log")
public class ReaderPointsLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer changeAmount;
    private Integer balanceAfter;
    private String bizType;
    private Long bizId;
    private String remark;
    private LocalDateTime createTime;
}
