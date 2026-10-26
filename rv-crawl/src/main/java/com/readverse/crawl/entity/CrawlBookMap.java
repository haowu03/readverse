package com.readverse.crawl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("crawl_book_map")
public class CrawlBookMap {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sourceId;
    private String externalBookId;
    private Long bookId;
    private LocalDateTime lastSyncTime;
    private LocalDateTime createTime;
}
