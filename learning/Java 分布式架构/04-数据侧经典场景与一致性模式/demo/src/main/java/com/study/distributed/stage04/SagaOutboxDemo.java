package com.study.distributed.stage04;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Saga 编排式事务 与 Outbox 本地消息表 模式演示。
 *
 * <p>两个独立演示：
 * <ol>
 *   <li>SagaDemo：模拟「创建订单 → 扣库存 → 扣积分」长事务，正向成功 + 补偿回滚两条路径</li>
 *   <li>OutboxDemo：模拟本地事务中原子写入业务数据 + 消息表，异步投递</li>
 * </ol>
 */
public final class SagaOutboxDemo {

    // ====================================================================
    // Saga 演示：编排式状态机
    // ====================================================================
    static class SagaDemo {
        interface SagaStep {
            /** 正向操作 */
            void execute() throws Exception;
            /** 补偿操作（幂等） */
            void compensate();
            String name();
        }

        static class CreateOrderStep implements SagaStep {
            private boolean executed = false;
            @Override public String name() { return "创建订单"; }
            @Override public void execute() {
                System.out.println("    [Saga] → 创建订单：订单状态=CREATED, orderId=ORD-001");
                executed = true;
            }
            @Override public void compensate() {
                if (executed) {
                    System.out.println("    [Saga·补偿] ← 取消订单：订单状态=CANCELLED");
                    executed = false;
                }
            }
        }

        static class DeductStockStep implements SagaStep {
            private boolean executed = false;
            @Override public String name() { return "扣减库存"; }
            @Override public void execute() throws Exception {
                System.out.println("    [Saga] → 扣减库存：stock-5");
                executed = true;
                // 模拟：假设库存不足引发失败（注释掉下面一行即成功路径）
                // throw new RuntimeException("库存不足");
            }
            @Override public void compensate() {
                if (executed) {
                    System.out.println("    [Saga·补偿] ← 回补库存：stock+5");
                    executed = false;
                }
            }
        }

        static class AddPointsStep implements SagaStep {
            private boolean executed = false;
            @Override public String name() { return "发放积分"; }
            @Override public void execute() {
                System.out.println("    [Saga] → 发放积分：points+100");
                executed = true;
            }
            @Override public void compensate() {
                if (executed) {
                    System.out.println("    [Saga·补偿] ← 扣回积分：points-100");
                    executed = false;
                }
            }
        }

        static void run(boolean failAtStock) {
            System.out.println("  ─── Saga" + (failAtStock ? "（模拟失败+补偿回滚）" : "（成功路径）") + " ───");

            List<SagaStep> steps = new ArrayList<>();
            steps.add(new CreateOrderStep());
            steps.add(new DeductStockStep() {
                @Override public void execute() throws Exception {
                    System.out.println("    [Saga] → 扣减库存：stock-5");
                    if (failAtStock) throw new RuntimeException("库存不足");
                }
            });
            steps.add(new AddPointsStep());

            int executedIndex = -1;
            try {
                for (int i = 0; i < steps.size(); i++) {
                    steps.get(i).execute();
                    executedIndex = i;
                }
                System.out.println("    [Saga] ✓ 全部完成，事务成功");
            } catch (Exception e) {
                System.out.println("    [Saga] ✗ 步骤「" + steps.get(executedIndex + 1).name()
                    + "」失败: " + e.getMessage());
                // 逆序补偿已执行过的步骤
                System.out.println("    [Saga] 开始补偿回滚（逆序）...");
                for (int i = executedIndex; i >= 0; i--) {
                    steps.get(i).compensate();
                }
            }
            System.out.println();
        }
    }

    // ====================================================================
    // Outbox 演示：本地消息表原子写入 + 模拟异步投递
    // ====================================================================
    static class OutboxDemo {
        // 模拟同一数据库中的两张表
        static class OutboxRecord {
            final String id;
            final String aggregateType;
            final String aggregateId;
            final String eventType;
            final String payload;
            String status;  // PENDING / SENT

            OutboxRecord(String id, String aggregateType, String aggregateId,
                         String eventType, String payload) {
                this.id = id;
                this.aggregateType = aggregateType;
                this.aggregateId = aggregateId;
                this.eventType = eventType;
                this.payload = payload;
                this.status = "PENDING";
            }
        }

        private final Map<String, String> orderTable = new ConcurrentHashMap<>();
        private final Map<String, OutboxRecord> outboxTable = new ConcurrentHashMap<>();
        private final AtomicInteger sentCount = new AtomicInteger(0);

        /** 模拟：同一本地事务中写入订单表 + 消息表 */
        void createOrder(String orderId, String userId, int amount) {
            // 在一个"本地事务"中：BEGIN
            orderTable.put(orderId, String.format("userId=%s, amount=%d, status=CREATED", userId, amount));
            OutboxRecord event = new OutboxRecord(
                "evt-" + orderId, "Order", orderId, "OrderCreated",
                String.format("{\"orderId\":\"%s\",\"amount\":%d}", orderId, amount)
            );
            outboxTable.put(event.id, event);
            // COMMIT —— 两者的写入是原子的
            System.out.printf("    [Outbox·本地事务] 写入订单 %s + 消息 %s → COMMIT（同 DB 事务）%n",
                orderId, event.id);
        }

        /** 模拟定时任务：轮询待投递消息 */
        void pollingSend(int batchSize) {
            System.out.println("    [Outbox·定时任务] 轮询 status=PENDING 的消息...");
            int count = 0;
            for (OutboxRecord record : outboxTable.values()) {
                if (count >= batchSize) break;
                if (!"PENDING".equals(record.status)) continue;
                // 模拟投递到 MQ
                System.out.printf("    [Outbox·投递] eventId=%s, eventType=%s → MQ（模拟）%n",
                    record.id, record.eventType);
                record.status = "SENT";
                sentCount.incrementAndGet();
                count++;
            }
            if (count == 0) {
                System.out.println("    [Outbox·定时任务] 无待投递消息");
            }
        }

        void status() {
            System.out.printf("    [Outbox] 订单数=%d, 消息总数=%d, 已投递=%d%n",
                orderTable.size(), outboxTable.size(), sentCount.get());
        }
    }

    // ====================================================================

    public static void main(String[] args) {
        System.out.println();
        System.out.println("============ Saga 编排式事务 + Outbox 本地消息表 演示 ============");
        System.out.println("（纯 Java 模拟，印证 THEORY.md 第三、四节逻辑）");
        System.out.println();

        // ─── Saga ───
        SagaDemo.run(false);   // 成功路径
        SagaDemo.run(true);    // 失败 + 补偿

        // ─── Outbox ───
        System.out.println("  ═══ Outbox 本地消息表 ═══");
        OutboxDemo outbox = new OutboxDemo();
        outbox.createOrder("ORD-1001", "user-42", 299);
        outbox.createOrder("ORD-1002", "user-42", 599);
        System.out.println();

        // 第一次轮询：投递一批
        outbox.pollingSend(10);
        outbox.status();
        System.out.println();

        // 第二次轮询：无新消息
        outbox.pollingSend(10);
        System.out.println();

        // 新订单进来
        outbox.createOrder("ORD-1003", "user-77", 199);
        outbox.pollingSend(10);
        outbox.status();
        System.out.println();

        System.out.println("  关键点：outbox 写入与业务数据写入在同一个数据库事务中——");
        System.out.println("  如果业务写入成功但进程在 COMMIT 前崩溃，两者一起回滚，");
        System.out.println("  不存在「业务落库了但消息丢了」的不一致状态。");
        System.out.println();

        System.out.println("==================== 演示结束 ====================");
    }
}
