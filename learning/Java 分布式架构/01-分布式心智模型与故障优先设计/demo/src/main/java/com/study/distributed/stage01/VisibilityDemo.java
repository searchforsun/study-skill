package com.study.distributed.stage01;

/**
 * 可见性演示：JMM 下指令重排与缓存导致变量可见性问题
 *
 * 知识点：线程对共享变量的读写不一定立刻同步到主内存
 * 预期：可能输出 0（value 未同步）或 42，或程序不退出（flag=true 但 value 仍为 0）
 */
public class VisibilityDemo {
    private static boolean flag = false;
    private static int value = 0;

    public static void main(String[] args) throws InterruptedException {
        Thread writer = new Thread(() -> {
            value = 42;    // 先写 value
            flag = true;   // 后写 flag（可能指令重排）
        }, "Writer");

        Thread reader = new Thread(() -> {
            // 可能看到 flag=true 但 value 仍为 0
            while (!flag) {
                // 空循环等待
            }
            System.out.println("读到 value = " + value);
            if (value != 42) {
                System.out.println("可见性问题：flag=true 但 value 不是 42");
            }
        }, "Reader");

        writer.start();
        reader.start();
        writer.join();

        // 3 秒后强制退出（防止极端情况程序卡住）
        Thread.sleep(3000);
        System.out.println("演示结束");
    }
}