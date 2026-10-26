package com.readverse.content.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.readverse.common.exception.BizException;
import com.readverse.common.exception.ErrorCode;
import com.readverse.common.result.PageResult;
import com.readverse.content.dto.AuthorSaveRequest;
import com.readverse.content.dto.AuditRequest;
import com.readverse.content.entity.ContentAuthor;
import com.readverse.content.entity.ContentBook;
import com.readverse.content.mapper.ContentAuthorMapper;
import com.readverse.content.mapper.ContentBookMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthorService {

    private final ContentAuthorMapper authorMapper;
    private final ContentBookMapper bookMapper;

    public PageResult<ContentAuthor> page(long current, long size, String keyword, String status) {
        Page<ContentAuthor> page = new Page<>(Math.max(1, current), Math.min(100, Math.max(1, size)));
        IPage<ContentAuthor> result = authorMapper.selectPage(page, Wrappers.<ContentAuthor>lambdaQuery()
                .like(StringUtils.hasText(keyword), ContentAuthor::getName, keyword)
                .eq(StringUtils.hasText(status), ContentAuthor::getStatus, status)
                .orderByDesc(ContentAuthor::getCreateTime));
        return PageResult.of(result);
    }

    public List<ContentAuthor> list() {
        return authorMapper.selectList(Wrappers.<ContentAuthor>lambdaQuery()
                .orderByAsc(ContentAuthor::getName));
    }

    public ContentAuthor get(Long id) {
        ContentAuthor author = authorMapper.selectById(id);
        if (author == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "作者不存在");
        }
        return author;
    }

    @Transactional
    public ContentAuthor create(AuthorSaveRequest request) {
        ContentAuthor author = new ContentAuthor();
        author.setName(request.name());
        author.setAvatar(request.avatar());
        author.setIntro(request.intro());
        author.setStatus("APPROVED");
        author.setBookCount(0);
        author.setDeleted(0);
        authorMapper.insert(author);
        return author;
    }

    @Transactional
    public ContentAuthor update(Long id, AuthorSaveRequest request) {
        ContentAuthor author = get(id);
        author.setName(request.name());
        author.setAvatar(request.avatar());
        author.setIntro(request.intro());
        authorMapper.updateById(author);
        return author;
    }

    @Transactional
    public void delete(Long id) {
        Long books = bookMapper.selectCount(Wrappers.<ContentBook>lambdaQuery().eq(ContentBook::getAuthorId, id));
        if (books != null && books > 0) {
            throw new BizException(ErrorCode.CONFLICT, "该作者下仍有书籍，不能删除");
        }
        authorMapper.deleteById(id);
    }

    @Transactional
    public void audit(Long id, AuditRequest request) {
        ContentAuthor author = get(id);
        author.setStatus(request.status());
        author.setAuditRemark(request.remark());
        authorMapper.updateById(author);
    }

    public void refreshBookCount(Long authorId) {
        Long count = bookMapper.selectCount(Wrappers.<ContentBook>lambdaQuery()
                .eq(ContentBook::getAuthorId, authorId)
                .eq(ContentBook::getDeleted, 0));
        ContentAuthor author = authorMapper.selectById(authorId);
        if (author != null) {
            author.setBookCount(count == null ? 0 : count.intValue());
            authorMapper.updateById(author);
        }
    }
}
