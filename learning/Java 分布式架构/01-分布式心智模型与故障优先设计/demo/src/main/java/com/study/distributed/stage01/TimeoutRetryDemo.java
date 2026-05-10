package com.study.distributed.stage01;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 超时与重试边界行为演示
 *
 * 知识点：
 * 1. 超时判断是概率决策，不是精确查表
 * 2. 无退避的重试会放大雪崩
 * 3. 指数退避 + jitter 的效果
 */
public class TimeoutRetryDemo {

    private static final AtomicInteger attemptCount = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 超时与重试边界演示 ===\n");

        // 场景1：超时过短导致误判
        System.out.println("【场景1】超时阈值 10ms（过短）");
        simulateCallsWithTimeout(10, 5);

        // 场景2：合理超时
        System.out.println("\n【场景2】超时阈值 500ms（合理）");
        simulateCallsWithTimeout(500, 5);

        // 场景3：重试风暴（无退避）
        System.out.println("\n【场景3】固定间隔重试（无退避）— 易形成同步震荡");
        simulateRetriesWithoutBackoff(3);

        // 场景4：指数退避 + jitter
        System.out.println("\n【场景4】指数退避 + jitter — 流量更平滑");
        simulateRetriesWithBackoff(3);

        System.out.println("\n=== 核心结论 ===");
        System.out.println("1. 超时设置应基于依赖服务的 P99 延迟，而非拍脑袋");
        System.out.println("2. 无退避重试会在故障时产生流量尖峰，放大雪崩");
        System.out.println("3. 重试只在读操作或明确幂等操作上进行");
    }

    private static void simulateCallsWithTimeout(int timeoutMs, int callCount) throws InterruptedException {
        for (int i = 0; i < callCount; i++) {
            long start = System.currentTimeMillis();
            boolean success = simulateNetworkCall(timeoutMs);
            long elapsed = System.currentTimeMillis() - start;

            if (success) {
                System.out.printf("  调用成功，耗时 %dms%n", elapsed);
            } else {
                System.out.printf("  超时（设置 %dms，实际 %dms）%n", timeoutMs, elapsed);
            }
            TimeUnit.MILLISECONDS.sleep(100); // 间隔
        }
    }

    private static void simulateRetriesWithoutBackoff(int maxRetries) throws InterruptedException {
        int attempt = 0;
        long startTime = System.currentTimeMillis();
        while (attempt < maxRetries) {
            attempt++;
            attemptCount.incrementAndGet();
            boolean success = simulateNetworkCall(200); // 模拟慢服务
            long elapsed = System.currentTimeMillis() - startTime;
            System.out.printf("  重试 #%d，时间 %dms，%s%n", attempt, elapsed,
                success ? "成功" : "失败（继续重试）");
            if (success) break;
            // 无退避，立即重试
            TimeUnit.MILLISECONDS.sleep(100); // 固定间隔
        }
    }

    private static void simulateRetriesWithBackoff(int maxRetries) throws InterruptedException {
        int attempt = 0;
        long startTime = System.currentTimeMillis();
        while (attempt < maxRetries) {
            attempt++;
            attemptCount.incrementAndGet();
            boolean success = simulateNetworkCall(200);
            long elapsed = System.currentTimeMillis() - startTime;
            System.out.printf("  重试 #%d，时间 %dms，%s%n", attempt, elapsed,
                success ? "成功" : "失败");
            if (success) break;
            // 指数退避 + jitter
            long baseDelay = (long) Math.pow(2, attempt - 1) * 100;
            long jitter = (long) (Math.random() * baseDelay * 0.2);
            long delay = baseDelay + jitter;
            System.out.printf("    等待 %dms 后重试（含 jitter）%n", delay);
            TimeUnit.MILLISECONDS.sleep(delay);
        }
    }

    // 模拟网络调用：95% 概率在 50-150ms 返回，5% 概率 300ms+
    private static boolean simulateNetworkCall(int timeoutMs) {
        try {
            int delay = Math.random() < 0.95
                ? (int) (50 + Math.random() * 100)
                : (int) (300 + Math.random() * 200);
            Thread.sleep(delay);
            return delay < timeoutMs;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}