package com.readverse.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_notify")
public class SysNotify {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long receiverId;
    private String type;
    private String title;
    private String content;
    private Long bizId;
    private Boolean isRead;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
