package com.study.distributed.stage04;

import java.util.BitSet;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 缓存三大问题（穿透/击穿/雪崩）的防御模式演示。
 *
 * <p>三个子演示可独立运行，分别对应 THEORY.md 一节的三种故障模式与对策。
 */
public final class CacheDefenseDemo {

    /* ==================== 共享组件：简易布隆过滤器 ==================== */
    static class SimpleBloomFilter {
        private final BitSet bits;
        private final int size;
        private final int hashCount;

        SimpleBloomFilter(int expectedElements, double falsePositiveRate) {
            // size = -n * ln(p) / (ln2)^2 （简化取整）
            this.size = (int) (-expectedElements * Math.log(falsePositiveRate) / (Math.log(2) * Math.log(2)));
            this.hashCount = (int) (this.size / (double) expectedElements * Math.log(2));
            this.bits = new BitSet(this.size);
        }

        void add(String key) {
            for (int i = 0; i < hashCount; i++) {
                bits.set(hash(key, i) % size, true);
            }
        }

        boolean mightContain(String key) {
            for (int i = 0; i < hashCount; i++) {
                if (!bits.get(hash(key, i) % size)) {
                    return false;  // 绝对不存在
                }
            }
            return true;  // 可能存在（有假阳性概率）
        }

        private int hash(String key, int seed) {
            return Math.abs((key.hashCode() ^ (seed * 0x9e3779b9)) % Integer.MAX_VALUE);
        }
    }

    /* ==================== 共享组件：简易本地"缓存" ==================== */
    static class SimpleCache {
        private final Map<String, CacheEntry<?>> store = new ConcurrentHashMap<>();

        static class CacheEntry<T> {
            final T value;
            final long expireAt;
            CacheEntry(T value, long ttlMs) {
                this.value = value;
                this.expireAt = System.currentTimeMillis() + ttlMs;
            }
            boolean isExpired() { return System.currentTimeMillis() > expireAt; }
        }

        @SuppressWarnings("unchecked")
        <T> T get(String key) {
            CacheEntry<?> entry = store.get(key);
            if (entry == null) return null;
            if (entry.isExpired()) {
                store.remove(key);
                return null;
            }
            return (T) entry.value;
        }

        void put(String key, Object value, long ttlMs) {
            store.put(key, new CacheEntry<>(value, ttlMs));
        }

        void remove(String key) {
            store.remove(key);
        }
    }

    /* ==================== 共享组件：模拟数据库 ==================== */
    static final Map<String, String> DB = new ConcurrentHashMap<>();
    static {
        DB.put("user:1001", "张三");
        DB.put("user:1002", "李四");
        DB.put("product:5001", "iPhone 17");
    }

    // ====================================================================
    // 演示 1：缓存穿透 — 布隆过滤器 + 空值缓存双层防御
    // ====================================================================
    static void demoPenetration() {
        System.out.println("══════════ 演示 1：缓存穿透 — 布隆过滤器 + 空值缓存 ══════════");

        SimpleBloomFilter bloom = new SimpleBloomFilter(10000, 0.01);
        SimpleCache cache = new SimpleCache();
        AtomicInteger dbHits = new AtomicInteger(0);

        // 初始化：把数据库中存在的 key 加入布隆
        DB.keySet().forEach(bloom::add);

        // 测试：5 个 key 中 2 个存在、3 个不存在
        String[] testKeys = {"user:1001", "user:9999", "product:5001", "ghost:0001", "user:1002"};
        for (String key : testKeys) {
            // 第一层：布隆过滤器
            if (!bloom.mightContain(key)) {
                System.out.printf("  [布隆拦截] key=%s → 绝对不存在，直接拒绝（不打 DB）%n", key);
                cache.put(key, "NULL_MARKER", 60_000);  // 空值缓存 60s
                continue;
            }

            // 第二层：查缓存
            String cached = cache.get(key);
            if (cached != null) {
                System.out.printf("  [缓存命中] key=%s → value=%s%n", key, cached);
                continue;
            }

            // 第三层：查数据库
            String dbValue = DB.get(key);
            if (dbValue != null) {
                cache.put(key, dbValue, 300_000);
                System.out.printf("  [查库成功] key=%s → value=%s（已写入缓存）%n", key, dbValue);
            } else {
                // 不存在则写空值标记
                cache.put(key, "NULL_MARKER", 60_000);
                System.out.printf("  [查库为空] key=%s → 写入空值缓存 60s，后续穿透被拦截%n", key);
            }
            dbHits.incrementAndGet();
        }
        System.out.printf("  本次查库次数：%d / %d 个 key（无布隆+空值缓存时为 5/5）%n",
            dbHits.get(), testKeys.length);
        System.out.println();
    }

    // ====================================================================
    // 演示 2：缓存击穿 — 分布式互斥锁（简易版）防止热点重建风暴
    // ====================================================================
    static void demoBreakdown() throws InterruptedException {
        System.out.println("══════════ 演示 2：缓存击穿 — 互斥锁防热点重建风暴 ══════════");

        SimpleCache cache = new SimpleCache();
        // 模拟一个即将过期的热点 key
        cache.put("hot:event:2026", "缓存旧值", 100); // TTL 极短，立即过期
        AtomicInteger dbHits = new AtomicInteger(0);
        Object lock = new Object();  // 简化：单 JVM 内的锁模拟分布式锁

        Runnable queryTask = () -> {
            String key = "hot:event:2026";
            String value = cache.get(key);
            if (value != null) {
                System.out.printf("  [线程%s] 缓存命中 → %s%n",
                    Thread.currentThread().getName().replace("Thread-", ""), value);
                return;
            }

            // 缓存未命中 → 尝试获取"分布式锁"重建
            synchronized (lock) {
                // 双重检查：锁内再次查缓存（前一个拿锁的线程可能已重建完毕）
                value = cache.get(key);
                if (value != null) {
                    System.out.printf("  [线程%s] 双重检查命中（前一个线程已重建）→ %s%n",
                        Thread.currentThread().getName().replace("Thread-", ""), value);
                    return;
                }

                // 真正重建
                String dbValue = DB.getOrDefault(key, "热点事件数据");
                cache.put(key, dbValue, 300_000);
                dbHits.incrementAndGet();
                System.out.printf("  [线程%s] 拿到锁，查库重建 → %s（查库次数累计=%d）%n",
                    Thread.currentThread().getName().replace("Thread-", ""), dbValue, dbHits.get());
            }
        };

        // 模拟 10 个并发线程同时发现缓存过期
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(queryTask, "T" + i);
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.printf("  10 个并发线程，查库次数：%d（无锁时为 10 次）%n", dbHits.get());
        System.out.println();
    }

    // ====================================================================
    // 演示 3：缓存雪崩 — 随机 TTL 离散化过期时间
    // ====================================================================
    static void demoAvalanche() {
        System.out.println("══════════ 演示 3：缓存雪崩 — 随机 TTL 离散化过期时间 ══════════");

        Random rng = ThreadLocalRandom.current();
        int baseTTL = 3600;  // 基础 TTL：1 小时
        double jitterRatio = 0.20;  // ±20% 随机偏移

        System.out.println("  若在整点批量预热 10 个 key，TTL 完全相同 → 全部同时过期");
        System.out.println("  加入随机偏移后，过期时间被离散化：");
        System.out.println();

        for (int i = 1; i <= 10; i++) {
            // 在 baseTTL * (1 - jitterRatio) 到 baseTTL * (1 + jitterRatio) 之间随机
            double jitter = (rng.nextDouble() * 2 - 1) * jitterRatio;
            long actualTTL = (long) (baseTTL * (1 + jitter));
            long expireInMinutes = actualTTL / 60;

            System.out.printf("  key=hot:page:%02d  baseTTL=%ds  jitter=%+.1f%%  actualTTL=%ds  过期≈%d分钟后%n",
                i, baseTTL, jitter * 100, actualTTL, expireInMinutes);
        }

        // 统计离散度
        System.out.println();
        System.out.println("  效果：最大过期间隔 = baseTTL × jitterRatio × 2 ≈ "
            + (long)(baseTTL * jitterRatio * 2) + "s —— 拥堵窗口从 0 秒拉长到分散的滑动窗口。");
        System.out.println();
    }

    // ====================================================================
    // 演示 4：Cache-aside — 先库后删 vs 先删后库
    // ====================================================================
    static void demoCacheAside() {
        System.out.println("══════ 演示 4：Cache-aside — 先更新库再删缓存 vs 先删缓存再更新库 ══════");

        SimpleCache cache = new SimpleCache();
        Map<String, String> db = new ConcurrentHashMap<>();
        db.put("stock:prod1", "100");
        cache.put("stock:prod1", "100", 600_000);

        // 正确做法：先更新库，再删除缓存
        System.out.println("  正确顺序（先库后删）：");
        db.put("stock:prod1", "99");         // 1. 更新数据库
        cache.remove("stock:prod1");         // 2. 删除缓存
        System.out.println("    库: stock=99, 缓存: 已删除 → 下次读为 null → 查库得 99 → 不一致窗口极小");
        System.out.println();

        // 错误做法的风险
        System.out.println("  错误顺序（先删后库）：");
        cache.remove("stock:prod1");         // 1. 删缓存（在这一步之后，另一个线程读缓存为 null，查库得旧值，把旧值写回缓存）
        db.put("stock:prod1", "97");         // 2. 更新数据库
        System.out.println("    风险：在步骤 1 和 2 之间，并发读会把旧值写回缓存 → 缓存中存的是旧数据，直到下次失效");
        System.out.println("    「先库后删」的并发窗口（读旧值 → 写 → 删 → 读写缓存）远小于「先删后库」。");
        System.out.println();
    }

    // ====================================================================

    public static void main(String[] args) throws InterruptedException {
        System.out.println();
        System.out.println("==================== 缓存防御模式演示 ====================");
        System.out.println("（纯 Java 模拟，不需 Redis/DB；印证 THEORY.md 第一节逻辑）");
        System.out.println();

        demoPenetration();
        demoBreakdown();
        demoAvalanche();
        demoCacheAside();

        System.out.println("==================== 演示结束 ====================");
    }
}
