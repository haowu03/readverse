package com.readverse.system.controller;

import com.readverse.common.result.PageResult;
import com.readverse.common.result.R;
import com.readverse.framework.security.SecurityUtils;
import com.readverse.system.service.NotifyService;
import com.readverse.system.vo.NotifyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class NotifyController {

    private final NotifyService notifyService;

    @GetMapping
    public R<PageResult<NotifyVO>> page(@RequestParam(defaultValue = "1") long current,
                                        @RequestParam(defaultValue = "10") long size) {
        return R.ok(notifyService.page(SecurityUtils.requireUserId(), current, size));
    }

    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        return R.ok(notifyService.unreadCount(SecurityUtils.requireUserId()));
    }

    @PutMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        notifyService.markRead(id, SecurityUtils.requireUserId());
        return R.ok();
    }

    @PutMapping("/read-all")
    public R<Void> markAllRead() {
        notifyService.markAllRead(SecurityUtils.requireUserId());
        return R.ok();
    }
}
