package com.readverse.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class SysOperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String module;
    private String action;
    private String method;
    private String uri;
    private String ip;
    private String params;
    private String result;
    private Long costMs;
    private LocalDateTime createTime;
}
