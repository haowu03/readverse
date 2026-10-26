package com.readverse.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reader_bookshelf")
public class ReaderBookshelf {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long bookId;
    private Long lastChapterId;
    private Integer lastChapterNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
