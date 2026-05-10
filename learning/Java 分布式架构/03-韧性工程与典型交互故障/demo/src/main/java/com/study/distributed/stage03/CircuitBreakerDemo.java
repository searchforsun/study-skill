package com.study.distributed.stage03;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 熔断器演示：三状态转换
 *
 * 知识点：
 * 1. 熔断器三状态：Closed / Open / Half-Open
 * 2. 失败率阈值触发熔断
 * 3. 半开状态逐步恢复流量
 */
public class CircuitBreakerDemo {

    private static final int FAILURE_THRESHOLD = 5;  // 5 次失败触发熔断
    private static final int SUCCESS_THRESHOLD = 3;   // 3 次成功恢复
    private static final long OPEN_TIMEOUT = 3000;    // 3 秒后尝试半开

    private static AtomicInteger failureCount = new AtomicInteger(0);
    private static AtomicInteger successCount = new AtomicInteger(0);
    private static State state = State.CLOSED;
    private static long lastFailureTime = 0;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 熔断器演示 ===\n");

        System.out.println("配置：失败阈值=" + FAILURE_THRESHOLD +
            "，成功恢复阈值=" + SUCCESS_THRESHOLD +
            "，Open 超时=" + OPEN_TIMEOUT + "ms\n");

        // 模拟调用：先触发熔断，再恢复
        simulateCalls();

        System.out.println("\n=== 核心结论 ===");
        System.out.println("1. Closed：正常调用，失败计数");
        System.out.println("2. Open：快速失败，不发往下游");
        System.out.println("3. Half-Open：放行试探，成功转 Closed，失败转 Open");
        System.out.println("4. 熔断后必须有降级策略，否则只是让用户看到错误");
    }

    private static void simulateCalls() throws InterruptedException {
        System.out.println("【阶段1】正常调用，触发熔断\n");

        // 模拟 10 次调用，其中 6 次失败（超过阈值触发熔断）
        for (int i = 1; i <= 10; i++) {
            boolean success = makeCall(i);
            System.out.printf("  调用 #%d [%s] 结果=%s%n",
                i, state, success ? "成功" : "失败");
            TimeUnit.MILLISECONDS.sleep(100);
        }

        System.out.println("\n【阶段2】熔断开启，所有调用快速失败\n");

        for (int i = 11; i <= 13; i++) {
            boolean success = makeCall(i);
            System.out.printf("  调用 #%d [%s] 结果=%s%n",
                i, state, success ? "成功" : "失败");
            TimeUnit.MILLISECONDS.sleep(100);
        }

        System.out.println("\n【阶段3】等待 " + OPEN_TIMEOUT + "ms，进入 Half-Open\n");
        TimeUnit.MILLISECONDS.sleep(OPEN_TIMEOUT);

        // Half-Open 状态：成功 3 次才能恢复
        System.out.println("【阶段3】Half-Open 试探恢复\n");

        for (int i = 14; i <= 16; i++) {
            boolean success = makeCall(i);
            System.out.printf("  调用 #%d [%s] 结果=%s%n",
                i, state, success ? "成功" : "失败");
            TimeUnit.MILLISECONDS.sleep(100);
        }
    }

    private static boolean makeCall(int callId) {
        switch (state) {
            case CLOSED:
                return callClosed(callId);
            case OPEN:
                return callOpen(callId);
            case HALF_OPEN:
                return callHalfOpen(callId);
            default:
                return false;
        }
    }

    private static boolean callClosed(int callId) {
        // 模拟：前 6 次调用高概率失败，触发熔断
        boolean shouldFail = callId <= 6 && Math.random() < 0.8;
        boolean success = !shouldFail;

        if (!success) {
            failureCount.incrementAndGet();
            lastFailureTime = System.currentTimeMillis();

            if (failureCount.get() >= FAILURE_THRESHOLD) {
                state = State.OPEN;
                System.out.println("    [状态切换] CLOSED -> OPEN（失败率超阈值）");
            }
        } else {
            successCount.incrementAndGet();
        }

        return success;
    }

    private static boolean callOpen(int callId) {
        // Open 状态：快速失败，不发往下游
        System.out.println("    [快速失败] 熔断器 Open，拒绝调用");
        return false;
    }

    private static boolean callHalfOpen(int callId) {
        // Half-Open：放行请求，模拟成功/失败
        boolean success = Math.random() > 0.3; // 70% 成功率

        if (success) {
            successCount.incrementAndGet();
            System.out.println("    [试探成功] successCount=" + successCount.get());

            if (successCount.get() >= SUCCESS_THRESHOLD) {
                state = State.CLOSED;
                failureCount.set(0);
                successCount.set(0);
                System.out.println("    [状态切换] HALF_OPEN -> CLOSED（恢复）");
            }
        } else {
            successCount.set(0);
            state = State.OPEN;
            System.out.println("    [状态切换] HALF_OPEN -> OPEN（试探失败）");
        }

        return success;
    }

    enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }
}