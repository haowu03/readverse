package com.readverse.content.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.readverse.content.entity.ContentBook;
import com.readverse.content.entity.ReaderReadRecord;
import com.readverse.content.mapper.ContentBookMapper;
import com.readverse.content.mapper.ContentChapterMapper;
import com.readverse.content.mapper.ReaderReadRecordMapper;
import com.readverse.content.vo.DashboardSummaryVO;
import com.readverse.content.vo.TrendPointVO;
import com.readverse.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final ContentBookMapper bookMapper;
    private final ContentChapterMapper chapterMapper;
    private final SysUserMapper userMapper;
    private final ReaderReadRecordMapper readRecordMapper;

    public DashboardSummaryVO summary() {
        long books = count(bookMapper.selectCount(null));
        long chapters = count(chapterMapper.selectCount(null));
        long users = count(userMapper.selectCount(null));
        Long todayReads = readRecordMapper.selectCount(Wrappers.<ReaderReadRecord>lambdaQuery()
                .ge(ReaderReadRecord::getUpdateTime, LocalDate.now().atStartOfDay()));
        return new DashboardSummaryVO(books, chapters, users, todayReads == null ? 0 : todayReads, 0, 0);
    }

    public List<TrendPointVO> bookTrend(int days) {
        int size = Math.min(30, Math.max(1, days));
        LocalDate start = LocalDate.now().minusDays(size - 1L);
        List<ContentBook> books = bookMapper.selectList(Wrappers.<ContentBook>lambdaQuery()
                .ge(ContentBook::getCreateTime, start.atStartOfDay()));
        Map<LocalDate, Long> grouped = books.stream()
                .filter(b -> b.getCreateTime() != null)
                .collect(Collectors.groupingBy(b -> b.getCreateTime().toLocalDate(), Collectors.counting()));
        return fillDays(start, size, grouped);
    }

    public List<TrendPointVO> readActivity(int days) {
        int size = Math.min(30, Math.max(1, days));
        LocalDate start = LocalDate.now().minusDays(size - 1L);
        List<ReaderReadRecord> records = readRecordMapper.selectList(Wrappers.<ReaderReadRecord>lambdaQuery()
                .ge(ReaderReadRecord::getUpdateTime, start.atStartOfDay()));
        Map<LocalDate, Long> grouped = records.stream()
                .filter(r -> r.getUpdateTime() != null)
                .collect(Collectors.groupingBy(r -> r.getUpdateTime().toLocalDate(), Collectors.counting()));
        return fillDays(start, size, grouped);
    }

    private List<TrendPointVO> fillDays(LocalDate start, int size, Map<LocalDate, Long> grouped) {
        List<TrendPointVO> points = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            LocalDate day = start.plusDays(i);
            points.add(new TrendPointVO(day.toString(), grouped.getOrDefault(day, 0L)));
        }
        return points;
    }

    private long count(Long value) {
        return value == null ? 0 : value;
    }
}
