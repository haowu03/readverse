package com.readverse.content.controller.portal;

import com.readverse.common.result.R;
import com.readverse.content.dto.ReadRecordRequest;
import com.readverse.content.service.ReadingService;
import com.readverse.content.vo.ReadRecordVO;
import com.readverse.framework.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portal/read-record")
@RequiredArgsConstructor
public class PortalReadingController {

    private final ReadingService readingService;

    @GetMapping("/{bookId}")
    public R<ReadRecordVO> get(@PathVariable Long bookId) {
        return R.ok(readingService.getRecord(SecurityUtils.requireUserId(), bookId));
    }

    @PutMapping
    public R<ReadRecordVO> save(@Valid @RequestBody ReadRecordRequest request) {
        return R.ok(readingService.saveRecord(SecurityUtils.requireUserId(), request));
    }
}
