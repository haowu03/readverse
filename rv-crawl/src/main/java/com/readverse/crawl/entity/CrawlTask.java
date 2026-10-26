package com.readverse.crawl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("crawl_task")
public class CrawlTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sourceId;
    private String taskType;
    private Long bookId;
    private String state;
    private Integer retryTimes;
    private Integer maxRetry;
    private String syncCursor;
    private String message;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
