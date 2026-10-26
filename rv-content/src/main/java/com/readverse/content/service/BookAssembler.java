package com.readverse.content.service;

import com.readverse.content.entity.ContentAuthor;
import com.readverse.content.entity.ContentBook;
import com.readverse.content.entity.ContentCategory;
import com.readverse.content.mapper.ContentAuthorMapper;
import com.readverse.content.mapper.ContentCategoryMapper;
import com.readverse.content.vo.BookCardVO;
import com.readverse.content.vo.BookDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookAssembler {

    private final ContentAuthorMapper authorMapper;
    private final ContentCategoryMapper categoryMapper;

    public BookCardVO toCard(ContentBook book) {
        if (book == null) {
            return null;
        }
        ContentAuthor author = authorMapper.selectById(book.getAuthorId());
        ContentCategory category = categoryMapper.selectById(book.getCategoryId());
        return new BookCardVO(book.getId(), book.getTitle(), book.getSubtitle(),
                author == null ? "未知作者" : author.getName(),
                category == null ? "未分类" : category.getName(),
                book.getCoverUrl(), book.getIntro(), book.getStatus(), book.getScore(),
                book.getRatingCount(), book.getVisitCount(), book.getChapterCount(), book.getWordCount(), book.getIsVip(),
                book.getLastChapterTitle(), book.getUpdateTime());
    }

    public BookDetailVO toDetail(ContentBook book) {
        if (book == null) {
            return null;
        }
        ContentAuthor author = authorMapper.selectById(book.getAuthorId());
        ContentCategory category = categoryMapper.selectById(book.getCategoryId());
        return new BookDetailVO(book.getId(), book.getTitle(), book.getSubtitle(),
                book.getAuthorId(), author == null ? "未知作者" : author.getName(),
                book.getCategoryId(), category == null ? "未分类" : category.getName(),
                book.getCoverUrl(), book.getIntro(), book.getStatus(), book.getScore(),
                book.getRatingCount(), book.getVisitCount(), book.getWordCount(), book.getChapterCount(),
                book.getIsVip(), book.getPublishTime(), book.getLastChapterTitle(), book.getUpdateTime());
    }
}
