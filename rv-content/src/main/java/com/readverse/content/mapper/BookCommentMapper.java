package com.readverse.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.readverse.content.entity.BookComment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookCommentMapper extends BaseMapper<BookComment> {
}
