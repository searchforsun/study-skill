# 阶段二 示例说明：DDD 限界上下文与聚合设计

本目录示例通过事件风暴和聚合设计练习，验证「业务边界决定服务边界」这一核心原则。

## 示例总览

| 入口文件 / 目录 | 对应知识点 | 建议顺序 |
|----------------|-----------|----------|
| `contextmapping/` | 上下文映射（ACL/Customer-Supplier 等） | 先理解映射关系 |
| `aggregate/` | 聚合设计、聚合根、跨聚合 ID 引用 | 次之，理解一致性边界 |
| `domainevent/` | 领域事件发布与订阅、跨聚合协作 | 后运行，验证事件驱动 |
| `ApiVersionDemo.java` | API 版本兼容策略 | 按需 |

## 环境要求

- **JDK**：17+
- **Maven**：3.8+

## 运行命令

### 上下文映射演示

```bash
mvn -q compile exec:java -Dexec.mainClass=com.study.distributed.stage02.ContextMappingDemo
```

**观察**：理解 Customer-Supplier vs ACL 两种映射方式的调用链路差异。

### 聚合设计演示

```bash
mvn -q compile exec:java -Dexec.mainClass=com.study.distributed.stage02.AggregateDesignDemo
```

**观察**：聚合内事务一致性 vs 跨聚合最终一致性的边界；聚合根作为唯一入口的设计。

### 领域事件演示

```bash
mvn -q compile exec:java -Dexec.mainClass=com.study.distributed.stage02.DomainEventDemo
```

**观察**：OrderPlaced 事件触发 Inventory 扣减；事件异步协作无需跨聚合事务。

### API 版本兼容演示

```bash
mvn -q compile exec:java -Dexec.mainClass=com.study.distributed.stage02.ApiVersionDemo
```

**观察**：顺序兼容（新增字段）vs 破坏性变更的判断。

## 学习建议（如何改代码做实验）

1. **ContextMappingDemo**
   - 对比「直接调用」与「通过 ACL 转换」两种方式，哪个更利于跨团队独立演进？
   - 思考：如果 Order 服务调用 Product 服务时直接用 Product 的 model，会有什么问题？

2. **AggregateDesignDemo**
   - 尝试修改聚合边界：把 OrderItem 从 Order 聚合中拆分出来，观察事务一致性的变化。
   - 对比：聚合内强一致 vs 跨聚合最终一致的业务含义。

3. **DomainEventDemo**
   - 添加一个新事件（如 PaymentConfirmed），观察订阅者如何扩展。
   - 思考：领域事件是否适合所有跨聚合协作？什么场景下直接调用更合适？

4. **ApiVersionDemo**
   - 尝试设计一个破坏性变更（如删除字段），观察兼容性如何断裂。
   - 对比「版本共存」与「一次性升版」的迁移风险。

## 与 `THEORY.md` 的配合

先阅读理论稿 **「第二章：战略 DDD：子域与限界上下文」** 与 **「第三章：战术 DDD：聚合、领域事件与边界」**，再运行示例；运行结果作为「现象锚点」回到文中对照 **「限界上下文」「聚合根」「领域事件」「上下文映射」** 等核心概念的讨论。