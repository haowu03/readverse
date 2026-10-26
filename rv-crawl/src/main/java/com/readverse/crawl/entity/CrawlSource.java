package com.readverse.crawl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("crawl_source")
public class CrawlSource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String sourceType;
    private String baseUrl;
    private String configJson;
    private Integer status;
    private LocalDateTime lastSyncTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
