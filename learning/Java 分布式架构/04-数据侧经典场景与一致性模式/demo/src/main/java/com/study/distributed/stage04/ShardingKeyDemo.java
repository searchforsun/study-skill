package com.study.distributed.stage04;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 分库分表决策清单演示：对比不同分键选择导致的数据倾斜与查询路由差异。
 *
 * <p>不模拟真实分片中间件，而是用 Java 逻辑展示"分键选错会怎样"。
 * 对应 THEORY.md 第五节「分键是分库分表的第一性决策」。
 */
public final class ShardingKeyDemo {

    // 模拟 4 个分片
    static class ShardingSimulator {
        private final int shardCount;
        private final Map<Integer, Map<String, String>> shards = new ConcurrentHashMap<>();

        ShardingSimulator(int shardCount) {
            this.shardCount = shardCount;
            for (int i = 0; i < shardCount; i++) {
                shards.put(i, new ConcurrentHashMap<>());
            }
        }

        /** 按分键决定数据落在哪个分片 */
        void write(String shardingKey, String key, String value) {
            int shard = Math.abs(shardingKey.hashCode()) % shardCount;
            shards.get(shard).put(key, value);
        }

        /** 需要分键才能路由 */
        int resolveShard(String shardingKey) {
            return Math.abs(shardingKey.hashCode()) % shardCount;
        }

        /** 无分键 → 需要全分片扫描 */
        String readWithoutKey(String key) {
            for (Map.Entry<Integer, Map<String, String>> entry : shards.entrySet()) {
                if (entry.getValue().containsKey(key)) {
                    return entry.getValue().get(key) + " (来自分片" + entry.getKey() + ")";
                }
            }
            return null;
        }

        Map<Integer, Integer> distribution() {
            Map<Integer, Integer> dist = new ConcurrentHashMap<>();
            for (Map.Entry<Integer, Map<String, String>> entry : shards.entrySet()) {
                dist.put(entry.getKey(), entry.getValue().size());
            }
            return dist;
        }
    }

    public static void main(String[] args) {
        System.out.println();
        System.out.println("============ 分库分表 — 分键选择与数据倾斜演示 ============");
        System.out.println("（印证 THEORY.md 第五节「分键是分库分表的第一性决策」）");
        System.out.println();

        // ─── 演示 1：好的分键（高基数，均匀分布）───
        System.out.println("══════ 演示 1：好的分键（orderId，高基数） ══════");
        ShardingSimulator goodSharding = new ShardingSimulator(4);
        for (int i = 1; i <= 1000; i++) {
            String orderId = "ORD-" + String.format("%06d", i);
            goodSharding.write(orderId, orderId, "订单数据" + i);
        }
        Map<Integer, Integer> goodDist = goodSharding.distribution();
        System.out.print("  各分片数据量：");
        goodDist.forEach((shard, count) ->
            System.out.printf("分片%d=%d条(%.1f%%)  ", shard, count, count / 10.0));
        System.out.println();
        System.out.println("  ✓ 分布均匀，热点风险低。绝大多数查询携带 orderId → 精确定位分片。");
        System.out.println();

        // ─── 演示 2：坏的分键（状态值，低基数，严重倾斜）───
        System.out.println("══════ 演示 2：坏的分键（status，低基数） ══════");
        ShardingSimulator badSharding = new ShardingSimulator(4);
        String[] statuses = {"CREATED", "PAID", "SHIPPED", "COMPLETED", "CANCELLED"};
        for (int i = 1; i <= 1000; i++) {
            String orderId = "ORD-" + String.format("%06d", i);
            // 模拟：90% 的订单在 CREATED/PAID 状态
            String status;
            double r = ThreadLocalRandom.current().nextDouble();
            if (r < 0.70) status = "CREATED";        // 70%
            else if (r < 0.90) status = "PAID";       // 20%
            else status = statuses[2 + ThreadLocalRandom.current().nextInt(3)]; // 10%
            badSharding.write(status, orderId, "订单数据" + i);
        }
        Map<Integer, Integer> badDist = badSharding.distribution();
        System.out.print("  各分片数据量：");
        badDist.forEach((shard, count) ->
            System.out.printf("分片%d=%d条(%.1f%%)  ", shard, count, count / 10.0));
        System.out.println();
        System.out.println("  ✗ 数据严重倾斜！CREATED 状态都在一个分片，热点打满单分片资源。");
        System.out.println("  查询「某状态的订单」无法利用分键定位——但若按 orderId 查，根本没带 status。");
        System.out.println();

        // ─── 演示 3：全分片扫描的代价 ───
        System.out.println("══════ 演示 3：无分键查询 → 全分片扫描 ══════");
        String targetKey = "ORD-000500";
        // 知道 key 但不知道分键 → 只能扫
        String result = goodSharding.readWithoutKey(targetKey);
        System.out.printf("  查询 key=%s（无分键）→ %s%n", targetKey, result);
        System.out.println("  代价：4 次 DB 查询 + 应用层聚合，而带分键只需 1 次。");
        System.out.println();

        // ─── 决策清单输出 ───
        System.out.println("══════ 分库分表决策前置四问 ══════");
        System.out.println("  □ 1. 垂直拆分做了吗？（不同业务域的表分到不同库）");
        System.out.println("  □ 2. 冷热分离做了吗？（历史数据归档到离线存储）");
        System.out.println("  □ 3. 读写分离做了吗？（读压力用从库扛）");
        System.out.println("  □ 4. 单表数据量真的撑不住了吗？（MySQL 单表 5000 万行内 + 合理索引通常够）");
        System.out.println("  → 四项都确认后才启动分库分表，否则你在用最贵的方案解决便宜的问题。");
        System.out.println();

        System.out.println("==================== 演示结束 ====================");
    }
}
