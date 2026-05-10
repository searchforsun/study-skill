package com.study.distributed.stage03;

import java.util.concurrent.TimeUnit;

/**
 * 超时链设计演示：分层超时配置
 *
 * 知识点：
 * 1. 每跳超时应小于上一跳的剩余时间
 * 2. 超时过短导致误判，过长导致雪崩
 * 3. 按下游 SLO（P99）设置超时基准
 */
public class TimeoutChainDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 超时链设计演示 ===\n");

        // 模拟调用链：Gateway -> Service A -> Service B -> DB
        // 假设各节点 P99 延迟：DB=50ms, B=80ms, A=120ms, Gateway 总超时=300ms

        System.out.println("【场景1】Gateway 总超时 300ms，各节点 P99 如下：");
        System.out.println("  DB: 50ms, Service B: 80ms, Service A: 120ms");
        System.out.println("  计算：Gateway 超时 > A P99 + B P99 + C P99 + 网络波动\n");

        TimeoutConfig config1 = new TimeoutConfig(
            50,   // DB 超时
            80,   // Service B 超时
            120,  // Service A 超时
            300   // Gateway 总超时
        );
        simulateTimeoutChain(config1);

        System.out.println("\n【场景2】Gateway 超时过短（100ms），各节点 P99 不变");
        System.out.println("  预期：大量误判，服务被错误标记为不可用\n");

        TimeoutConfig config2 = new TimeoutConfig(
            50, 80, 120, 100  // Gateway 总超时只有 100ms
        );
        simulateTimeoutChain(config2);

        System.out.println("\n=== 核心结论 ===");
        System.out.println("1. 超时设置应基于各节点 P99 + 网络波动余量");
        System.out.println("2. Gateway 超时是所有下游超时的上限，而非独立设置");
        System.out.println("3. 超时过短 = 高误判率；超时过长 = 雪崩风险");
    }

    private static void simulateTimeoutChain(TimeoutConfig config) {
        // 模拟 Gateway 调用
        long start = System.currentTimeMillis();
        boolean success = simulateCall("Service A", config.serviceATimeout);
        long elapsed = System.currentTimeMillis() - start;

        if (success) {
            System.out.printf("  Gateway 调用成功，总耗时 %dms%n", elapsed);
        } else {
            System.out.printf("  Gateway 调用超时（设置 %dms，实际 %dms）%n",
                config.gatewayTimeout, elapsed);
        }
    }

    private static boolean simulateCall(String serviceName, int timeoutMs) {
        try {
            // 模拟服务调用延迟（P99 延迟附近随机）
            int delay = (int) (timeoutMs * 0.8 + Math.random() * timeoutMs * 0.4);
            Thread.sleep(delay);
            return delay <= timeoutMs;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    static class TimeoutConfig {
        int dbTimeout;
        int serviceBTimeout;
        int serviceATimeout;
        int gatewayTimeout;

        TimeoutConfig(int db, int b, int a, int gateway) {
            this.dbTimeout = db;
            this.serviceBTimeout = b;
            this.serviceATimeout = a;
            this.gatewayTimeout = gateway;
        }
    }
}