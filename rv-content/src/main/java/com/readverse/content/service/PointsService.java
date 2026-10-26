package com.readverse.content.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.readverse.common.exception.BizException;
import com.readverse.common.exception.ErrorCode;
import com.readverse.common.result.PageResult;
import com.readverse.content.dto.ExchangeRequest;
import com.readverse.content.entity.PointsExchangeOrder;
import com.readverse.content.entity.PointsGoods;
import com.readverse.content.entity.ReaderEntitlement;
import com.readverse.content.entity.ReaderPointsAccount;
import com.readverse.content.entity.ReaderPointsLog;
import com.readverse.content.entity.ReaderSignIn;
import com.readverse.content.mapper.PointsExchangeOrderMapper;
import com.readverse.content.mapper.PointsGoodsMapper;
import com.readverse.content.mapper.ReaderEntitlementMapper;
import com.readverse.content.mapper.ReaderPointsAccountMapper;
import com.readverse.content.mapper.ReaderPointsLogMapper;
import com.readverse.content.mapper.ReaderSignInMapper;
import com.readverse.content.vo.ExchangeResultVO;
import com.readverse.content.vo.PointsAccountVO;
import com.readverse.content.vo.SignInMonthVO;
import com.readverse.content.vo.SignInResultVO;
import com.readverse.framework.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointsService {

    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final ReaderPointsAccountMapper accountMapper;
    private final ReaderPointsLogMapper logMapper;
    private final ReaderSignInMapper signInMapper;
    private final PointsGoodsMapper goodsMapper;
    private final PointsExchangeOrderMapper orderMapper;
    private final ReaderEntitlementMapper entitlementMapper;
    private final StringRedisTemplate redis;

    @Value("${rv.points.sign-in-base:5}")
    private int signInBase;
    @Value("${rv.points.continuous-seven-bonus:20}")
    private int sevenBonus;
    @Value("${rv.points.continuous-thirty-bonus:100}")
    private int thirtyBonus;
    @Value("${rv.redis.enabled:true}")
    private boolean redisEnabled;

    public PointsAccountVO account() {
        Long userId = SecurityUtils.requireUserId();
        ReaderPointsAccount account = getOrCreateAccount(userId);
        return new PointsAccountVO(account.getBalance(), account.getTotalEarned(), account.getTotalSpent());
    }

    @Transactional
    public SignInResultVO signIn() {
        Long userId = SecurityUtils.requireUserId();
        LocalDate today = LocalDate.now();
        Long exists = signInMapper.selectCount(Wrappers.<ReaderSignIn>lambdaQuery()
                .eq(ReaderSignIn::getUserId, userId)
                .eq(ReaderSignIn::getSignDate, today));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.SIGN_IN_DUPLICATED);
        }
        ReaderSignIn yesterday = signInMapper.selectOne(Wrappers.<ReaderSignIn>lambdaQuery()
                .eq(ReaderSignIn::getUserId, userId)
                .eq(ReaderSignIn::getSignDate, today.minusDays(1)));
        int continuous = yesterday == null ? 1 : yesterday.getContinuousDays() + 1;
        int reward = signInBase;
        if (continuous == 7) {
            reward += sevenBonus;
        }
        if (continuous == 30) {
            reward += thirtyBonus;
        }
        ReaderSignIn signIn = new ReaderSignIn();
        signIn.setUserId(userId);
        signIn.setSignDate(today);
        signIn.setContinuousDays(continuous);
        signIn.setRewardPoints(reward);
        try {
            signInMapper.insert(signIn);
            int balance = changePoints(userId, reward, "SIGN_IN",
                    Long.valueOf(today.format(BASIC_DATE)), "每日签到");
            markBitmap(userId, today);
            return new SignInResultVO(true, reward, continuous, balance);
        } catch (DataIntegrityViolationException e) {
            throw new BizException(ErrorCode.SIGN_IN_DUPLICATED);
        }
    }

    public SignInMonthVO month() {
        Long userId = SecurityUtils.requireUserId();
        YearMonth month = YearMonth.now();
        List<ReaderSignIn> list = signInMapper.selectList(Wrappers.<ReaderSignIn>lambdaQuery()
                .eq(ReaderSignIn::getUserId, userId)
                .ge(ReaderSignIn::getSignDate, month.atDay(1))
                .le(ReaderSignIn::getSignDate, month.atEndOfMonth())
                .orderByAsc(ReaderSignIn::getSignDate));
        List<Integer> days = list.stream().map(s -> s.getSignDate().getDayOfMonth()).toList();
        int continuous = list.isEmpty() ? 0 : list.get(list.size() - 1).getContinuousDays();
        boolean signedToday = list.stream().anyMatch(s -> s.getSignDate().equals(LocalDate.now()));
        return new SignInMonthVO(month.toString(), days, continuous, signedToday);
    }

    public PageResult<ReaderPointsLog> logs(long current, long size) {
        Long userId = SecurityUtils.requireUserId();
        Page<ReaderPointsLog> page = new Page<>(Math.max(1, current), Math.min(100, Math.max(1, size)));
        IPage<ReaderPointsLog> result = logMapper.selectPage(page, Wrappers.<ReaderPointsLog>lambdaQuery()
                .eq(ReaderPointsLog::getUserId, userId)
                .orderByDesc(ReaderPointsLog::getCreateTime));
        return PageResult.of(result);
    }

    public List<PointsGoods> goods() {
        return goodsMapper.selectList(Wrappers.<PointsGoods>lambdaQuery()
                .eq(PointsGoods::getStatus, 1)
                .orderByAsc(PointsGoods::getPointsPrice));
    }

    public List<PointsGoods> allGoods() {
        return goodsMapper.selectList(Wrappers.<PointsGoods>lambdaQuery()
                .orderByAsc(PointsGoods::getPointsPrice));
    }

    @Transactional
    public PointsGoods saveGoods(Long id, com.readverse.content.dto.GoodsSaveRequest request) {
        PointsGoods goods = id == null ? new PointsGoods() : goodsMapper.selectById(id);
        if (goods == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        goods.setName(request.name());
        goods.setType(request.type());
        goods.setPointsPrice(request.pointsPrice());
        goods.setStock(request.stock());
        goods.setStatus(request.status() == null ? 1 : request.status());
        goods.setConfigJson(request.configJson());
        if (id == null) {
            goodsMapper.insert(goods);
        } else {
            goodsMapper.updateById(goods);
        }
        return goods;
    }

    @Transactional
    public void deleteGoods(Long id) {
        goodsMapper.deleteById(id);
    }

    @Transactional
    public ExchangeResultVO exchange(ExchangeRequest request) {
        Long userId = SecurityUtils.requireUserId();
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            PointsExchangeOrder existing = orderMapper.selectOne(Wrappers.<PointsExchangeOrder>lambdaQuery()
                    .eq(PointsExchangeOrder::getUserId, userId)
                    .eq(PointsExchangeOrder::getIdempotencyKey, request.idempotencyKey()));
            if (existing != null) {
                ReaderPointsAccount account = getOrCreateAccount(userId);
                return new ExchangeResultVO(existing.getOrderNo(), existing.getCouponCode(),
                        existing.getPointsCost(), account.getBalance(), existing.getStatus());
            }
        }
        PointsGoods goods = goodsMapper.selectById(request.goodsId());
        if (goods == null || goods.getStatus() == null || goods.getStatus() != 1) {
            throw new BizException(ErrorCode.NOT_FOUND, "商品不存在或已下架");
        }
        int stockUpdated = goodsMapper.update(null, Wrappers.<PointsGoods>lambdaUpdate()
                .eq(PointsGoods::getId, goods.getId())
                .gt(PointsGoods::getStock, 0)
                .setSql("stock = stock - 1"));
        if (stockUpdated == 0) {
            throw new BizException(ErrorCode.EXCHANGE_OUT_OF_STOCK);
        }

        PointsExchangeOrder order = new PointsExchangeOrder();
        order.setOrderNo("RV" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999));
        order.setUserId(userId);
        order.setGoodsId(goods.getId());
        order.setPointsCost(goods.getPointsPrice());
        order.setStatus("SUCCESS");
        order.setCouponCode("RVC" + Long.toHexString(ThreadLocalRandom.current().nextLong(0x100000000L, 0xFFFFFFFFFL)).toUpperCase());
        order.setIdempotencyKey(request.idempotencyKey());
        orderMapper.insert(order);

        int balance = changePoints(userId, -goods.getPointsPrice(), "EXCHANGE", order.getId(),
                "兑换 " + goods.getName());

        if (request.targetId() != null) {
            String type = goods.getType() == null ? "" : goods.getType().toUpperCase();
            String targetType = type.contains("BOOK") ? "BOOK" : type.contains("CHAPTER") ? "CHAPTER" : null;
            if (targetType != null) {
                ReaderEntitlement entitlement = new ReaderEntitlement();
                entitlement.setUserId(userId);
                entitlement.setTargetType(targetType);
                entitlement.setTargetId(request.targetId());
                entitlement.setSource("EXCHANGE");
                entitlement.setStatus(1);
                entitlement.setExpireTime(LocalDateTime.now().plusDays(7));
                try {
                    entitlementMapper.insert(entitlement);
                } catch (DataIntegrityViolationException ignored) {
                    // Already entitled; idempotent for the same target.
                }
            }
        }
        return new ExchangeResultVO(order.getOrderNo(), order.getCouponCode(),
                goods.getPointsPrice(), balance, order.getStatus());
    }

    public PageResult<PointsExchangeOrder> orders(long current, long size) {
        Long userId = SecurityUtils.requireUserId();
        Page<PointsExchangeOrder> page = new Page<>(Math.max(1, current), Math.min(100, Math.max(1, size)));
        IPage<PointsExchangeOrder> result = orderMapper.selectPage(page, Wrappers.<PointsExchangeOrder>lambdaQuery()
                .eq(PointsExchangeOrder::getUserId, userId)
                .orderByDesc(PointsExchangeOrder::getCreateTime));
        return PageResult.of(result);
    }

    public PageResult<PointsExchangeOrder> adminOrders(long current, long size) {
        Page<PointsExchangeOrder> page = new Page<>(Math.max(1, current), Math.min(100, Math.max(1, size)));
        IPage<PointsExchangeOrder> result = orderMapper.selectPage(page, Wrappers.<PointsExchangeOrder>lambdaQuery()
                .orderByDesc(PointsExchangeOrder::getCreateTime));
        return PageResult.of(result);
    }

    public List<String> reconcile() {
        List<String> diffs = new ArrayList<>();
        List<ReaderPointsAccount> accounts = accountMapper.selectList(null);
        for (ReaderPointsAccount account : accounts) {
            List<ReaderPointsLog> logs = logMapper.selectList(Wrappers.<ReaderPointsLog>lambdaQuery()
                    .eq(ReaderPointsLog::getUserId, account.getUserId()));
            int sum = logs.stream().mapToInt(ReaderPointsLog::getChangeAmount).sum();
            if (sum != account.getBalance()) {
                diffs.add("user=" + account.getUserId() + " balance=" + account.getBalance() + " ledger=" + sum);
            }
        }
        return diffs;
    }

    private ReaderPointsAccount getOrCreateAccount(Long userId) {
        ReaderPointsAccount account = accountMapper.selectOne(Wrappers.<ReaderPointsAccount>lambdaQuery()
                .eq(ReaderPointsAccount::getUserId, userId));
        if (account != null) {
            return account;
        }
        account = new ReaderPointsAccount();
        account.setUserId(userId);
        account.setBalance(0);
        account.setTotalEarned(0);
        account.setTotalSpent(0);
        account.setVersion(0);
        try {
            accountMapper.insert(account);
            return account;
        } catch (DataIntegrityViolationException e) {
            ReaderPointsAccount existing = accountMapper.selectOne(Wrappers.<ReaderPointsAccount>lambdaQuery()
                    .eq(ReaderPointsAccount::getUserId, userId));
            if (existing != null) {
                return existing;
            }
            throw e;
        }
    }

    private int changePoints(Long userId, int delta, String bizType, Long bizId, String remark) {
        for (int attempt = 0; attempt < 3; attempt++) {
            ReaderPointsAccount account = getOrCreateAccount(userId);
            int balance = account.getBalance() + delta;
            if (balance < 0) {
                throw new BizException(ErrorCode.POINTS_NOT_ENOUGH);
            }
            int earned = account.getTotalEarned() + Math.max(delta, 0);
            int spent = account.getTotalSpent() + Math.max(-delta, 0);
            int updated = accountMapper.update(null, Wrappers.<ReaderPointsAccount>lambdaUpdate()
                    .eq(ReaderPointsAccount::getId, account.getId())
                    .eq(ReaderPointsAccount::getVersion, account.getVersion())
                    .ge(delta < 0, ReaderPointsAccount::getBalance, -delta)
                    .set(ReaderPointsAccount::getBalance, balance)
                    .set(ReaderPointsAccount::getTotalEarned, earned)
                    .set(ReaderPointsAccount::getTotalSpent, spent)
                    .set(ReaderPointsAccount::getVersion, account.getVersion() + 1));
            if (updated > 0) {
                ReaderPointsLog log = new ReaderPointsLog();
                log.setUserId(userId);
                log.setChangeAmount(delta);
                log.setBalanceAfter(balance);
                log.setBizType(bizType);
                log.setBizId(bizId);
                log.setRemark(remark);
                logMapper.insert(log);
                return balance;
            }
        }
        throw new BizException("积分账户繁忙，请稍后重试");
    }

    private void markBitmap(Long userId, LocalDate date) {
        if (!redisEnabled) {
            return;
        }
        try {
            String key = "rv:signin:" + userId + ":" + YearMonth.from(date);
            redis.opsForValue().setBit(key, date.getDayOfMonth() - 1, true);
            redis.expire(key, Duration.ofDays(40));
        } catch (Exception e) {
            log.warn("Sign-in bitmap update failed: {}", e.getMessage());
        }
    }
}
