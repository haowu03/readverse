package com.readverse.content.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.readverse.common.exception.BizException;
import com.readverse.common.exception.ErrorCode;
import com.readverse.common.result.PageResult;
import com.readverse.content.dto.CommentRequest;
import com.readverse.content.entity.BookComment;
import com.readverse.content.entity.ContentBook;
import com.readverse.content.mapper.BookCommentMapper;
import com.readverse.content.mapper.ContentBookMapper;
import com.readverse.content.vo.CommentVO;
import com.readverse.framework.security.SecurityUtils;
import com.readverse.system.entity.SysUser;
import com.readverse.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final BookCommentMapper commentMapper;
    private final ContentBookMapper bookMapper;
    private final SysUserMapper userMapper;

    public PageResult<CommentVO> page(Long bookId, long current, long size) {
        Page<BookComment> page = new Page<>(Math.max(1, current), Math.min(50, Math.max(1, size)));
        IPage<BookComment> result = commentMapper.selectPage(page, Wrappers.<BookComment>lambdaQuery()
                .eq(BookComment::getBookId, bookId)
                .eq(BookComment::getStatus, 1)
                .eq(BookComment::getDeleted, 0)
                .orderByDesc(BookComment::getCreateTime));
        List<CommentVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public CommentVO create(CommentRequest request) {
        Long userId = SecurityUtils.requireUserId();
        if (request.bookId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "bookId 不能为空");
        }
        ContentBook book = bookMapper.selectById(request.bookId());
        if (book == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "书籍不存在");
        }
        BookComment comment = new BookComment();
        comment.setUserId(userId);
        comment.setBookId(request.bookId());
        comment.setChapterId(request.chapterId());
        comment.setParentId(request.parentId() == null ? 0L : request.parentId());
        comment.setContent(request.content());
        comment.setLikeCount(0);
        comment.setStatus(1);
        comment.setDeleted(0);
        commentMapper.insert(comment);
        return toVO(comment);
    }

    @Transactional
    public void delete(Long id) {
        Long userId = SecurityUtils.requireUserId();
        BookComment comment = commentMapper.selectById(id);
        if (comment == null) {
            return;
        }
        boolean admin = SecurityUtils.getLoginUser().getRoles() != null
                && SecurityUtils.getLoginUser().getRoles().contains("ADMIN");
        if (!comment.getUserId().equals(userId) && !admin) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        comment.setDeleted(1);
        commentMapper.updateById(comment);
    }

    @Transactional
    public void like(Long id) {
        BookComment comment = commentMapper.selectById(id);
        if (comment == null) {
            return;
        }
        comment.setLikeCount((comment.getLikeCount() == null ? 0 : comment.getLikeCount()) + 1);
        commentMapper.updateById(comment);
    }

    private CommentVO toVO(BookComment comment) {
        SysUser user = userMapper.selectById(comment.getUserId());
        return new CommentVO(comment.getId(), comment.getUserId(),
                user == null ? "书友" : user.getNickname(),
                user == null ? null : user.getAvatar(),
                comment.getContent(), comment.getLikeCount(), comment.getCreateTime());
    }
}
