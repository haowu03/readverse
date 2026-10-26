package com.readverse.content.controller.portal;

import com.readverse.common.result.R;
import com.readverse.content.dto.RatingRequest;
import com.readverse.content.service.RatingService;
import com.readverse.content.vo.RatingSummaryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portal/ratings")
@RequiredArgsConstructor
public class PortalRatingController {

    private final RatingService ratingService;

    @PostMapping("/{bookId}")
    public R<RatingSummaryVO> rate(@PathVariable Long bookId, @Valid @RequestBody RatingRequest request) {
        return R.ok(ratingService.rate(bookId, request.score()));
    }
}
