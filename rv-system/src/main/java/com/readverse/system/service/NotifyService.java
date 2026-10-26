package com.readverse.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.readverse.common.result.PageResult;
import com.readverse.system.entity.SysNotify;
import com.readverse.system.mapper.SysNotifyMapper;
import com.readverse.system.vo.NotifyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotifyService {

    private final SysNotifyMapper notifyMapper;

    public PageResult<NotifyVO> page(Long receiverId, long current, long size) {
        Page<SysNotify> page = new Page<>(Math.max(1, current), Math.min(100, Math.max(1, size)));
        IPage<SysNotify> result = notifyMapper.selectPage(page, Wrappers.<SysNotify>lambdaQuery()
                .eq(SysNotify::getReceiverId, receiverId)
                .orderByDesc(SysNotify::getCreateTime));
        List<NotifyVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public long unreadCount(Long receiverId) {
        Long count = notifyMapper.selectCount(Wrappers.<SysNotify>lambdaQuery()
                .eq(SysNotify::getReceiverId, receiverId)
                .eq(SysNotify::getIsRead, false));
        return count == null ? 0 : count;
    }

    public void markRead(Long id, Long receiverId) {
        SysNotify notify = notifyMapper.selectById(id);
        if (notify == null || !notify.getReceiverId().equals(receiverId)) {
            return;
        }
        notify.setIsRead(true);
        notifyMapper.updateById(notify);
    }

    public void markAllRead(Long receiverId) {
        SysNotify update = new SysNotify();
        update.setIsRead(true);
        notifyMapper.update(update, Wrappers.<SysNotify>lambdaUpdate()
                .eq(SysNotify::getReceiverId, receiverId)
                .eq(SysNotify::getIsRead, false));
    }

    public void create(Long receiverId, String type, String title, String content, Long bizId) {
        SysNotify notify = new SysNotify();
        notify.setReceiverId(receiverId);
        notify.setType(type);
        notify.setTitle(title);
        notify.setContent(content);
        notify.setBizId(bizId);
        notify.setIsRead(false);
        notifyMapper.insert(notify);
    }

    private NotifyVO toVO(SysNotify notify) {
        return new NotifyVO(notify.getId(), notify.getType(), notify.getTitle(), notify.getContent(),
                notify.getBizId(), notify.getIsRead(), notify.getCreateTime());
    }
}
