package com.readverse.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("points_goods")
public class PointsGoods {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String type;
    private Integer pointsPrice;
    private Integer stock;
    private Integer status;
    private String configJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
