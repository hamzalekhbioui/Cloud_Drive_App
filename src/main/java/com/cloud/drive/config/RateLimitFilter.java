package com.cloud.drive.config;

import com.cloud.drive.repository.SubscriptionRepository;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-user (or per-IP for guests) rate limiter.
 * Limits are refreshed every 5 minutes so plan upgrades take effect promptly.
 *
 *   FREE      → 100 req / min
 *   PRO       → 500 req / min
 *   BUSINESS  → 2 000 req / min
 *   Guest IP  →  30 req / min
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record BucketEntry(Bucket bucket, long refreshAt) {}

    private final Map<String, BucketEntry> buckets = new ConcurrentHashMap<>();
    private final SubscriptionRepository subscriptionRepo;

    public RateLimitFilter(SubscriptionRepository subscriptionRepo) {
        this.subscriptionRepo = subscriptionRepo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String key = resolveKey(request);
        Bucket bucket = getOrRefresh(key);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Too many requests\",\"retryAfterSeconds\":60,\"status\":429}");
        }
    }

    private String resolveKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
            return "user:" + auth.getName();
        }
        return "ip:" + clientIp(request);
    }

    private Bucket getOrRefresh(String key) {
        long now = System.currentTimeMillis();
        BucketEntry entry = buckets.get(key);
        if (entry != null && now < entry.refreshAt()) {
            return entry.bucket();
        }
        Bucket bucket = buildBucket(key);
        buckets.put(key, new BucketEntry(bucket, now + 300_000L));
        return bucket;
    }

    private Bucket buildBucket(String key) {
        long capacity;
        if (key.startsWith("user:")) {
            String email = key.substring(5);
            String plan = subscriptionRepo.findByUserEmail(email)
                    .map(s -> s.getPlan()).orElse("FREE");
            capacity = switch (plan) {
                case "PRO"      -> 500L;
                case "BUSINESS" -> 2000L;
                default         -> 100L;
            };
        } else {
            capacity = 30L;
        }
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(capacity, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}