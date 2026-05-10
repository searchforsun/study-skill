package com.study.distributed.stage02;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 领域事件演示：跨聚合异步协作
 *
 * 知识点：
 * 1. 领域事件是「已发生的事实」，不可变
 * 2. 事件驱动实现跨聚合最终一致性
 * 3. 订单聚合与库存聚合通过事件解耦
 */
public class DomainEventDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 领域事件演示 ===\n");

        // 事件总线（简化版）
        EventBus eventBus = new EventBus();

        // 库存聚合（订阅者）
        InventoryService inventoryService = new InventoryService();
        inventoryService.subscribe(eventBus);
        inventoryService.addStock("P001", 100); // 初始库存
        System.out.println("初始库存：P001 = " + inventoryService.getStock("P001") + " 件\n");

        // 订单聚合（发布者）
        OrderService orderService = new OrderService(eventBus);

        // 创建订单，触发领域事件
        System.out.println("【步骤1】创建订单：商品 P001，数量 5");
        Order order = orderService.createOrder("P001", 5);
        System.out.println("  订单已创建，发布 OrderPlaced 事件\n");

        // 模拟事件异步处理
        System.out.println("【步骤2】事件异步处理（库存聚合订阅并扣减）");
        Thread.sleep(100); // 模拟异步延迟

        System.out.println("\n【验证结果】");
        System.out.println("  订单状态：" + order.status);
        System.out.println("  库存剩余：" + inventoryService.getStock("P001") + " 件");
        System.out.println("  库存扣减成功：" + (inventoryService.getStock("P001") == 95 ? "✓" : "✗"));

        System.out.println("\n=== 核心结论 ===");
        System.out.println("1. 领域事件命名用过去式（OrderPlaced），代表已发生的事实");
        System.out.println("2. 发布者与订阅者解耦，跨聚合协作无需分布式事务");
        System.out.println("3. 事件不可变，事件内容即事实本身");
        System.out.println("4. 跨聚合最终一致，通过补偿处理不一致（如库存不足时的退款）");
    }

    // ==================== 领域事件 ====================

    interface DomainEvent {
        String eventType();
        long occurredAt();
    }

    static class OrderPlacedEvent implements DomainEvent {
        final String orderId;
        final String productId;
        final int quantity;
        final long timestamp;

        OrderPlacedEvent(String orderId, String productId, int quantity) {
            this.orderId = orderId;
            this.productId = productId;
            this.quantity = quantity;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public String eventType() { return "OrderPlaced"; }

        @Override
        public long occurredAt() { return timestamp; }
    }

    // ==================== 事件总线 ====================

    static class EventBus {
        private final List<EventSubscriber> subscribers = new CopyOnWriteArrayList<>();

        public void subscribe(EventSubscriber subscriber) {
            subscribers.add(subscriber);
        }

        public void publish(DomainEvent event) {
            for (EventSubscriber subscriber : subscribers) {
                subscriber.onEvent(event);
            }
        }
    }

    interface EventSubscriber {
        void onEvent(DomainEvent event);
    }

    // ==================== 订单聚合（发布者） ====================

    static class OrderService {
        private final EventBus eventBus;
        private final List<Order> orders = new ArrayList<>();

        OrderService(EventBus eventBus) {
            this.eventBus = eventBus;
        }

        public Order createOrder(String productId, int quantity) {
            Order order = new Order(productId, quantity);
            orders.add(order);

            // 发布领域事件
            OrderPlacedEvent event = new OrderPlacedEvent(order.orderId, productId, quantity);
            eventBus.publish(event);

            order.status = "PLACED";
            return order;
        }
    }

    static class Order {
        static int counter = 0;
        final String orderId;
        final String productId;
        final int quantity;
        String status;

        Order(String productId, int quantity) {
            this.orderId = "ORD-" + (++counter);
            this.productId = productId;
            this.quantity = quantity;
            this.status = "PENDING";
        }
    }

    // ==================== 库存聚合（订阅者） ====================

    static class InventoryService implements EventSubscriber {
        private java.util.Map<String, Integer> stocks = new java.util.HashMap<>();

        public void addStock(String productId, int quantity) {
            stocks.put(productId, stocks.getOrDefault(productId, 0) + quantity);
        }

        public int getStock(String productId) {
            return stocks.getOrDefault(productId, 0);
        }

        @Override
        public void onEvent(DomainEvent event) {
            if (event instanceof OrderPlacedEvent) {
                OrderPlacedEvent e = (OrderPlacedEvent) event;
                deductStock(e.productId, e.quantity);
            }
        }

        private void deductStock(String productId, int quantity) {
            int current = stocks.getOrDefault(productId, 0);
            if (current >= quantity) {
                stocks.put(productId, current - quantity);
                System.out.println("  [库存事件处理] 扣减库存：" + productId + " × " + quantity + "，剩余 " + stocks.get(productId));
            } else {
                System.out.println("  [库存事件处理] 库存不足，无法扣减！");
            }
        }
    }
}