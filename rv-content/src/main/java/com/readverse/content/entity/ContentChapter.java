package com.readverse.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("content_chapter")
public class ContentChapter {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bookId;
    private Integer chapterNo;
    private String title;
    private Integer wordCount;
    private String content;
    private Boolean isVip;
    private String status;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
