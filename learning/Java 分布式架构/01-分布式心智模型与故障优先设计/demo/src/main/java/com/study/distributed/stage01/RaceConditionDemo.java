package com.study.distributed.stage01;

/**
 * 竞态条件演示：read-modify-write 非原子性问题
 *
 * 知识点：多线程并发下 ++ 操作不是原子的
 * 预期：多次运行结果不同，多数 < 20000
 */
public class RaceConditionDemo {
    private static int counter = 0;

    public static void main(String[] args) throws InterruptedException {
        Runnable task = () -> {
            for (int i = 0; i < 10000; i++) {
                counter++;  // read-modify-write，非原子
            }
        };

        Thread t1 = new Thread(task, "Thread-A");
        Thread t2 = new Thread(task, "Thread-B");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("最终值：" + counter + "（期望 20000）");
        if (counter < 20000) {
            System.out.println("发现竞态：counter 值小于预期，证明多线程并发修改存在丢失");
        }
    }
}