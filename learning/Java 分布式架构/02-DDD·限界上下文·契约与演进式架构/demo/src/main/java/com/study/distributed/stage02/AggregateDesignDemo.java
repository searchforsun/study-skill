package com.study.distributed.stage02;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 聚合设计演示：聚合内强一致 vs 跨聚合最终一致
 *
 * 知识点：
 * 1. 聚合根作为唯一入口，保证聚合内一致性
 * 2. 跨聚合通过 ID 引用，不直接访问内部状态
 * 3. 跨聚合协作通过领域事件而非跨聚合事务
 */
public class AggregateDesignDemo {

    public static void main(String[] args) {
        System.out.println("=== 聚合设计演示 ===\n");

        // 场景：订单创建时校验库存
        // 错误做法：订单聚合直接调用库存聚合（跨聚合事务）
        // 正确做法：订单聚合发布事件，库存聚合订阅并扣减（最终一致）

        // 创建订单上下文
        OrderContext orderContext = new OrderContext();

        // 模拟商品上下文中的库存
        Inventory inventory = new Inventory();
        inventory.addStock("P001", 10);
        System.out.println("初始库存：P001 = " + inventory.getStock("P001") + " 件\n");

        // 尝试创建订单（内部校验）
        System.out.println("【尝试】创建订单：商品 P001，数量 3");
        Order order = orderContext.createOrder("P001", 3);

        if (order != null) {
            System.out.println("  订单创建成功，订单号：" + order.orderId);
            System.out.println("  订单状态：" + order.status);
            System.out.println("\n【验证】聚合内一致性：订单金额 = 商品单价 × 数量");
            System.out.println("  商品单价（从外部查询）：" + orderContext.getProductPrice("P001"));
            System.out.println("  订单金额（聚合内计算）：" + order.totalAmount);
            System.out.println("  一致性：" + (order.totalAmount == orderContext.getProductPrice("P001") * 3 ? "✓" : "✗"));
        } else {
            System.out.println("  订单创建失败（库存不足）");
        }

        System.out.println("\n=== 核心结论 ===");
        System.out.println("1. 聚合内操作是原子的，通过聚合根保证一致性");
        System.out.println("2. 聚合间通过 ID 引用，不直接访问对方状态");
        System.out.println("3. 跨聚合协作用领域事件，避免分布式事务");
    }

    // ==================== 订单上下文 ====================

    static class OrderContext {
        private final List<Order> orders = new ArrayList<>();

        public Order createOrder(String productId, int quantity) {
            // 聚合内操作：校验业务不变式
            double unitPrice = getProductPrice(productId);
            double totalAmount = unitPrice * quantity;

            // 创建订单聚合
            Order order = new Order(
                UUID.randomUUID().toString(),
                productId,
                quantity,
                unitPrice,
                totalAmount
            );

            orders.add(order);
            return order;
        }

        public double getProductPrice(String productId) {
            // 简化：从外部查询（实际应通过上下文映射获取）
            return 100.0;
        }
    }

    static class Order {
        String orderId;
        String productId;
        int quantity;
        double unitPrice;
        double totalAmount;
        String status;

        Order(String orderId, String productId, int quantity,
              double unitPrice, double totalAmount) {
            this.orderId = orderId;
            this.productId = productId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalAmount = totalAmount;
            this.status = "CREATED";

            // 聚合不变式校验
            if (quantity <= 0) {
                throw new IllegalArgumentException("订单数量必须大于0");
            }
            if (totalAmount != unitPrice * quantity) {
                throw new IllegalStateException("订单金额计算错误");
            }
        }
    }

    // ==================== 库存上下文（简化） ====================

    static class Inventory {
        private java.util.Map<String, Integer> stocks = new java.util.HashMap<>();

        public void addStock(String productId, int quantity) {
            stocks.put(productId, stocks.getOrDefault(productId, 0) + quantity);
        }

        public int getStock(String productId) {
            return stocks.getOrDefault(productId, 0);
        }

        public boolean deduct(String productId, int quantity) {
            int current = getStock(productId);
            if (current < quantity) {
                return false;
            }
            stocks.put(productId, current - quantity);
            return true;
        }
    }
}