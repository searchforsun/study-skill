package com.study.distributed.stage03;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 舱壁模式演示：线程池隔离
 *
 * 知识点：
 * 1. 线程池隔离：不同服务使用独立线程池
 * 2. 资源耗尽时隔离保护
 * 3. 舱壁 vs 熔断的配合
 */
public class BulkheadDemo {

    private static final int SHARED_POOL_SIZE = 10;
    private static final int PRODUCT_POOL_SIZE = 5;
    private static final int ORDER_POOL_SIZE = 3;

    private static final AtomicInteger sharedPoolActive = new AtomicInteger(0);
    private static final AtomicInteger productPoolActive = new AtomicInteger(0);
    private static final AtomicInteger orderPoolActive = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 舱壁模式演示 ===\n");

        System.out.println("场景：商品服务和订单服务共享线程池 vs 独立线程池\n");

        // 场景1：共享线程池（一个服务耗尽影响另一个）
        System.out.println("【场景1】共享线程池（无隔离）");
        testSharedPool();

        // 场景2：独立线程池（舱壁隔离）
        System.out.println("\n【场景2】独立线程池（舱壁隔离）");
        testIsolatedPools();

        System.out.println("\n=== 核心结论 ===");
        System.out.println("1. 共享线程池：一个服务慢导致另一个服务也慢");
        System.out.println("2. 独立线程池：商品服务慢不影响订单服务");
        System.out.println("3. 舱壁防止资源耗尽扩散，熔断防止故障扩散");
    }

    private static void testSharedPool() throws InterruptedException {
        ExecutorService sharedPool = Executors.newFixedThreadPool(SHARED_POOL_SIZE);

        System.out.println("  并发请求：商品查询 8 个 + 订单查询 8 个（共享 " + SHARED_POOL_SIZE + " 线程）");
        System.out.println("  预期：线程池饱和，请求排队，响应时间变长\n");

        long start = System.currentTimeMillis();

        // 商品查询（模拟慢查询）
        for (int i = 0; i < 8; i++) {
            sharedPool.submit(() -> {
                sharedPoolActive.incrementAndGet();
                simulateSlowQuery("商品查询", 500);
                sharedPoolActive.decrementAndGet();
            });
        }

        // 订单查询（快速查询）
        for (int i = 0; i < 8; i++) {
            sharedPool.submit(() -> {
                sharedPoolActive.incrementAndGet();
                simulateFastQuery("订单查询", 50);
                sharedPoolActive.decrementAndGet();
            });
        }

        sharedPool.shutdown();
        sharedPool.awaitTermination(3, TimeUnit.SECONDS);

        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("  总耗时：%dms（订单查询被商品查询拖累）%n", elapsed);
    }

    private static void testIsolatedPools() throws InterruptedException {
        ExecutorService productPool = Executors.newFixedThreadPool(PRODUCT_POOL_SIZE);
        ExecutorService orderPool = Executors.newFixedThreadPool(ORDER_POOL_SIZE);

        System.out.println("  并发请求：商品查询 8 个（独立 " + PRODUCT_POOL_SIZE + " 线程） + 订单查询 8 个（独立 " + ORDER_POOL_SIZE + " 线程）");
        System.out.println("  预期：商品服务排队，订单服务不受影响\n");

        long start = System.currentTimeMillis();

        // 商品查询（模拟慢查询）
        for (int i = 0; i < 8; i++) {
            productPool.submit(() -> {
                productPoolActive.incrementAndGet();
                simulateSlowQuery("商品查询", 500);
                productPoolActive.decrementAndGet();
            });
        }

        // 订单查询（快速查询）
        for (int i = 0; i < 8; i++) {
            orderPool.submit(() -> {
                orderPoolActive.incrementAndGet();
                simulateFastQuery("订单查询", 50);
                orderPoolActive.decrementAndGet();
            });
        }

        productPool.shutdown();
        orderPool.shutdown();
        productPool.awaitTermination(3, TimeUnit.SECONDS);
        orderPool.awaitTermination(3, TimeUnit.SECONDS);

        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("  总耗时：%dms（订单查询未被商品查询影响）%n", elapsed);
    }

    private static void simulateSlowQuery(String name, int delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void simulateFastQuery(String name, int delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}