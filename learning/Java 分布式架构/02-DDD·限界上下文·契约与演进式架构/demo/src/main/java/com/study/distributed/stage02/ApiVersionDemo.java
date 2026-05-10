package com.study.distributed.stage02;

import java.util.HashMap;
import java.util.Map;

/**
 * API 版本兼容策略演示
 *
 * 知识点：
 * 1. 顺序兼容（向后兼容）：新增字段不影响旧消费者
 * 2. 破坏性变更：删除/改字段类型导致旧消费者崩溃
 * 3. 版本共存：多版本同时运行，逐步迁移
 */
public class ApiVersionDemo {

    public static void main(String[] args) {
        System.out.println("=== API 版本兼容演示 ===\n");

        // 模拟 API 版本演进
        ApiV1 v1 = new ApiV1();
        ApiV2 v2 = new ApiV2();

        // v1 消费者
        System.out.println("【V1 消费者】调用 /api/v1/products");
        Map<String, Object> v1Response = v1.getProduct("P001");
        System.out.println("  响应：" + v1Response);
        System.out.println("  消费者处理：name=" + v1Response.get("name") + ", price=" + v1Response.get("price"));

        System.out.println();

        // v2 消费者（新增 category 字段）
        System.out.println("【V2 消费者】调用 /api/v2/products");
        Map<String, Object> v2Response = v2.getProduct("P001");
        System.out.println("  响应：" + v2Response);
        System.out.println("  消费者处理：name=" + v2Response.get("name") + ", price=" + v2Response.get("price") + ", category=" + v2Response.get("category"));

        System.out.println();

        // 破坏性变更演示
        System.out.println("【破坏性变更场景】假设删除了 price 字段");
        Map<String, Object> breakingResponse = v2.getProductBreaking("P001");
        System.out.println("  响应：" + breakingResponse);
        System.out.println("  V1 消费者尝试获取 price：");
        if (breakingResponse.containsKey("price")) {
            System.out.println("    price = " + breakingResponse.get("price"));
        } else {
            System.out.println("    ✗ 字段不存在！V1 消费者崩溃或行为异常");
        }

        System.out.println("\n=== 核心结论 ===");
        System.out.println("1. 顺序兼容：只新增字段，不删除/修改旧字段");
        System.out.println("2. 版本共存：新旧版本同时运行，给消费者迁移时间");
        System.out.println("3. 破坏性变更须提前通知，有明确的迁移窗口期");
        System.out.println("4. 推荐实践：契约测试（CDC）验证兼容性，防止契约漂移");
    }

    // ==================== V1 API ====================

    static class ApiV1 {
        public Map<String, Object> getProduct(String productId) {
            Map<String, Object> product = new HashMap<>();
            product.put("id", productId);
            product.put("name", "笔记本电脑");
            product.put("price", 7999.0);
            return product;
        }
    }

    // ==================== V2 API（顺序兼容） ====================

    static class ApiV2 {
        public Map<String, Object> getProduct(String productId) {
            Map<String, Object> product = new HashMap<>();
            product.put("id", productId);
            product.put("name", "笔记本电脑");
            product.put("price", 7999.0);
            product.put("category", "电子产品"); // 新增字段，V1 消费者忽略
            product.put("description", "高性能轻薄本"); // 新增字段，V1 消费者忽略
            return product;
        }

        // 破坏性变更示例（实际不应这样做）
        public Map<String, Object> getProductBreaking(String productId) {
            Map<String, Object> product = new HashMap<>();
            product.put("id", productId);
            product.put("name", "笔记本电脑");
            // price 字段被删除或改名
            product.put("amount", 7999.0); // 改名为 amount
            return product;
        }
    }
}