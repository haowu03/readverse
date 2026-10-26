package com.readverse.crawl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_job_log")
public class SysJobLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String jobName;
    private String state;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long costMs;
    private Integer successCount;
    private Integer failCount;
    private String message;
    private LocalDateTime createTime;
}
