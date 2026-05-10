package com.study.distributed.stage02;

import java.util.ArrayList;
import java.util.List;

/**
 * 上下文映射演示：Customer-Supplier vs ACL
 *
 * 知识点：
 * 1. Customer-Supplier：上游提供服务，下游消费，团队独立演进
 * 2. ACL（防腐层）：在本上下文内转换外部模型，隔离外部变化
 */
public class ContextMappingDemo {

    public static void main(String[] args) {
        System.out.println("=== 上下文映射演示 ===\n");

        // 场景：订单上下文调用商品上下文获取商品信息

        // 方式一：Customer-Supplier 直接调用
        System.out.println("【方式一】Customer-Supplier（直接调用）");
        ProductService productService = new ProductService();
        Product product = productService.getProductById("P001");
        System.out.println("  商品名：" + product.name + "，价格：" + product.price);
        System.out.println("  问题：若商品上下文模型变更（如字段改名），订单上下文需同步修改\n");

        // 方式二：ACL 防腐层
        System.out.println("【方式二】ACL 防腐层（隔离外部模型）");
        ProductACL productACL = new ProductACL(new ProductService());
        OrderProduct orderProduct = productACL.getProductForOrder("P001");
        System.out.println("  订单专用商品对象：" + orderProduct.name + "，订单价格：" + orderProduct.orderPrice);
        System.out.println("  优势：商品上下文模型变化时，只需修改 ACL，订单上下文不受影响\n");

        System.out.println("=== 核心结论 ===");
        System.out.println("1. Customer-Supplier 适用于团队紧密协作、模型相对稳定的场景");
        System.out.println("2. ACL 适用于集成遗留系统或不受控的外部服务，隔离变化");
        System.out.println("3. 选择哪种映射方式，取决于团队自主性、模型稳定性、演进需求");
    }

    // 商品上下文模型（外部）
    static class ProductService {
        public Product getProductById(String id) {
            return new Product(id, "笔记本电脑", 7999.0, "USD");
        }
    }

    static class Product {
        String id;
        String name;
        double price;
        String currency; // 商品上下文特有的字段

        Product(String id, String name, double price, String currency) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.currency = currency;
        }
    }

    // 订单上下文专用的商品视图（ACL 转换结果）
    static class OrderProduct {
        String id;
        String name;
        double orderPrice; // 统一使用本上下文的计量单位

        OrderProduct(String id, String name, double orderPrice) {
            this.id = id;
            this.name = name;
            this.orderPrice = orderPrice;
        }
    }

    // 防腐层：转换外部模型为本上下文模型
    static class ProductACL {
        private final ProductService productService;

        ProductACL(ProductService productService) {
            this.productService = productService;
        }

        public OrderProduct getProductForOrder(String productId) {
            Product external = productService.getProductById(productId);
            // 转换：统一货币单位、过滤不需要的字段
            return new OrderProduct(
                external.id,
                external.name,
                convertToOrderCurrency(external.price, external.currency)
            );
        }

        private double convertToOrderCurrency(double price, String currency) {
            // 简化示例：实际应查汇率表
            if ("USD".equals(currency)) {
                return price * 7.2; // 假设 USD->CNY 汇率
            }
            return price;
        }
    }
}