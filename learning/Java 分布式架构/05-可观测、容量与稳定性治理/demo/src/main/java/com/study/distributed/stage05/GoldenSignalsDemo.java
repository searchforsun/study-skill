package com.study.distributed.stage05;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 黄金信号与 RED/USE 框架演示
 *
 * 演示内容：
 * 1. 黄金信号（Latency/Traffic/Errors/Saturation）的定义与采集
 * 2. RED 框架：Rate（请求率）、Errors（错误率）、Duration（延迟分布）
 * 3. USE 框架：Utilization（利用率）、Saturation（饱和度）、Errors（错误）
 * 4. P99 计算与业务含义
 *
 * 对应理论稿：一、二（可观测三根 + 黄金信号框架）
 */
public class GoldenSignalsDemo {

    // ==================== 数据结构定义 ====================

    /** 单次请求记录 */
    static class Request {
        long timestamp;
        int durationMs;
        boolean success;
        String service;

        Request(long timestamp, int durationMs, boolean success, String service) {
            this.timestamp = timestamp;
            this.durationMs = durationMs;
            this.success = success;
            this.service = service;
        }
    }

    /** 服务基础信息（模拟 USE 框架的资源指标） */
    static class ServiceMetrics {
        String name;
        double cpuUtilization;      // 利用率 0~1
        double memorySaturation;   // 饱和度 0~1
        long activeConnections;    // 活跃连接数
        long maxConnections;       // 最大连接数
        long errorCount;

        ServiceMetrics(String name) {
            this.name = name;
            this.cpuUtilization = 0.3;
            this.memorySaturation = 0.4;
            this.activeConnections = 10;
            this.maxConnections = 100;
            this.errorCount = 0;
        }

        double connectionSaturation() {
            return (double) activeConnections / maxConnections;
        }
    }

    // ==================== 黄金信号采集器 ====================

    static class GoldenSignalsCollector {
        // 存储请求数据（模拟 Prometheus 时序数据）
        private final List<Request> requests = new ArrayList<>();
        private final Map<String, ServiceMetrics> services = new ConcurrentHashMap<>();

        // 当前时间窗口
        private long windowStart = System.currentTimeMillis();

        void recordRequest(String service, int durationMs, boolean success) {
            // 简单清理：只保留最近 5 分钟的数据
            requests.add(new Request(System.currentTimeMillis(), durationMs, success, service));
        }

        void updateServiceMetrics(String service, double cpu, double memSat, long activeConn) {
            services.computeIfAbsent(service, ServiceMetrics::new);
            ServiceMetrics m = services.get(service);
            m.cpuUtilization = cpu;
            m.memorySaturation = memSat;
            m.activeConnections = activeConn;
        }

        // ===== RED 框架指标 =====

        /** Rate：请求率 QPS */
        double requestRate(String service, long windowMs) {
            long now = System.currentTimeMillis();
            long start = now - windowMs;
            return requests.stream()
                .filter(r -> r.service.equals(service) && r.timestamp >= start)
                .count() * 1000.0 / windowMs;
        }

        /** Errors：错误率 */
        double errorRate(String service, long windowMs) {
            long now = System.currentTimeMillis();
            long start = now - windowMs;
            List<Request> window = requests.stream()
                .filter(r -> r.service.equals(service) && r.timestamp >= start)
                .collect(Collectors.toList());
            if (window.isEmpty()) return 0;
            long errors = window.stream().filter(r -> !r.success).count();
            return (double) errors / window.size();
        }

        /** Duration：P99 延迟 */
        int p99Latency(String service, long windowMs) {
            long now = System.currentTimeMillis();
            long start = now - windowMs;
            List<Integer> latencies = requests.stream()
                .filter(r -> r.service.equals(service) && r.timestamp >= start)
                .map(r -> r.durationMs)
                .sorted()
                .collect(Collectors.toList());
            if (latencies.isEmpty()) return 0;
            int index = (int) Math.ceil(latencies.size() * 0.99) - 1;
            return latencies.get(Math.max(0, index));
        }

        /** Duration：平均延迟 */
        double avgLatency(String service, long windowMs) {
            long now = System.currentTimeMillis();
            long start = now - windowMs;
            return requests.stream()
                .filter(r -> r.service.equals(service) && r.timestamp >= start)
                .mapToInt(r -> r.durationMs)
                .average()
                .orElse(0);
        }

        // ===== USE 框架指标 =====

        void printUSEFramework(String service) {
            ServiceMetrics m = services.get(service);
            if (m == null) {
                System.out.println("  [USE] 未找到服务: " + service);
                return;
            }

            System.out.println("  [USE] " + service + " 资源指标:");
            System.out.println("    利用率 (Utilization): CPU=" + String.format("%.1f%%", m.cpuUtilization * 100));
            System.out.println("    饱和度 (Saturation): 内存=" + String.format("%.1f%%", m.memorySaturation * 100)
                + ", 连接池=" + String.format("%.1f%%", connectionSaturation(m) * 100));
        }

        private double connectionSaturation(ServiceMetrics m) {
            return (double) m.activeConnections / m.maxConnections;
        }

        // ===== 黄金信号综合报表 =====

        void printGoldenSignals(String service, long windowMs) {
            System.out.println("\n========== 黄金信号报表 - " + service + " (窗口: " + windowMs + "ms) ==========");

            // RED 框架
            double rate = requestRate(service, windowMs);
            double error = errorRate(service, windowMs);
            int p99 = p99Latency(service, windowMs);
            double avg = avgLatency(service, windowMs);

            System.out.println("\n[RED 框架 - 面向请求]");
            System.out.println("  Rate (QPS):      " + String.format("%.2f", rate));
            System.out.println("  Errors (错误率): " + String.format("%.2f%%", error * 100));
            System.out.println("  Duration:");
            System.out.println("    平均: " + String.format("%.1fms", avg));
            System.out.println("    P99: " + p99 + "ms <-- 关键指标，用户体验的真实反映");

            // USE 框架
            printUSEFramework(service);

            // 业务含义解读
            System.out.println("\n[业务含义解读]");
            if (p99 > 500) {
                System.out.println("  ⚠ P99 超过 500ms，用户体验受损，SLO 告警阈值应触发");
            } else if (p99 > 200) {
                System.out.println("  ⚡ P99 在 200~500ms，延迟可接受但需关注趋势");
            } else {
                System.out.println("  ✓ P99 在 SLA 范围内");
            }

            ServiceMetrics m = services.get(service);
            if (m != null && m.cpuUtilization > 0.8) {
                System.out.println("  ⚠ CPU 利用率 > 80%，接近饱和，应考虑扩容");
            }
            if (m != null && connectionSaturation(m) > 0.85) {
                System.out.println("  ⚠ 连接池饱和度 > 85%，延迟可能来自排队而非计算");
            }
        }
    }

    // ==================== P99 计算演示 ====================

    static void demonstrateP99Calculation() {
        System.out.println("\n========== P99 计算演示 ==========");

        // 模拟 1000 次请求的延迟分布
        List<Integer> latencies = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < 1000; i++) {
            // 大部分请求在 50~150ms，少量在 150~500ms，极少量 > 500ms
            if (random.nextDouble() < 0.7) {
                latencies.add(random.nextInt(30, 150));
            } else if (random.nextDouble() < 0.95) {
                latencies.add(random.nextInt(150, 400));
            } else {
                latencies.add(random.nextInt(400, 800));
            }
        }

        Collections.sort(latencies);

        int p50 = latencies.get((int) (1000 * 0.50) - 1);
        int p90 = latencies.get((int) (1000 * 0.90) - 1);
        int p95 = latencies.get((int) (1000 * 0.95) - 1);
        int p99 = latencies.get((int) (1000 * 0.99) - 1);

        System.out.println("延迟分布（1000 次请求）:");
        System.out.println("  P50 (中位数): " + p50 + "ms");
        System.out.println("  P90:          " + p90 + "ms");
        System.out.println("  P95:          " + p95 + "ms");
        System.out.println("  P99:          " + p99 + "ms <-- SLA 通常基于此值");
        System.out.println("\n为什么不用平均值?");
        double avg = latencies.stream().mapToInt(Integer::intValue).average().orElse(0);
        System.out.println("  平均值: " + String.format("%.1f", avg) + "ms");
        System.out.println("  P99:   " + p99 + "ms");
        System.out.println("  解释: 平均值被大量快速请求拉低，P99 反映最差用户体验");
        System.out.println("        SLA 承诺的是'99%用户不受影响'，不是'平均响应快'");
    }

    // ==================== 主演示流程 ====================

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║  阶段 5 演示：可观测性 — 黄金信号与 RED/USE 框架               ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");

        GoldenSignalsCollector collector = new GoldenSignalsCollector();
        Random random = ThreadLocalRandom.current();

        // 模拟订单服务的运行
        System.out.println("\n[1] 模拟订单服务运行 30 秒...");

        for (int second = 0; second < 30; second++) {
            // 每秒 50~100 次请求
            int requestsThisSecond = random.nextInt(50, 101);
            for (int i = 0; i < requestsThisSecond; i++) {
                // 95% 成功，5% 失败
                boolean success = random.nextDouble() > 0.05;
                // 正常请求 30~200ms，慢请求 200~600ms
                int duration = random.nextDouble() > 0.1
                    ? random.nextInt(30, 200)
                    : random.nextInt(200, 600);
                collector.recordRequest("order-service", duration, success);
            }

            // 更新资源指标
            double cpu = 0.3 + random.nextDouble() * 0.4 + (second > 20 ? 0.2 : 0);
            long conn = 10 + random.nextInt(30) + (second > 20 ? 20 : 0);
            collector.updateServiceMetrics("order-service", cpu, 0.4 + random.nextDouble() * 0.2, conn);

            // 模拟延迟输出
            if (second % 10 == 0) {
                System.out.print("  t=" + second + "s ");
            }
        }

        // 输出黄金信号报表
        collector.printGoldenSignals("order-service", 30000);

        // P99 计算演示
        demonstrateP99Calculation();

        // 关键结论
        System.out.println("\n========== 演示结论 ==========");
        System.out.println("1. 黄金信号 = Latency + Traffic + Errors + Saturation");
        System.out.println("2. RED 框架用于无状态服务（HTTP/RPC）：Rate/Errors/Duration");
        System.out.println("3. USE 框架用于基础设施（DB/Redis）：Utilization/Saturation/Errors");
        System.out.println("4. P99 而非平均值 —— SLA 是用户体验承诺，不是平均统计");
        System.out.println("5. 饱和度（Saturation）是判断是否需要扩容的关键");
        System.out.println("   - 连接池先满时，CPU 可能看起来正常，但延迟在排队中成倍增长");
        System.out.println("\n对应理论稿章节：一、可观测三根、二、黄金信号与 RED/USE");
    }
}