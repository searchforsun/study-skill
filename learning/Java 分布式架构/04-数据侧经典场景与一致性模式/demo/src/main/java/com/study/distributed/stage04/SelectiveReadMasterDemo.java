package com.study.distributed.stage04;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 读写分离 + 读一致性三级 + 选择性读主 演示。
 *
 * <p>模拟主库与从库之间存在延迟窗口，验证选择性读主方案的有效性。
 * 与 THEORY.md 第二节「读一致性的三级业务语义」对应。
 */
public final class SelectiveReadMasterDemo {

    // 模拟"主库"与"从库"——从库有延迟（滞后写入）
    static class SimulatedDB {
        private final Map<String, String> master = new ConcurrentHashMap<>();
        private final Map<String, String> replica = new ConcurrentHashMap<>();
        private long replicationLagMs;

        SimulatedDB(long replicationLagMs) {
            this.replicationLagMs = replicationLagMs;
        }

        /** 写主库 */
        void write(String key, String value) {
            master.put(key, value);
            // 模拟异步复制延迟：启动一个线程延迟后同步到从库
            long lag = replicationLagMs + ThreadLocalRandom.current().nextLong(-20, 50);
            new Thread(() -> {
                try {
                    Thread.sleep(lag);
                    replica.put(key, value);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        /** 读主库（强一致） */
        String readMaster(String key) { return master.get(key); }

        /** 读从库（最终一致，可能有延迟） */
        String readReplica(String key) { return replica.get(key); }

        /** 从库延迟毫秒数 */
        long lag() { return replicationLagMs; }
    }

    // 记录"最近被写过的 key"——TTL 约等于 max_lag + buffer
    static class RecentlyWritten {
        private final Map<String, Instant> keys = new ConcurrentHashMap<>();
        private final Duration ttl;

        RecentlyWritten(Duration ttl) { this.ttl = ttl; }

        void mark(String key) { keys.put(key, Instant.now().plus(ttl)); }

        boolean isRecent(String key) {
            Instant expire = keys.get(key);
            if (expire == null) return false;
            if (Instant.now().isAfter(expire)) {
                keys.remove(key);
                return false;
            }
            return true;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println();
        System.out.println("==================== 读写分离与读一致性演示 ====================");
        System.out.println("（模拟主从延迟 + 选择性读主；印证 THEORY.md 第二节）");
        System.out.println();

        long maxLagMs = 200;  // 最大延迟 200ms
        SimulatedDB db = new SimulatedDB(maxLagMs);
        // TTL = maxLag(99分位) + buffer(100ms) = 300ms
        RecentlyWritten recentWrites = new RecentlyWritten(Duration.ofMillis(300));

        // 场景：用户修改了自己的昵称
        String key = "user:nickname:1001";
        db.master.put(key, "旧昵称");
        db.replica.put(key, "旧昵称");

        System.out.printf("  主从延迟：≤ %dms（P99）%n", maxLagMs);
        System.out.println();

        // 1. 写入
        System.out.println("  [写] 用户修改昵称 → master 已更新");
        db.write(key, "新昵称");
        recentWrites.mark(key);

        // 2. 刚写完后立即读 —— 发现 key 在 recentlyWritten 中 → 强制读主
        Thread.sleep(50);  // 假设写后 50ms 读
        String readApproach;
        String readValue;
        if (recentWrites.isRecent(key)) {
            readValue = db.readMaster(key);
            readApproach = "读主（selective-read-master：检测到近期写入）";
        } else {
            readValue = db.readReplica(key);
            readApproach = "读从";
        }
        System.out.printf("  [读 ① 写后+50ms] %s → value=%s %n", readApproach, readValue);

        // 3. 等稍久一些再读 —— TTL 未过期，仍然读主
        Thread.sleep(100);
        if (recentWrites.isRecent(key)) {
            readValue = db.readMaster(key);
            readApproach = "读主（selective-read-master 标记仍在有效期内）";
        } else {
            readValue = db.readReplica(key);
            readApproach = "读从";
        }
        System.out.printf("  [读 ② 写后+150ms] %s → value=%s %n", readApproach, readValue);

        // 4. TTL 过期后读 —— 回到读从
        Thread.sleep(200);  // 总延迟已超过 TTL 300ms
        if (recentWrites.isRecent(key)) {
            readValue = db.readMaster(key);
            readApproach = "读主";
        } else {
            readValue = db.readReplica(key);
            readApproach = "读从（TTL 过期，恢复读从）";
        }
        System.out.printf("  [读 ③ 写后+350ms] %s → value=%s %n", readApproach, readValue);
        System.out.println();

        // 5. 对比：如果一直读从会发生什么
        System.out.println("  对比：若没用 selective-read-master，前两次读从的结果：");
        System.out.println("    读① → 可能还是「旧昵称」（从库尚未复制）");
        System.out.println("    读② → 可能还是「旧昵称」或刚到「新昵称」（不可预测）");
        System.out.println("    读③ → 大概率已复制完成，但不确定");
        System.out.println();
        System.out.println("  选择性读主的代价：每次读多一次 Redis 查标记（~1ms）");
        System.out.println("  收益：刚写的用户始终看到最新值，其他用户接受从库的短暂延迟。");
        System.out.println();

        System.out.println("==================== 演示结束 ====================");
    }
}
