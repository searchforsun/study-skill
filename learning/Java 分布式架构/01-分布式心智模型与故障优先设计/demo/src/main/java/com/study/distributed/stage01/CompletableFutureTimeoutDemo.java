package com.study.distributed.stage01;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 演示：CompletableFuture 的超时与「取消」在本地语义下意味着什么。
 * <p>
 * 分布式里对应的是：调用方超时 ≠ 服务端已停止执行；需要幂等、去重、对账与可观测来兜底。
 */
public final class CompletableFutureTimeoutDemo {

    private CompletableFutureTimeoutDemo() {
    }

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newCachedThreadPool();

        System.out.println("--- 场景 A：orTimeout 后 CompletableFuture 完成，但底层任务可能仍在跑 ---");
        CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> {
            sleep(2000);
            System.out.println("[slow] 后台仍打印：说明超时并不会自动「杀死」线程里的工作");
            return "done";
        }, pool);

        try {
            String r = slow.orTimeout(500, TimeUnit.MILLISECONDS).join();
            System.out.println("结果：" + r);
        } catch (Exception e) {
            System.out.println("调用方感知：" + rootCauseMessage(e));
        }
        // 给后台线程一点时间把 println 打完，便于观察
        sleep(2500);

        System.out.println("\n--- 场景 B：显式 cancel(true) 尝试中断线程（仍非分布式语义）---");
        CompletableFuture<String> interruptible = CompletableFuture.supplyAsync(() -> {
            for (int i = 0; i < 20; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("[interruptible] 收到中断，提前退出");
                    return "interrupted";
                }
                sleep(200);
            }
            return "finished";
        }, pool);
        sleep(300);
        boolean cancelled = interruptible.cancel(true);
        System.out.println("cancel(true) 返回：" + cancelled);
        sleep(500);

        System.out.println("\n--- 场景 C：链路过长 → 超时预算被逐级吞噬（示意）---");
        CompletableFuture<Integer> chain = CompletableFuture
                .supplyAsync(() -> delay(300, 1), pool)
                .thenApplyAsync(x -> delay(300, x + 1), pool)
                .thenApplyAsync(x -> delay(300, x + 1), pool);
        try {
            Integer v = chain.get(Duration.ofMillis(500).toMillis(), TimeUnit.MILLISECONDS);
            System.out.println("链结果：" + v);
        } catch (TimeoutException te) {
            System.out.println("整体在 500ms 内未完成（链路累积延迟 > 预算）");
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }

    private static int delay(int ms, int value) {
        sleep(ms);
        return value;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String rootCauseMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) {
            c = c.getCause();
        }
        return c.getClass().getSimpleName() + ": " + c.getMessage();
    }
}
