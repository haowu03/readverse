package com.readverse.content.controller.admin;

import com.readverse.common.result.PageResult;
import com.readverse.common.result.R;
import com.readverse.content.dto.GoodsSaveRequest;
import com.readverse.content.entity.PointsExchangeOrder;
import com.readverse.content.entity.PointsGoods;
import com.readverse.content.service.PointsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/points")
@RequiredArgsConstructor
public class AdminPointsController {

    private final PointsService pointsService;

    @GetMapping("/goods")
    @PreAuthorize("hasAuthority('points:goods:list')")
    public R<List<PointsGoods>> goods() {
        return R.ok(pointsService.allGoods());
    }

    @PostMapping("/goods")
    @PreAuthorize("hasAuthority('points:goods:add')")
    public R<PointsGoods> create(@Valid @RequestBody GoodsSaveRequest request) {
        return R.ok(pointsService.saveGoods(null, request));
    }

    @PutMapping("/goods/{id}")
    @PreAuthorize("hasAuthority('points:goods:edit')")
    public R<PointsGoods> update(@PathVariable Long id, @Valid @RequestBody GoodsSaveRequest request) {
        return R.ok(pointsService.saveGoods(id, request));
    }

    @DeleteMapping("/goods/{id}")
    @PreAuthorize("hasAuthority('points:goods:delete')")
    public R<Void> delete(@PathVariable Long id) {
        pointsService.deleteGoods(id);
        return R.ok();
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('points:order:list')")
    public R<PageResult<PointsExchangeOrder>> orders(@RequestParam(defaultValue = "1") long current,
                                                     @RequestParam(defaultValue = "10") long size) {
        return R.ok(pointsService.adminOrders(current, size));
    }

    @PostMapping("/reconcile")
    @PreAuthorize("hasAuthority('points:order:list')")
    public R<List<String>> reconcile() {
        return R.ok(pointsService.reconcile());
    }
}
