package com.study.distributed.stage05;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * 告警治理：降噪与可信演示
 *
 * 演示内容：
 * 1. 固定阈值 vs 动态基线的对比
 * 2. 告警聚合与同源抑制
 * 3. 维护窗口与发布期告警抑制
 * 4. 速率驱动告警级别判断
 *
 * 对应理论稿：五、告警治理
 */
public class AlertDemo {

    // ==================== 告警数据结构 ====================

    static class Alert {
        String name;
        String service;
        String metric;
        double value;
        double threshold;
        long timestamp;
        int severity; // 1=P1 紧急, 2=P2 重要, 3=P3 观察

        Alert(String name, String service, String metric, double value, double threshold, int severity) {
            this.name = name;
            this.service = service;
            this.metric = metric;
            this.value = value;
            this.threshold = threshold;
            this.timestamp = System.currentTimeMillis();
            this.severity = severity;
        }

        String severityLabel() {
            return switch (severity) {
                case 1 -> "P1 紧急";
                case 2 -> "P2 重要";
                case 3 -> "P3 观察";
                default -> "未知";
            };
        }

        void print() {
            System.out.printf("  [%s] %s (%s) 当前值=%.1f, 阈值=%.1f%n",
                severityLabel(), name, service, value, threshold);
        }
    }

    // ==================== 固定阈值 vs 动态基线 ====================

    static class AlertRule {
        String name;
        String metric;
        double fixedThreshold;         // 固定阈值
        double baselineMultiplier;     // 动态基线倍数
        boolean useDynamicBaseline;

        AlertRule(String name, String metric, double fixedThreshold, boolean useDynamic) {
            this.name = name;
            this.metric = metric;
            this.fixedThreshold = fixedThreshold;
            this.useDynamicBaseline = useDynamic;
            this.baselineMultiplier = 1.2;
        }

        boolean shouldAlert(double currentValue, double baseline) {
            if (useDynamicBaseline) {
                return currentValue > baseline * baselineMultiplier;
            }
            return currentValue > fixedThreshold;
        }

        double threshold(double baseline) {
            return useDynamicBaseline ? baseline * baselineMultiplier : fixedThreshold;
        }
    }

    static void demonstrateDynamicBaseline() {
        System.out.println("\n========== 固定阈值 vs 动态基线 ==========");

        AlertRule[] rules = {
            new AlertRule("CPU 固定阈值", "cpu", 80, false),
            new AlertRule("CPU 动态基线", "cpu", 80, true)
        };

        // 模拟业务周期
        Map<String, double[]> businessCycle = Map.of(
            "凌晨 1:00", new double[]{25, 0.6},    // 凌晨：CPU 低，基线低
            "上午 10:00", new double[]{75, 0.8},   // 高峰：CPU 高，基线也高
            "下午 3:00", new double[]{85, 0.9}     // 峰值：CPU 更高
        );

        System.out.println("\n业务周期 CPU 监控对比:");

        for (Map.Entry<String, double[]> entry : businessCycle.entrySet()) {
            String time = entry.getKey();
            double cpu = entry.getValue()[0];
            double baseline = entry.getValue()[1] * 100; // 基线

            System.out.println("\n  " + time + " — CPU=" + (int) cpu + "%, 基线=" + (int) baseline + "%");

            for (AlertRule rule : rules) {
                boolean alert = rule.shouldAlert(cpu, baseline);
                System.out.printf("    %s: 阈值=%.0f%%, 触发=%s%n",
                    rule.name, rule.threshold(baseline), alert ? "✓ 是" : "✗ 否");
            }
        }

        System.out.println("\n关键对比:");
        System.out.println("  固定阈值 80%：凌晨误报（CPU 25% 但超过阈值），上午正确，峰值误报");
        System.out.println("  动态基线：凌晨正常，上午正常，峰值（相对基线 90%）可能告警但更合理");
    }

    // ==================== 告警聚合与抑制 ====================

    static class AlertAggregator {
        // 模拟 AlertManager 的抑制规则
        Map<String, List<Alert>> alerts = new LinkedHashMap<>();

        /** 聚合同服务多实例告警 */
        List<Alert> aggregateByService(String service) {
            List<Alert> serviceAlerts = alerts.getOrDefault(service, Collections.emptyList());
            if (serviceAlerts.size() <= 1) return serviceAlerts;

            // 按 metric 分组合并
            Map<String, List<Alert>> byMetric = new HashMap<>();
            for (Alert alert : serviceAlerts) {
                byMetric.computeIfAbsent(alert.metric, k -> new ArrayList<>()).add(alert);
            }

            System.out.println("  聚合前: " + serviceAlerts.size() + " 条告警");
            System.out.println("  聚合后: " + byMetric.size() + " 条（按 metric 分组）");

            // 返回聚合后的代表性告警（取最严重的）
            List<Alert> aggregated = new ArrayList<>();
            for (Map.Entry<String, List<Alert>> entry : byMetric.entrySet()) {
                Alert worst = entry.getValue().stream()
                    .min(Comparator.comparingInt(a -> a.severity))
                    .orElse(entry.getValue().get(0));
                aggregated.add(worst);
            }
            return aggregated;
        }

        /** 上游告警触发时抑制下游 */
        boolean shouldSuppress(String downstreamService, Map<String, Boolean> upstreamAlerts) {
            for (String upstream : upstreamAlerts.keySet()) {
                if (upstreamAlerts.get(upstream) && isDownstream(upstream, downstreamService)) {
                    System.out.println("  抑制: " + downstreamService + "（上游 " + upstream + " 已告警）");
                    return true;
                }
            }
            return false;
        }

        boolean isDownstream(String upstream, String downstream) {
            // 简化的服务依赖关系
            Map<String, List<String>> deps = Map.of(
                "gateway", List.of("order-service", "user-service"),
                "order-service", List.of("inventory-service", "payment-service"),
                "user-service", List.of("cache-service", "db-service")
            );
            return deps.getOrDefault(upstream, Collections.emptyList()).contains(downstream);
        }
    }

    static void demonstrateAlertAggregation() {
        System.out.println("\n========== 告警聚合与抑制 ==========");

        AlertAggregator aggregator = new AlertAggregator();

        // 模拟同一服务的多个 Pod 告警
        List<Alert> podAlerts = List.of(
            new Alert("order-service CPU 高", "order-service", "cpu", 85, 80, 2),
            new Alert("order-service CPU 高", "order-service", "cpu", 88, 80, 2),
            new Alert("order-service CPU 高", "order-service", "cpu", 82, 80, 2)
        );

        aggregator.alerts.put("order-service", new ArrayList<>(podAlerts));

        System.out.println("\n场景：订单服务 3 个 Pod 同时 CPU 高");
        System.out.println("聚合结果:");
        List<Alert> aggregated = aggregator.aggregateByService("order-service");
        for (Alert a : aggregated) {
            a.print();
        }

        // 模拟上下游抑制
        Map<String, Boolean> upstreamStatus = Map.of(
            "gateway", true,         // 网关告警
            "order-service", false
        );

        System.out.println("\n场景：上游网关告警，抑制下游告警");
        System.out.println("  订单服务: " + (aggregator.shouldSuppress("order-service", upstreamStatus) ? "抑制" : "正常触发"));
        System.out.println("  用户服务: " + (aggregator.shouldSuppress("user-service", upstreamStatus) ? "抑制" : "正常触发"));
    }

    // ==================== 速率驱动告警级别 ====================

    static class RateBasedAlertLevel {
        String alertName;
        int consumedBudget;        // 已消耗的错误数
        long windowStartMs;

        RateBasedAlertLevel(String name) {
            this.alertName = name;
            this.consumedBudget = 0;
            this.windowStartMs = System.currentTimeMillis();
        }

        void recordErrors(int count) {
            consumedErrors(count);
        }

        synchronized void consumedErrors(int count) {
            this.consumedBudget += count;
        }

        /** 计算消耗速率级别 */
        int calculateLevel(int dailyBudget, int weeklyBudget) {
            long elapsedMinutes = (System.currentTimeMillis() - windowStartMs) / 60000;
            if (elapsedMinutes == 0) return 1;

            double errorsPerHour = consumedBudget * 60.0 / elapsedMinutes;

            System.out.println("\n  错误消耗分析:");
            System.out.println("    窗口: " + elapsedMinutes + " 分钟");
            System.out.println("    消耗: " + consumedBudget + " 次");
            System.out.println("    速率: " + String.format("%.1f", errorsPerHour) + " 次/小时");
            System.out.println("    日预算: " + dailyBudget + " 次");

            if (errorsPerHour > dailyBudget) {
                System.out.println("    → P1 紧急：1 小时内将消耗 1 天预算");
                return 1;
            } else if (errorsPerHour > weeklyBudget / 7.0 / 24) {
                System.out.println("    → P2 重要：1 天内将消耗 1 周预算");
                return 2;
            }
            System.out.println("    → P3 观察：在预算范围内");
            return 3;
        }
    }

    static void demonstrateRateBasedAlerting() {
        System.out.println("\n========== 速率驱动告警级别 ==========");

        int monthlyBudget = 1000;        // 月度错误预算 1000 次
        int dailyBudget = monthlyBudget / 30;   // 日预算 33 次
        int weeklyBudget = monthlyBudget / 4;  // 周预算 250 次

        System.out.println("SLO 配置:");
        System.out.println("  月度错误预算: " + monthlyBudget + " 次");
        System.out.println("  日预算: " + dailyBudget + " 次");
        System.out.println("  周预算: " + weeklyBudget + " 次");

        RateBasedAlertLevel tracker = new RateBasedAlertLevel("订单服务延迟告警");

        // 模拟正常运营
        System.out.println("\n[场景1] 正常运营 — 错误率 0.1%");
        for (int i = 0; i < 60; i++) { // 60 分钟
            tracker.recordErrors(1); // 每分钟 1 次
        }
        tracker.calculateLevel(dailyBudget, weeklyBudget);

        // 重置追踪器，模拟故障
        tracker = new RateBasedAlertLevel("订单服务延迟告警");
        System.out.println("\n[场景2] 数据库抖动 — 错误率 2%，持续 30 分钟");
        for (int i = 0; i < 30; i++) {
            tracker.recordErrors(30); // 每分钟 30 次
        }
        tracker.calculateLevel(dailyBudget, weeklyBudget);

        System.out.println("\n速率驱动 vs 固定阈值:");
        System.out.println("  固定阈值：'错误数 > 50 就告警'");
        System.out.println("  速率驱动：'1 小时内消耗 1 天预算就告警 P1'");
        System.out.println("\n速率驱动的优势:");
        System.out.println("  - 告警紧迫性与业务影响挂钩");
        System.out.println("  - 凌晨低流量时不会因固定阈值误报");
        System.out.println("  - 大促高峰时能更早发现问题");
    }

    // ==================== 告警规则设计模板 ====================

    static void demonstrateAlertRuleTemplate() {
        System.out.println("\n========== 告警规则设计模板 ==========");

        System.out.println("""
            每条告警规则应包含以下要素：

            ┌─────────────────────────────────────────────────────────────┐
            │ 告警规则: order_service_p99_latency_slo_breach              │
            ├─────────────────────────────────────────────────────────────┤
            │ 条件:                                                       │
            │   histogram_quantile(0.99,                                  │
            │     rate(http_request_duration_seconds_bucket{              │
            │       service="order"}[5m])) > 0.5                          │
            │   AND                                                       │
            │   rate(http_requests_total{                                 │
            │     service="order", status=~"5.."}[5m])                   │
            │   / rate(http_requests_total{service="order"}[5m])          │
            │   > 0.01                                                    │
            │ 持续时间: 5m (for: 5m)                                      │
            │ 标签: severity=p2, team=order-platform, slo=order_latency  │
            │ 描述: 订单服务 P99 超过 500ms 或错误率超过 1%，持续 5 分钟  │
            ├─────────────────────────────────────────────────────────────┤
            │ 操作指南:                                                   │
            │   1. 查看 SkyWalking 链路追踪定位慢请求根因                   │
            │   2. 检查下游依赖（支付服务、库存服务）                      │
            │   3. 如是数据库瓶颈，检查慢查询日志                          │
            │   4. 如超时来源不明，注入故障隔离                           │
            │   5. 若错误预算消耗速率达到 P1 阈值，启动降级预案            │
            └─────────────────────────────────────────────────────────────┘
            """);
    }

    // ==================== 维护窗口 ====================

    static class MaintenanceWindow {
        String service;
        long windowStartMs;
        long windowDurationMs;
        boolean inWindow;

        MaintenanceWindow(String service, long start, long durationMs) {
            this.service = service;
            this.windowStartMs = start;
            this.windowDurationMs = durationMs;
            this.inWindow = isCurrentlyInWindow();
        }

        boolean isCurrentlyInWindow() {
            long now = System.currentTimeMillis();
            return now >= windowStartMs && now < windowStartMs + windowDurationMs;
        }
    }

    static void demonstrateMaintenanceWindow() {
        System.out.println("\n========== 维护窗口与发布期告警抑制 ==========");

        MaintenanceWindow window = new MaintenanceWindow(
            "order-service",
            System.currentTimeMillis(), // 现在开始
            30 * 60 * 1000             // 持续 30 分钟
        );

        System.out.println("当前维护窗口: " + window.service);
        System.out.println("  开始: " + new Date(window.windowStartMs));
        System.out.println("  结束: " + new Date(window.windowStartMs + window.windowDurationMs));
        System.out.println("  当前是否在窗口内: " + (window.inWindow ? "✓ 是" : "✗ 否"));

        System.out.println("\n发布期告警策略:");
        System.out.println("  1. 维护窗口开启时，该服务告警自动抑制");
        System.out.println("  2. 维护窗口结束前 5 分钟，自动解除抑制");
        System.out.println("  3. 窗口结束后，若存在持续告警，重新触发并升级");
        System.out.println("  4. 配置示例 (AlertManager inhibit_rules):");
        System.out.println("""
               - target_match:
                   maintenance_window: "true"
                 source_match:
                   severity: "critical"
                 equal: ["service"]
                 action: "suppress"
            """);
    }

    // ==================== 主演示流程 ====================

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║  阶段 5 演示：告警治理 — 降噪与可信                            ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");

        // 固定阈值 vs 动态基线
        demonstrateDynamicBaseline();

        // 告警聚合与抑制
        demonstrateAlertAggregation();

        // 速率驱动告警级别
        demonstrateRateBasedAlerting();

        // 告警规则设计模板
        demonstrateAlertRuleTemplate();

        // 维护窗口
        demonstrateMaintenanceWindow();

        // 关键结论
        System.out.println("\n========== 演示结论 ==========");
        System.out.println("1. 告警质量准则：可行动 + 紧急性分级（不是数字难看就告警）");
        System.out.println("2. 动态基线：固定阈值不适应业务周期（凌晨 25% CPU ≠ 故障）");
        System.out.println("3. 聚合抑制：同源告警合并，上游告警时抑制下游");
        System.out.println("4. 速率驱动：1 小时内消耗 1 天预算 = P1 紧急（与流量无关）");
        System.out.println("5. 维护窗口：发布/升级期间的服务抖动不应触发生产告警");
        System.out.println("6. 每条告警必须有操作指南：收到告警后下一步做什么");
        System.out.println("\n对应理论稿章节：五、告警治理");
    }
}