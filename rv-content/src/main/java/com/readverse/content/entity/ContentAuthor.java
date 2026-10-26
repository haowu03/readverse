package com.readverse.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("content_author")
public class ContentAuthor {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String avatar;
    private String intro;
    private String status;
    private String auditRemark;
    private Integer bookCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
