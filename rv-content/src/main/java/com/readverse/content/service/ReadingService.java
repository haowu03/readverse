package com.readverse.content.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.readverse.common.exception.BizException;
import com.readverse.common.exception.ErrorCode;
import com.readverse.content.dto.ReadRecordRequest;
import com.readverse.content.entity.ContentBook;
import com.readverse.content.entity.ContentChapter;
import com.readverse.content.entity.ReaderBookshelf;
import com.readverse.content.entity.ReaderReadRecord;
import com.readverse.content.mapper.ContentChapterMapper;
import com.readverse.content.mapper.ReaderBookshelfMapper;
import com.readverse.content.mapper.ReaderReadRecordMapper;
import com.readverse.content.vo.BookshelfItemVO;
import com.readverse.content.vo.ReadRecordVO;
import com.readverse.framework.mq.DomainEvent;
import com.readverse.framework.mq.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReadingService {

    private final ReaderBookshelfMapper bookshelfMapper;
    private final ReaderReadRecordMapper readRecordMapper;
    private final ContentChapterMapper chapterMapper;
    private final BookService bookService;
    private final BookAssembler assembler;
    private final EventPublisher eventPublisher;

    public List<BookshelfItemVO> bookshelf(Long userId) {
        List<ReaderBookshelf> shelves = bookshelfMapper.selectList(Wrappers.<ReaderBookshelf>lambdaQuery()
                .eq(ReaderBookshelf::getUserId, userId)
                .orderByDesc(ReaderBookshelf::getUpdateTime));
        return shelves.stream().map(shelf -> {
            ContentBook book = bookService.getEntity(shelf.getBookId());
            return new BookshelfItemVO(assembler.toCard(book), shelf.getLastChapterId(),
                    shelf.getLastChapterNo(), shelf.getUpdateTime());
        }).toList();
    }

    @Transactional
    public void add(Long userId, Long bookId) {
        ContentBook book = bookService.getEntity(bookId);
        if (!"ON_SHELF".equals(book.getStatus())) {
            throw new BizException(ErrorCode.NOT_FOUND, "书籍未上架");
        }
        Long exists = bookshelfMapper.selectCount(Wrappers.<ReaderBookshelf>lambdaQuery()
                .eq(ReaderBookshelf::getUserId, userId)
                .eq(ReaderBookshelf::getBookId, bookId));
        if (exists != null && exists > 0) {
            return;
        }
        ReaderBookshelf shelf = new ReaderBookshelf();
        shelf.setUserId(userId);
        shelf.setBookId(bookId);
        bookshelfMapper.insert(shelf);
    }

    @Transactional
    public void remove(Long userId, Long bookId) {
        bookshelfMapper.delete(Wrappers.<ReaderBookshelf>lambdaQuery()
                .eq(ReaderBookshelf::getUserId, userId)
                .eq(ReaderBookshelf::getBookId, bookId));
    }

    public ReadRecordVO getRecord(Long userId, Long bookId) {
        ReaderReadRecord record = readRecordMapper.selectOne(Wrappers.<ReaderReadRecord>lambdaQuery()
                .eq(ReaderReadRecord::getUserId, userId)
                .eq(ReaderReadRecord::getBookId, bookId));
        return record == null ? null : toVO(record);
    }

    @Transactional
    public ReadRecordVO saveRecord(Long userId, ReadRecordRequest request) {
        ContentChapter chapter = chapterMapper.selectById(request.chapterId());
        if (chapter == null || !chapter.getBookId().equals(request.bookId())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "章节与书籍不匹配");
        }
        ReaderReadRecord record = readRecordMapper.selectOne(Wrappers.<ReaderReadRecord>lambdaQuery()
                .eq(ReaderReadRecord::getUserId, userId)
                .eq(ReaderReadRecord::getBookId, request.bookId()));
        BigDecimal progress = request.progress() == null ? BigDecimal.ZERO : request.progress();
        int readSeconds = request.readSeconds() == null ? 0 : request.readSeconds();
        if (record == null) {
            record = new ReaderReadRecord();
            record.setUserId(userId);
            record.setBookId(request.bookId());
            record.setChapterId(request.chapterId());
            record.setChapterNo(request.chapterNo() == null ? chapter.getChapterNo() : request.chapterNo());
            record.setProgress(progress);
            record.setReadSeconds(readSeconds);
            readRecordMapper.insert(record);
        } else {
            record.setChapterId(request.chapterId());
            record.setChapterNo(request.chapterNo() == null ? chapter.getChapterNo() : request.chapterNo());
            record.setProgress(progress);
            record.setReadSeconds((record.getReadSeconds() == null ? 0 : record.getReadSeconds()) + readSeconds);
            readRecordMapper.updateById(record);
        }
        updateShelf(userId, request.bookId(), chapter);
        eventPublisher.publish(DomainEvent.of("CHAPTER_READ", request.bookId(),
                Map.of("userId", userId, "bookId", request.bookId(), "chapterId", request.chapterId())));
        return toVO(record);
    }

    private void updateShelf(Long userId, Long bookId, ContentChapter chapter) {
        ReaderBookshelf shelf = bookshelfMapper.selectOne(Wrappers.<ReaderBookshelf>lambdaQuery()
                .eq(ReaderBookshelf::getUserId, userId)
                .eq(ReaderBookshelf::getBookId, bookId));
        if (shelf == null) {
            shelf = new ReaderBookshelf();
            shelf.setUserId(userId);
            shelf.setBookId(bookId);
            shelf.setLastChapterId(chapter.getId());
            shelf.setLastChapterNo(chapter.getChapterNo());
            bookshelfMapper.insert(shelf);
        } else {
            shelf.setLastChapterId(chapter.getId());
            shelf.setLastChapterNo(chapter.getChapterNo());
            shelf.setUpdateTime(LocalDateTime.now());
            bookshelfMapper.updateById(shelf);
        }
    }

    private ReadRecordVO toVO(ReaderReadRecord record) {
        return new ReadRecordVO(record.getBookId(), record.getChapterId(), record.getChapterNo(),
                record.getProgress(), record.getReadSeconds(), record.getUpdateTime());
    }
}
