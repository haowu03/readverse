package com.readverse.framework.ratelimit;

import com.readverse.common.exception.BizException;
import com.readverse.common.exception.ErrorCode;
import com.readverse.framework.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitAspect {

    private static final String LUA = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local rate = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local ttl = tonumber(ARGV[4])
            local data = redis.call('HMGET', key, 'tokens', 'ts')
            local tokens = tonumber(data[1])
            local ts = tonumber(data[2])
            if tokens == nil then tokens = capacity end
            if ts == nil then ts = now end
            local delta = math.max(0, now - ts)
            tokens = math.min(capacity, tokens + delta * rate)
            local allowed = 0
            if tokens >= 1 then
              tokens = tokens - 1
              allowed = 1
            end
            redis.call('HSET', key, 'tokens', tokens, 'ts', now)
            redis.call('EXPIRE', key, ttl)
            return allowed
            """;

    private final StringRedisTemplate redis;
    private final boolean enabled;
    private final boolean redisEnabled;
    private final DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA, Long.class);
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
    private final Map<String, Window> localWindows = new ConcurrentHashMap<>();

    public RateLimitAspect(StringRedisTemplate redis,
                           @Value("${rv.rate-limit.enabled:true}") boolean enabled,
                           @Value("${rv.redis.enabled:true}") boolean redisEnabled) {
        this.redis = redis;
        this.enabled = enabled;
        this.redisEnabled = redisEnabled;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        if (!enabled) {
            return point.proceed();
        }
        String key = buildKey(point, rateLimit);
        boolean allowed = redisEnabled ? tryRedis(key, rateLimit) : tryLocal(key, rateLimit);
        if (!allowed) {
            String message = StringUtils.hasText(rateLimit.message()) ? rateLimit.message() : ErrorCode.TOO_MANY_REQUESTS.getMessage();
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS, message);
        }
        return point.proceed();
    }

    private boolean tryRedis(String key, RateLimit rateLimit) {
        try {
            double now = System.currentTimeMillis() / 1000.0;
            double rate = (double) rateLimit.permits() / Math.max(1, rateLimit.windowSeconds());
            Long result = redis.execute(script, List.of("rv:rl:" + key),
                    String.valueOf(rateLimit.permits()),
                    String.valueOf(rate),
                    String.valueOf(now),
                    String.valueOf(Math.max(60, rateLimit.windowSeconds() * 2L)));
            return result != null && result == 1L;
        } catch (Exception e) {
            log.warn("Redis rate limit failed, fallback to local: {}", e.getMessage());
            return tryLocal(key, rateLimit);
        }
    }

    private boolean tryLocal(String key, RateLimit rateLimit) {
        long now = System.currentTimeMillis();
        long windowMillis = Math.max(1, rateLimit.windowSeconds()) * 1000L;
        Window window = localWindows.compute(key, (k, old) -> {
            if (old == null || old.resetAt <= now) {
                return new Window(now + windowMillis, 1);
            }
            return new Window(old.resetAt, old.count + 1);
        });
        return window.count <= rateLimit.permits();
    }

    private String buildKey(ProceedingJoinPoint point, RateLimit rateLimit) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        String businessKey = rateLimit.key();
        if (StringUtils.hasText(businessKey)) {
            try {
                MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                        null, method, point.getArgs(), discoverer);
                Object value = parser.parseExpression(businessKey).getValue(context);
                if (value != null) {
                    businessKey = String.valueOf(value);
                }
            } catch (Exception e) {
                log.debug("Rate limit key parse failed: {}", e.getMessage());
            }
        }
        if (!StringUtils.hasText(businessKey)) {
            businessKey = method.getDeclaringClass().getSimpleName() + ":" + method.getName();
        }
        String dimension = switch (rateLimit.dimension().toUpperCase()) {
            case "USER" -> "u:" + (SecurityUtils.getUserIdOrNull() == null ? "anonymous" : SecurityUtils.getUserIdOrNull());
            case "GLOBAL" -> "global";
            default -> "ip:" + clientIp();
        };
        return dimension + ":" + businessKey;
    }

    private String clientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record Window(long resetAt, int count) {
    }
}
