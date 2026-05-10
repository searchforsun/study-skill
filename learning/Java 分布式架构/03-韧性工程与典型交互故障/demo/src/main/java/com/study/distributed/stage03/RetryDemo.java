package com.study.distributed.stage03;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 重试策略演示：指数退避 + jitter vs 固定间隔
 *
 * 知识点：
 * 1. 重试三原则：幂等/上限/退避
 * 2. 指数退避 + jitter 打散重试峰值
 * 3. 重试风暴的防御手段
 */
public class RetryDemo {

    private static final AtomicInteger totalAttempts = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 重试策略演示 ===\n");

        // 场景1：固定间隔重试（无退避）
        System.out.println("【场景1】固定间隔重试（无退避）");
        System.out.println("  问题：所有客户端在故障恢复瞬间同时重试，形成流量尖峰\n");
        simulateFixedRetry(3);

        // 场景2：指数退避 + jitter
        System.out.println("\n【场景2】指数退避 + jitter");
        System.out.println("  优势：重试间隔递增 + 随机偏移，打散峰值\n");
        simulateExponentialBackoffWithJitter(3);

        // 场景3：幂等性判断
        System.out.println("\n【场景3】幂等性判断示例");
        System.out.println("  读操作（GET）：可以重试");
        System.out.println("  写操作（POST/扣库存）：风险操作，次数要严格控制");
        demonstrateIdempotency();

        System.out.println("\n=== 核心结论 ===");
        System.out.println("1. 重试只在幂等操作上进行：读操作、带唯一键的写操作");
        System.out.println("2. 指数退避 + jitter 防止同步震荡");
        System.out.println("3. 重试次数上限（建议 ≤3）防止无限重试");
        System.out.println("4. 熔断兜底：当下游错误率高，停止重试直接降级");
    }

    private static void simulateFixedRetry(int maxRetries) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        System.out.println("  开始时间：" + startTime + "ms");

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            totalAttempts.incrementAndGet();
            long attemptTime = System.currentTimeMillis() - startTime;
            boolean success = simulateCall(50); // 模拟 50ms 延迟

            System.out.printf("    尝试 #%d 时间=%dms %s%n",
                attempt, attemptTime, success ? "成功" : "失败");

            if (success) break;

            // 固定间隔 100ms（无退避）
            TimeUnit.MILLISECONDS.sleep(100);
        }
    }

    private static void simulateExponentialBackoffWithJitter(int maxRetries) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        System.out.println("  开始时间：" + startTime + "ms");

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            totalAttempts.incrementAndGet();

            // 指数退避：100ms -> 200ms -> 400ms
            long baseDelay = (long) Math.pow(2, attempt - 1) * 100;
            // Jitter：±20%
            long jitter = (long) (Math.random() * baseDelay * 0.2);
            long delay = baseDelay + jitter;

            long attemptTime = System.currentTimeMillis() - startTime;
            boolean success = attempt >= 3; // 模拟第3次尝试后成功

            System.out.printf("    尝试 #%d 时间=%dms，延迟=%dms %s%n",
                attempt, attemptTime, delay, success ? "成功" : "失败");

            if (success) break;

            TimeUnit.MILLISECONDS.sleep(delay);
        }
    }

    private static void demonstrateIdempotency() {
        System.out.println("\n  【幂等】GET /api/products/123 - 可以安全重试");
        System.out.println("  【非幂等】POST /api/orders - 重试可能创建重复订单");
        System.out.println("  【非幂等】DELETE /api/cart/items/123 - 重试可能误删已删除的 item");
        System.out.println("\n  设计建议：");
        System.out.println("    1. 写操作使用唯一键（如订单号）防止重复");
        System.out.println("    2. 支付等关键操作用『查询确认后再处理』模式");
        System.out.println("    3. 使用分布式锁 + 幂等键双重保护");
    }

    private static boolean simulateCall(int delayMs) {
        try {
            Thread.sleep(delayMs);
            return Math.random() > 0.7; // 30% 概率失败
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}