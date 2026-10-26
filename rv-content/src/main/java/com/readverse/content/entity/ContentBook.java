package com.readverse.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("content_book")
public class ContentBook {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String subtitle;
    private Long authorId;
    private Long categoryId;
    private String coverUrl;
    private String intro;
    private String status;
    private BigDecimal score;
    private Integer ratingCount;
    private Long visitCount;
    private Integer wordCount;
    private Integer chapterCount;
    private Boolean isVip;
    private LocalDateTime publishTime;
    private Long lastChapterId;
    private String lastChapterTitle;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
