package com.study.distributed.stage06;

import java.util.*;

/**
 * 阶段 6 演示：云原生交付核心概念
 *
 * 包含三个模块：
 * 1. ImageLayerDemo - 镜像分层与构建优化
 * 2. JVMDemo - JVM 容器参数与 K8s 资源配合
 * 3. ADRDemo - ADR 决策记录与架构评审
 */
public class CloudNativeDeliveryDemo {

    public static void main(String[] args) {
        System.out.println("\n╔═══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  阶段 6 演示：云原生交付与架构师职能综合                               ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════╝");

        ImageLayerDemo.run();
        JVMDemo.run();
        ADRDemo.run();
    }
}

/**
 * 模块一：镜像分层与构建优化
 *
 * 模拟 Docker 镜像层缓存机制，展示：
 * - 缓存命中 vs 失效的影响
 * - 多阶段构建的价值
 * - 镜像大小优化
 */
class ImageLayerDemo {

    static class Layer {
        String instruction;
        boolean changed;
        long size; // bytes

        Layer(String instruction, boolean changed, long size) {
            this.instruction = instruction;
            this.changed = changed;
            this.size = size;
        }
    }

    static class DockerfileSimulation {
        List<Layer> layers = new ArrayList<>();
        long totalSize = 0;

        void addLayer(String instruction, long size) {
            boolean changed = layers.isEmpty() || Math.random() < 0.3; // 模拟 30% 变更概率
            Layer layer = new Layer(instruction, changed, size);
            layers.add(layer);
            if (!changed) {
                // 缓存命中，不增加实际大小
                totalSize += size;
            }
        }

        void printAnalysis() {
            System.out.println("\n=== Dockerfile 层分析 ===");
            long cachedSize = 0;
            long rebuildSize = 0;
            for (int i = 0; i < layers.size(); i++) {
                Layer layer = layers.get(i);
                String status = layer.changed ? "[重建]" : "[缓存]";
                System.out.printf("  层 %d: %-40s %s %.1f MB%n",
                    i + 1, layer.instruction, status, layer.size / 1024.0 / 1024.0);
                if (layer.changed) {
                    rebuildSize += layer.size;
                } else {
                    cachedSize += layer.size;
                }
            }
            System.out.printf("%n构建结果：缓存大小=%.1f MB，重建大小=%.1f MB，总大小=%.1f MB%n",
                (double) cachedSize / 1024 / 1024, (double) rebuildSize / 1024 / 1024, (double) totalSize / 1024 / 1024);
            double hitRate = totalSize > 0 ? (cachedSize * 100.0 / totalSize) : 0;
            System.out.printf("缓存命中率：%.0f%%%n", hitRate);
        }
    }

    public static void run() {
        System.out.println("\n【模块一】镜像分层与构建优化");
        System.out.println("─".repeat(70));

        // 单阶段构建 vs 多阶段构建对比
        System.out.println("\n场景：Java Maven 项目的镜像构建");

        System.out.println("\n--- 单阶段构建（完整 JDK + Maven）---");
        DockerfileSimulation singleStage = new DockerfileSimulation();
        singleStage.addLayer("FROM maven:3.9-eclipse-temurin-17", 800 * 1024 * 1024); // 800MB
        singleStage.addLayer("COPY pom.xml .", 10 * 1024);
        singleStage.addLayer("RUN mvn dependency:go-offline", 200 * 1024 * 1024); // 依赖缓存
        singleStage.addLayer("COPY src ./src", 5 * 1024 * 1024);
        singleStage.addLayer("RUN mvn package", 100 * 1024 * 1024);
        singleStage.printAnalysis();
        System.out.printf("  典型镜像大小：约 900 MB%n");

        System.out.println("\n--- 多阶段构建（Maven 构建 + JRE 运行）---");
        DockerfileSimulation multiStage = new DockerfileSimulation();
        multiStage.addLayer("FROM maven:3.9-eclipse-temurin-17 AS builder", 800 * 1024 * 1024);
        multiStage.addLayer("RUN mvn dependency:go-offline (builder)", 200 * 1024 * 1024);
        multiStage.addLayer("RUN mvn package (builder)", 100 * 1024 * 1024);
        multiStage.addLayer("FROM eclipse-temurin:17-jre (runtime)", 250 * 1024 * 1024); // 仅 JRE
        multiStage.addLayer("COPY --from=builder app.jar", 80 * 1024 * 1024);
        multiStage.printAnalysis();
        System.out.printf("  典型镜像大小：约 330 MB%n");

        // 构建缓存优化原理
        System.out.println("\n--- 构建缓存优化原则 ---");
        System.out.println("  原则 1：不变指令放前面（充分利用缓存）");
        System.out.println("    ✓ 依赖文件（pom.xml）先 COPY");
        System.out.println("    ✓ 源代码后 COPY（变更频繁）");
        System.out.println("  原则 2：合并 RUN 指令减少层数");
        System.out.println("    ✗ RUN mvn clean");
        System.out.println("    ✗ RUN mvn package");
        System.out.println("    ✓ RUN mvn clean package");
        System.out.println("  原则 3：.dockerignore 排除无关文件");
        System.out.println("    排除：target/, node_modules/, .git/, *.md");

        // 核心结论
        System.out.println("\n【核心结论】");
        System.out.println("  多阶段构建：构建镜像从 ~900MB 降至 ~330MB（减少 63%）");
        System.out.println("  缓存命中：仅重建变更层，未变更层复用");
        System.out.println("  镜像小于 500MB：拉取快、启动快、K8s 调度效率高");
    }
}

/**
 * 模块二：JVM 容器参数与 K8s 资源限制配合
 *
 * 展示 Java 应用在容器化后的常见问题：
 * - JVM 不感知容器 limits 导致的问题
 * - 正确的 JVM 参数配置
 * - OOM Kill 场景模拟
 */
class JVMDemo {

    static class JVMConfig {
        long containerMemoryLimit; // bytes
        long heapSize;
        long offHeapSize;
        boolean useContainerSupport;
        String gcType;

        void printConfig(String label) {
            long containerMB = containerMemoryLimit / 1024 / 1024;
            long heapMB = heapSize / 1024 / 1024;
            long offHeapMB = offHeapSize / 1024 / 1024;
            System.out.printf("  [%s] 容器限制=%d MB, Heap=%d MB, Off-Heap=%d MB%n",
                label, containerMB, heapMB, offHeapMB);
            System.out.printf("           UseContainerSupport=%s, GC=%s%n",
                useContainerSupport, gcType);
            long total = heapSize + offHeapSize;
            double ratio = (double) total / containerMemoryLimit * 100;
            boolean safe = ratio <= 80; // 建议不超过 80%
            long totalMB = total / 1024 / 1024;
            System.out.printf("           总内存使用=%d MB (占容器 %.0f%%) %s%n",
                totalMB, ratio, safe ? "✓" : "⚠ 超限风险");
        }
    }

    public static void run() {
        System.out.println("\n【模块二】JVM 容器参数与 K8s 资源限制配合");
        System.out.println("─".repeat(70));

        long containerLimit = 512 * 1024 * 1024; // 512 MB

        System.out.println("\n--- 场景：K8s Pod 配置 memory: 512Mi ---");

        // 错误配置：不感知容器限制
        JVMConfig wrongConfig = new JVMConfig();
        wrongConfig.containerMemoryLimit = containerLimit;
        wrongConfig.heapSize = 400 * 1024 * 1024; // JVM 默认可能更大
        wrongConfig.offHeapSize = 150 * 1024 * 1024; // Metaspace + DirectBuffer
        wrongConfig.useContainerSupport = false;
        wrongConfig.gcType = "G1";
        System.out.println("\n错误配置（JDK 8 默认，不感知容器）:");
        wrongConfig.printConfig("JDK8");
        System.out.println("  问题：JVM 以为有更大内存，off-heap 可能超出限制 → OOMKilled");

        // 正确配置：容器感知 + 合理 heap
        JVMConfig correctConfig = new JVMConfig();
        correctConfig.containerMemoryLimit = containerLimit;
        correctConfig.heapSize = (long) (512 * 0.7 * 1024 * 1024); // 70% 用于 heap
        correctConfig.offHeapSize = (long) (512 * 0.15 * 1024 * 1024); // 15% 用于 off-heap
        correctConfig.useContainerSupport = true;
        correctConfig.gcType = "G1";
        System.out.println("\n正确配置（JDK 17+ UseContainerSupport）:");
        correctConfig.printConfig("JDK17");

        // 推荐 JVM 参数
        System.out.println("\n--- 推荐 JVM 参数（K8s 容器内）---");
        System.out.println("  -Xms512m -Xmx512m          # 显式堆大小（与 limits 对应）");
        System.out.println("  -XX:+UseG1GC              # G1 GC，默认选择");
        System.out.println("  -XX:MaxGCPauseMillis=200   # P99 延迟目标");
        System.out.println("  -XX:+UseContainerSupport  # JDK 10+ 自动感知容器 limits");
        System.out.println("  -XX:MaxRAMPercentage=70   # 堆使用容器内存的 70%");

        // GC 选型
        System.out.println("\n--- GC 选型决策 ---");
        System.out.println("  G1（默认）：P99 < 200ms，参数简单，适合多数场景");
        System.out.println("  ZGC（超大堆 >100GB）：P99 < 10ms，需要 JDK 11+");
        System.out.println("  Shenandoah：与 G1 类似但停顿更短");
        System.out.println("  Parallel：批处理，追求吞吐，允许较长停顿");

        // OOM Kill 排查
        System.out.println("\n--- OOMKilled 排查命令 ---");
        System.out.println("  kubectl describe pod <pod> | grep -A5 'Last State'");
        System.out.println("  kubectl exec <pod> -- cat /sys/fs/cgroup/memory/memory.limit_in_bytes");
        System.out.println("  kubectl exec <pod> -- java -XX:+PrintFlagsFinal | grep MaxHeapSize");

        System.out.println("\n【核心结论】");
        System.out.println("  JDK 10+ 使用 -XX:+UseContainerSupport 让 JVM 自动感知容器限制");
        System.out.println("  Heap 不超过容器 limits 的 70%，留足 off-heap 空间");
        System.out.println("  生产环境推荐 G1GC，极低延迟场景选 ZGC");
    }
}

/**
 * 模块三：ADR 决策记录与架构评审
 *
 * 展示：
 * - ADR 标准结构
 * - 架构评审清单
 * - 决策的 Trade-off 分析
 */
class ADRDemo {

    static class ADR {
        String id;
        String title;
        String status;
        String context;
        String decision;
        List<String> alternatives = new ArrayList<>();
        List<String> consequences = new ArrayList<>();
        String rollbackCondition;

        void print() {
            System.out.printf("%n  【%s】%s%n  状态: %s%n", id, title, status);
            System.out.printf("  背景: %s%n", context);
            System.out.printf("  决策: %s%n", decision);
            System.out.printf("  备选方案:%n");
            alternatives.forEach(a -> System.out.printf("    - %s%n", a));
            System.out.printf("  后果:%n");
            consequences.forEach(c -> System.out.printf("    + %s%n", c));
            System.out.printf("  回滚条件: %s%n", rollbackCondition);
        }
    }

    static class ReviewItem {
        String dimension;
        String checkPoint;
        boolean passed;
        String note;

        ReviewItem(String dimension, String checkPoint, boolean passed, String note) {
            this.dimension = dimension;
            this.checkPoint = checkPoint;
            this.passed = passed;
            this.note = note;
        }
    }

    public static void run() {
        System.out.println("\n【模块三】ADR 决策记录与架构评审");
        System.out.println("─".repeat(70));

        // ADR 示例：GC 选型决策
        System.out.println("\n--- ADR 示例：订单服务 GC 收集器选型 ---");

        ADR adr = new ADR();
        adr.id = "ADR-001";
        adr.title = "订单服务采用 G1 GC 作为默认收集器";
        adr.status = "Accepted";
        adr.context = "订单服务 P99 延迟在高并发下超过 500ms，GC 停顿是主要贡献者。堆大小 4GB，平均停顿时间 300ms，P99 最坏达到 1200ms。";
        adr.decision = "采用 G1 GC 收集器，设置 -XX:MaxGCPauseMillis=200，-XX:+UseContainerSupport";
        adr.alternatives.add("ZGC：延迟更低（JDK 11+），但升级成本高，G1 在当前负载下足够");
        adr.alternatives.add("Parallel GC：吞吐更高，但停顿时间不可控，不满足 P99 < 500ms 要求");
        adr.consequences.add("P99 延迟从 800ms 降至 280ms（实测）");
        adr.consequences.add("GC 吞吐量略有下降（< 5%），可接受");
        adr.consequences.add("参数调优需要持续观测，不是一次性配置");
        adr.rollbackCondition = "如果 P99 延迟无法降到 500ms 以内，或 GC 吞吐量下降超过 15%，回滚至 Parallel GC 并重新评估";
        adr.print();

        // 架构评审清单
        System.out.println("\n--- 架构评审清单（上线前必查）---");

        ReviewItem[] items = {
            new ReviewItem("故障模型", "最坏情况影响面？是否有熔断/降级？", true, "已配置 Sentinel 降级规则"),
            new ReviewItem("数据一致性", "跨服务一致性如何保证？事务边界与领域边界对齐？", true, "本地消息表 + 定时对账"),
            new ReviewItem("可观测", "关键路径 trace 埋点？告警阈值与 SLO 对齐？", true, "SkyWalking 已接入，告警与错误预算挂钩"),
            new ReviewItem("安全", "密钥上 KMS？RBAC 最小权限？镜像 CVE 扫描？", false, "⚠ DB 密钥仍在 ConfigMap，需要迁移到 KMS"),
            new ReviewItem("容量", "压测数据？扩容策略？依赖单点？", true, "单服务 2000 QPS，K8s HPA 配置完成"),
            new ReviewItem("交付", "回滚方案？金丝雀策略？停机窗口？", true, "滚动更新 maxUnavailable=0，回滚时间 < 5 分钟"),
        };

        System.out.printf("%n  %-12s | %-40s | %s | %s%n",
            "维度", "检查点", "通过", "备注");
        System.out.println("  " + "-".repeat(12) + "+" + "-".repeat(42) + "+" + "-".repeat(4) + "+" + "-".repeat(20));
        for (ReviewItem item : items) {
            System.out.printf("  %-12s | %-40s | %s | %s%n",
                item.dimension,
                item.checkPoint.length() > 40 ? item.checkPoint.substring(0, 37) + "..." : item.checkPoint,
                item.passed ? "✓" : "⚠",
                item.note.length() > 18 ? item.note.substring(0, 15) + "..." : item.note);
        }

        System.out.println("\n  ⚠ 阻塞项：DB 密钥需在发布前迁移到 KMS");
        System.out.println("  建议：架构师在评审中推动解决，确认签字责任人");

        // 架构师软技能
        System.out.println("\n--- 架构师软技能：沟通·谈判·推动 ---");
        System.out.println("  沟通：把技术决策翻译为业务语言（如「P99 500ms 对应转化率损失 2%」）");
        System.out.println("  谈判：在时间/人力/成本约束下找到最优方案（不是技术最优解）");
        System.out.println("  推动：跨团队协调落地，克服组织惯性（评审结论需要各方签字确认）");
        System.out.println("  关键：评审是讨论不是宣布，结论应有 Trade-off 的共识基础");

        System.out.println("\n【核心结论】");
        System.out.println("  ADR 是决策日志，不是设计文档——记录「为什么这么选」而非「做什么」");
        System.out.println("  架构评审是最后一道门——每个维度都应有明确的通过/阻塞状态");
        System.out.println("  密钥必须上 KMS——ConfigMap 明文是生产环境高危风险");
    }
}