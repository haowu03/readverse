package com.readverse.system.event;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.readverse.framework.mq.DomainEvent;
import com.readverse.system.entity.SysRole;
import com.readverse.system.entity.SysUserRole;
import com.readverse.system.mapper.SysRoleMapper;
import com.readverse.system.mapper.SysUserRoleMapper;
import com.readverse.system.service.NotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookAuditNotificationHandler {

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final NotifyService notifyService;

    public void handle(DomainEvent event) {
        if (event == null || !event.type().startsWith("BOOK_AUDIT")) {
            return;
        }
        SysRole admin = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, "ADMIN"));
        if (admin == null) {
            return;
        }
        List<Long> receivers = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getRoleId, admin.getId()))
                .stream().map(SysUserRole::getUserId).toList();
        Object title = event.payload() == null ? null : event.payload().get("title");
        String result = event.type().endsWith("PASSED") ? "通过" : "驳回";
        String content = "《" + (title == null ? event.bizId() : title) + "》审核" + result + "，点击查看详情。";
        for (Long receiver : receivers) {
            notifyService.create(receiver, "BOOK_AUDIT", "书籍审核结果", content, event.bizId());
        }
        log.info("Book audit notification created for {} admins, event={}", receivers.size(), event.eventId());
    }
}
