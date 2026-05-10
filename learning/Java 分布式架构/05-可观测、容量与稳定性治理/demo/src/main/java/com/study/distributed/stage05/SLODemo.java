package com.study.distributed.stage05;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * SLO、错误预算与容量规划演示
 *
 * 演示内容：
 * 1. SLO 的制定流程：从业务约束到技术承诺
 * 2. 错误预算计算与消耗速率驱动告警
 * 3. 容量规划：线程数/响应时间 → 理论 QPS
 * 4. 压测场景：饱和度判断与扩缩容决策
 *
 * 对应理论稿：三、SLO 与错误预算、四、容量规划
 */
public class SLODemo {

    // ==================== SLO 定义 ====================

    static class SLOConfig {
        String serviceName;
        double availabilityTarget;     // 可用率目标，如 99.9
        int latencyP99Target;           // P99 延迟目标（ms）
        int requestsPerMonth;            // 月请求量

        SLOConfig(String name, double availability, int latencyTarget, int monthlyRequests) {
            this.serviceName = name;
            this.availabilityTarget = availability;
            this.latencyP99Target = latencyTarget;
            this.requestsPerMonth = monthlyRequests;
        }

        double errorBudget() {
            return (1 - availabilityTarget / 100) * requestsPerMonth;
        }

        int dailyErrorBudget() {
            return (int) (errorBudget() / 30);
        }

        int weeklyErrorBudget() {
            return (int) (errorBudget() / 4);
        }
    }

    // ==================== 错误预算消耗追踪 ====================

    static class ErrorBudgetTracker {
        SLOConfig slo;
        int consumedErrors;
        long windowStartMs;

        ErrorBudgetTracker(SLOConfig slo) {
            this.slo = slo;
            this.consumedErrors = 0;
            this.windowStartMs = System.currentTimeMillis();
        }

        void recordErrors(int count) {
            consumedErrors += count;
        }

        /** 计算当前窗口的错误率 */
        double currentErrorRate() {
            // 简化：假设每秒请求数
            long elapsedSeconds = (System.currentTimeMillis() - windowStartMs) / 1000;
            if (elapsedSeconds == 0) return 0;
            return (double) consumedErrors / (elapsedSeconds * 100); // 假设 QPS=100
        }

        /** 消耗速率级别：1=正常，2=需关注，3=危险 */
        int consumptionRateLevel() {
            long elapsedMinutes = (System.currentTimeMillis() - windowStartMs) / 60000;
            if (elapsedMinutes == 0) return 1;

            // 计算小时维度消耗速率
            double errorsPerHour = consumedErrors * 60.0 / elapsedMinutes;
            double dailyBudget = slo.dailyErrorBudget();

            if (errorsPerHour > dailyBudget) {
                return 3; // 1 小时内消耗 1 天预算 = P1 紧急
            } else if (errorsPerHour > dailyBudget / 7) {
                return 2; // 1 天内消耗 1 周预算 = P2 重要
            }
            return 1; // 正常
        }

        void printStatus() {
            System.out.println("\n========== 错误预算状态 - " + slo.serviceName + " ==========");
            System.out.println("SLO: " + slo.availabilityTarget + "% 可用率，P99 < " + slo.latencyP99Target + "ms");
            System.out.println("月度错误预算: " + (int) slo.errorBudget() + " 次失败");
            System.out.println("日度错误预算: " + slo.dailyErrorBudget() + " 次失败");
            System.out.println("当前已消耗: " + consumedErrors + " 次");
            System.out.println("剩余: " + ((int) slo.errorBudget() - consumedErrors) + " 次");

            int level = consumptionRateLevel();
            System.out.print("消耗速率: ");
            switch (level) {
                case 1 -> System.out.println("✓ 正常（符合 SLO 预期）");
                case 2 -> System.out.println("⚡ P2 需关注（1天内将消耗1周预算）");
                case 3 -> System.out.println("🚨 P1 紧急（1小时内将消耗1天预算！）");
            }
        }
    }

    // ==================== 容量规划计算 ====================

    static class CapacityPlanner {
        int threadPoolSize;
        int avgLatencyMs;

        CapacityPlanner(int threads, int avgLatency) {
            this.threadPoolSize = threads;
            this.avgLatencyMs = avgLatency;
        }

        /** 理论最大 QPS（基于排队论简化） */
        int theoreticalMaxQPS() {
            // QPS = 线程数 / 平均响应时间(秒)
            return (int) (threadPoolSize * 1000.0 / avgLatencyMs);
        }

        /** 安全扩容建议（保留 30% 余量） */
        int safeCapacityQPS() {
            return (int) (theoreticalMaxQPS() * 0.7);
        }

        /** 当前负载百分比 */
        double loadPercentage(int currentQPS) {
            return (double) currentQPS / theoreticalMaxQPS() * 100;
        }

        void printAnalysis(int currentQPS, int targetP99) {
            System.out.println("\n========== 容量规划分析 ==========");
            System.out.println("线程池大小: " + threadPoolSize);
            System.out.println("平均响应时间: " + avgLatencyMs + "ms");
            System.out.println("理论最大 QPS: " + theoreticalMaxQPS());
            System.out.println("安全容量（含30%余量）: " + safeCapacityQPS());
            System.out.println("当前 QPS: " + currentQPS + " (" + String.format("%.1f%%", loadPercentage(currentQPS)) + ")");

            // 判断是否需要扩容
            double load = loadPercentage(currentQPS);
            System.out.println("\n扩缩容建议:");
            if (load < 50) {
                System.out.println("  ✓ 当前负载 < 50%，资源充裕，可缩减降低成本");
            } else if (load < 70) {
                System.out.println("  ⚡ 当前负载 50~70%，正常范围，持续监控");
            } else if (load < 85) {
                System.out.println("  ⚠ 当前负载 70~85%，接近饱和，准备扩容");
            } else {
                System.out.println("  🚨 当前负载 > 85%，立即扩容 + 检查降级策略");
            }

            // P99 vs 目标对比
            int expectedP99 = (int) (avgLatencyMs * 2.5); // 粗略估算 P99 ≈ 2.5 * avg
            System.out.println("\n延迟分析:");
            System.out.println("  当前平均延迟: " + avgLatencyMs + "ms");
            System.out.println("  预估 P99: " + expectedP99 + "ms");
            System.out.println("  目标 P99: " + targetP99 + "ms");
            if (expectedP99 > targetP99) {
                System.out.println("  ⚠ 预估 P99 超过目标，SLO 告警将触发");
            } else {
                System.out.println("  ✓ 预估 P99 在 SLA 范围内");
            }
        }
    }

    // ==================== 压测场景模拟 ====================

    static void simulateLoadTest() {
        System.out.println("\n========== 压测场景模拟 ==========");

        Random random = ThreadLocalRandom.current();

        // 模拟不同负载下的表现
        int[] targetQPS = {500, 1000, 1500, 2000, 2500};
        int threads = 200;
        int avgLatencyBase = 100; // ms

        System.out.println("线程池: " + threads + ", 基准平均延迟: " + avgLatencyBase + "ms");
        System.out.println("\n压测结果:");

        for (int qps : targetQPS) {
            // 负载增加时，延迟非线性增长（排队效应）
            double loadFactor = (double) qps / (threads * 1000.0 / avgLatencyBase);
            int avgLatency = (int) (avgLatencyBase * (1 + Math.pow(loadFactor, 1.5)));
            int p99Latency = (int) (avgLatency * (1.5 + loadFactor * 0.5));

            // 饱和度判断
            double saturation = Math.min(1.0, loadFactor);
            String saturationLabel = saturation < 0.7 ? "正常" : saturation < 0.85 ? "接近饱和" : "严重饱和";

            System.out.printf("  QPS=%5d | Avg=%4dms | P99=%5dms | 饱和度=%s%n",
                qps, avgLatency, p99Latency, saturationLabel);
        }

        System.out.println("\n关键观察:");
        System.out.println("1. 延迟不是线性增长的 — QPS 翻倍，延迟可能增长 3~5 倍");
        System.out.println("2. 连接池/线程池饱和时，延迟在排队中成倍增长，CPU 可能看起来正常");
        System.out.println("3. P99 > 平均值 × 2 通常意味着存在资源争抢");
    }

    // ==================== 降级决策演示 ====================

    static void demonstrateDegradation(SLOConfig slo) {
        System.out.println("\n========== 基于 SLO 的降级决策 ==========");

        // 模拟不同错误预算消耗场景
        Map<String, Integer> currentErrors = Map.of(
            "支付链路", 0,
            "推荐服务", 150,
            "积分服务", 300
        );

        System.out.println("当前状态:");
        System.out.println("  - 支付链路: 正常");
        System.out.println("  - 推荐服务: P99=1.2s，错误率 2%");
        System.out.println("  - 积分服务: P99=3s，错误率 5%");

        System.out.println("\n降级策略（按 SLO 优先级）:");
        System.out.println("1. 支付链路 — P0：保持全部功能，不降级");
        System.out.println("   → 条件：支付服务错误预算消耗 > 80%/小时");
        System.out.println("   → 动作：关闭非核心服务调用（推荐、积分），保护支付");

        System.out.println("\n2. 推荐服务 — P1：返回兜底数据，不阻塞主流程");
        System.out.println("   → 条件：推荐 P99 > 2s 或错误率 > 3%");
        System.out.println("   → 动作：返回默认推荐列表，异步更新缓存");

        System.out.println("\n3. 积分服务 — P2：接受延迟积压，最终一致即可");
        System.out.println("   → 条件：积分错误率 > 5% 或响应时间 > 5s");
        System.out.println("   → 动作：队列削峰，延迟处理，保证最终一致");

        System.out.println("\n降级配置（应提前写入 YAML/配置中心）:");
        System.out.println("  degradation:  # 开关配置，非临时改代码");
        System.out.println("    recommend_service:");
        System.out.println("      enabled: true");
        System.out.println("      fallback_data: [\"item_1\", \"item_2\", \"item_3\"]");
        System.out.println("      trigger_threshold:");
        System.out.println("        p99_latency_ms: 2000");
        System.out.println("        error_rate_percent: 3");
    }

    // ==================== 主演示流程 ====================

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║  阶段 5 演示：SLO、错误预算与容量规划                           ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");

        // 场景：电商订单服务的 SLO 配置
        SLOConfig orderSLO = new SLOConfig(
            "订单服务",
            99.5,     // 99.5% 可用率（月允许 3.6 小时故障）
            500,      // P99 延迟 < 500ms
            100_000_000 // 月请求量 1 亿
        );

        // SLO 配置展示
        System.out.println("\n========== SLO 配置 ==========");
        System.out.println("服务: " + orderSLO.serviceName);
        System.out.println("可用率目标: " + orderSLO.availabilityTarget + "%");
        System.out.println("P99 延迟目标: < " + orderSLO.latencyP99Target + "ms");
        System.out.println("月请求量: " + String.format("%,d", orderSLO.requestsPerMonth));
        System.out.println("月度错误预算: " + (int) orderSLO.errorBudget() + " 次失败（允许）");
        System.out.println("日度错误预算: " + orderSLO.dailyErrorBudget() + " 次失败（允许）");

        // 错误预算消耗模拟
        ErrorBudgetTracker tracker = new ErrorBudgetTracker(orderSLO);

        // 模拟正常运营
        System.out.println("\n[模拟] 正常运营 30 分钟:");
        tracker.recordErrors(5); // 少量随机失败

        // 模拟故障注入
        System.out.println("[模拟] 数据库抖动 10 分钟:");
        for (int i = 0; i < 10; i++) {
            tracker.recordErrors(20); // 每分钟 20 次失败，远超日预算
        }

        tracker.printStatus();

        // 容量规划
        CapacityPlanner planner = new CapacityPlanner(200, 100);
        planner.printAnalysis(1500, 500);

        // 压测场景
        simulateLoadTest();

        // 降级决策
        demonstrateDegradation(orderSLO);

        // 关键结论
        System.out.println("\n========== 演示结论 ==========");
        System.out.println("1. SLO 制定流程：业务约束 → 技术实现 → 承诺值 → 告警阈值");
        System.out.println("2. 错误预算 = (1 - SLO) × 月请求数，是'允许失败的配额'");
        System.out.println("3. 告警阈值基于消耗速率：1小时消耗1天预算 = P1 紧急");
        System.out.println("4. 容量规划公式：理论 QPS = 线程数 / 平均响应时间（秒）");
        System.out.println("5. 延迟非线性增长 — QPS 翻倍，延迟可能增长 3~5 倍");
        System.out.println("6. 降级策略必须提前配置（开关 + 阈值），故障时不能临时改代码");
        System.out.println("\n对应理论稿章节：三、SLO 与错误预算、四、容量规划");
    }
}