package com.study.distributed.stage01;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 演示：同一 JVM 内多线程对共享计数器的竞态。
 * <p>
 * 要点：synchronized / AtomicLong 能修复<strong>进程内</strong>可见性与原子性；
 * 但它不会自动外溢成跨服务的互斥——那是另一条语义链（幂等、队列、协调服务等）。
 */
public final class RaceConditionDemo {

    private RaceConditionDemo() {
    }

    public static void main(String[] args) throws InterruptedException {
        int threads = 8;
        long rounds = 50_000L;

        System.out.println("=== 1) 非原子 long 自增（易出现丢失更新）===");
        runContest(threads, rounds, new UnsafeCounter());

        System.out.println("\n=== 2) synchronized 保护（进程内互斥）===");
        runContest(threads, rounds, new SyncCounter());

        System.out.println("\n=== 3) AtomicLong（CAS，进程内原子）===");
        runContest(threads, rounds, new AtomicCounter());
    }

    private static void runContest(int threads, long roundsPerThread, Counter counter)
            throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (long r = 0; r < roundsPerThread; r++) {
                        counter.increment();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        long expect = (long) threads * roundsPerThread;
        start.countDown();
        done.await();
        pool.shutdown();
        long got = counter.get();
        System.out.printf("期望=%d, 实际=%d, 差值=%d%n", expect, got, expect - got);
    }

    interface Counter {
        void increment();

        long get();
    }

    /** 典型竞态：读-改-写非原子。 */
    static final class UnsafeCounter implements Counter {
        private long value;

        @Override
        public void increment() {
            value++;
        }

        @Override
        public long get() {
            return value;
        }
    }

    /** 进程内互斥：把临界区收窄到最小。 */
    static final class SyncCounter implements Counter {
        private long value;

        @Override
        public synchronized void increment() {
            value++;
        }

        @Override
        public synchronized long get() {
            return value;
        }
    }

    static final class AtomicCounter implements Counter {
        private final AtomicLong value = new AtomicLong();

        @Override
        public void increment() {
            value.incrementAndGet();
        }

        @Override
        public long get() {
            return value.get();
        }
    }
}
