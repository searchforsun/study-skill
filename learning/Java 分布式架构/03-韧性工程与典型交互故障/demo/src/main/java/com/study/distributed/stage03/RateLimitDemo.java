package com.study.distributed.stage03;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流算法演示：令牌桶 vs 滑动窗口
 *
 * 知识点：
 * 1. 令牌桶：平滑突发流量，按固定速率发放令牌
 * 2. 滑动窗口：精确限流，统计时间窗口内请求数
 * 3. 限流层级：入口限流 vs 服务间限流
 */
public class RateLimitDemo {

    private static final int RATE_LIMIT = 5; // 每秒允许 5 个请求

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 限流算法演示 ===\n");
        System.out.println("限流阈值：" + RATE_LIMIT + " 请求/秒\n");

        // 场景1：令牌桶限流
        System.out.println("【场景1】令牌桶限流（平滑突发）");
        System.out.println("  令牌以固定速率发放，请求消耗令牌\n");
        TokenBucket tokenBucket = new TokenBucket(RATE_LIMIT);
        simulateTokenBucket(tokenBucket, 10); // 10 个并发请求

        System.out.println("\n【场景2】滑动窗口限流（精确统计）");
        System.out.println("  统计滑动时间窗口内的请求数\n");
        SlidingWindowRateLimiter slidingWindow = new SlidingWindowRateLimiter(RATE_LIMIT, 1);
        simulateSlidingWindow(slidingWindow, 10);

        System.out.println("\n=== 核心结论 ===");
        System.out.println("1. 令牌桶：允许突发（攒够令牌时可一次处理多个），流量平滑");
        System.out.println("2. 滑动窗口：精确控制，单位时间内请求数严格受限");
        System.out.println("3. 限流位置：入口（网关）限流 vs 服务间限流，作用不同");
        System.out.println("4. 限流后处理：拒绝（快速失败）vs 排队（延迟处理）");
    }

    private static void simulateTokenBucket(TokenBucket bucket, int requestCount) throws InterruptedException {
        AtomicInteger allowed = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);

        for (int i = 1; i <= requestCount; i++) {
            final int reqId = i;
            if (bucket.tryAcquire()) {
                System.out.printf("  请求 #%d [允许] 当前令牌剩余=%d%n",
                    reqId, bucket.availableTokens());
                allowed.incrementAndGet();
            } else {
                System.out.printf("  请求 #%d [拒绝] 令牌不足，当前=%d，需要=1%n",
                    reqId, bucket.availableTokens());
                rejected.incrementAndGet();
            }
            TimeUnit.MILLISECONDS.sleep(50); // 模拟请求间隔
        }

        System.out.printf("  结果：允许=%d，拒绝=%d%n", allowed.get(), rejected.get());
    }

    private static void simulateSlidingWindow(SlidingWindowRateLimiter limiter, int requestCount) throws InterruptedException {
        AtomicInteger allowed = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);

        for (int i = 1; i <= requestCount; i++) {
            final int reqId = i;
            if (limiter.tryAcquire()) {
                System.out.printf("  请求 #%d [允许] 窗口内请求数=%d/%d%n",
                    reqId, limiter.currentCount(), limiter.windowSize());
                allowed.incrementAndGet();
            } else {
                System.out.printf("  请求 #%d [拒绝] 窗口内请求数已满=%d/%d%n",
                    reqId, limiter.currentCount(), limiter.windowSize());
                rejected.incrementAndGet();
            }
            TimeUnit.MILLISECONDS.sleep(50);
        }

        System.out.printf("  结果：允许=%d，拒绝=%d%n", allowed.get(), rejected.get());
    }

    // ==================== 令牌桶实现 ====================

    static class TokenBucket {
        private final int capacity;
        private final double refillRate; // 每秒补充令牌数
        private double tokens;
        private long lastRefillTime;

        TokenBucket(int rate) {
            this.capacity = rate;
            this.refillRate = rate;
            this.tokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }

        synchronized boolean tryAcquire() {
            refill();
            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            double elapsed = (now - lastRefillTime) / 1000.0;
            tokens = Math.min(capacity, tokens + elapsed * refillRate);
            lastRefillTime = now;
        }

        double availableTokens() {
            return tokens;
        }
    }

    // ==================== 滑动窗口限流器实现 ====================

    static class SlidingWindowRateLimiter {
        private final int maxRequests;
        private final long windowMillis;
        private final long[] timestamps;
        private int index = 0;

        SlidingWindowRateLimiter(int rate, int windowSeconds) {
            this.maxRequests = rate;
            this.windowMillis = windowSeconds * 1000L;
            this.timestamps = new long[rate];
        }

        synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            // 清除窗口外的请求
            long windowStart = now - windowMillis;
            int count = 0;
            for (int i = 0; i < maxRequests; i++) {
                if (timestamps[i] > windowStart) {
                    count++;
                }
            }

            if (count < maxRequests) {
                timestamps[index] = now;
                index = (index + 1) % maxRequests;
                return true;
            }
            return false;
        }

        int currentCount() {
            long now = System.currentTimeMillis();
            long windowStart = now - windowMillis;
            int count = 0;
            for (int i = 0; i < maxRequests; i++) {
                if (timestamps[i] > windowStart) {
                    count++;
                }
            }
            return count;
        }

        int windowSize() {
            return maxRequests;
        }
    }
}