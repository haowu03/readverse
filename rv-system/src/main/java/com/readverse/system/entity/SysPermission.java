package com.readverse.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_permission")
public class SysPermission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private String type;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private Boolean visible;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
