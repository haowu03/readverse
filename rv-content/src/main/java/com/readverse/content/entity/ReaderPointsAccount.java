package com.readverse.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reader_points_account")
public class ReaderPointsAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer balance;
    private Integer totalEarned;
    private Integer totalSpent;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
