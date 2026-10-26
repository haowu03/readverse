package com.readverse.content.controller.portal;

import com.readverse.common.result.PageResult;
import com.readverse.common.result.R;
import com.readverse.content.dto.ExchangeRequest;
import com.readverse.content.entity.PointsExchangeOrder;
import com.readverse.content.entity.PointsGoods;
import com.readverse.content.entity.ReaderPointsLog;
import com.readverse.content.service.PointsService;
import com.readverse.content.vo.ExchangeResultVO;
import com.readverse.content.vo.PointsAccountVO;
import com.readverse.content.vo.SignInMonthVO;
import com.readverse.content.vo.SignInResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalPointsController {

    private final PointsService pointsService;

    @GetMapping("/points/account")
    public R<PointsAccountVO> account() {
        return R.ok(pointsService.account());
    }

    @GetMapping("/points/logs")
    public R<PageResult<ReaderPointsLog>> logs(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size) {
        return R.ok(pointsService.logs(current, size));
    }

    @GetMapping("/points/goods")
    public R<List<PointsGoods>> goods() {
        return R.ok(pointsService.goods());
    }

    @PostMapping("/points/exchange")
    public R<ExchangeResultVO> exchange(@Valid @RequestBody ExchangeRequest request) {
        return R.ok(pointsService.exchange(request));
    }

    @GetMapping("/points/orders")
    public R<PageResult<PointsExchangeOrder>> orders(@RequestParam(defaultValue = "1") long current,
                                                     @RequestParam(defaultValue = "10") long size) {
        return R.ok(pointsService.orders(current, size));
    }

    @PostMapping("/sign-in")
    public R<SignInResultVO> signIn() {
        return R.ok(pointsService.signIn());
    }

    @GetMapping("/sign-in/month")
    public R<SignInMonthVO> month() {
        return R.ok(pointsService.month());
    }
}
