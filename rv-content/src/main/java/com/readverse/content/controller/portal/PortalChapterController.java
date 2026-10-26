package com.readverse.content.controller.portal;

import com.readverse.common.result.R;
import com.readverse.content.service.ChapterService;
import com.readverse.content.vo.ChapterDetailVO;
import com.readverse.framework.ratelimit.RateLimit;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portal/chapters")
@RequiredArgsConstructor
public class PortalChapterController {

    private final ChapterService chapterService;

    @GetMapping("/{id}")
    @RateLimit(key = "'chapter:' + #id", permits = 120, windowSeconds = 1, dimension = "IP")
    public R<ChapterDetailVO> detail(@PathVariable Long id) {
        return R.ok(chapterService.detail(id));
    }
}
