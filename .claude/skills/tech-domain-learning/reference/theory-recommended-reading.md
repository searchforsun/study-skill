# 推荐阅读排版规范

## 核心规则

| 规则 | 说明 |
|------|------|
| 说明在前 | 描述文字在 URL 之前 |
| 链接行仅 URL | URL 单独一行，该行**仅含 URL**，前后可留白，勿在同一行拼接其他字符 |
| 每条格式 | **加粗标题** → `与本阶段：…` → 纯 URL → `检索：`<code>关键词</code> |

## 推荐阅读结构（每条）

```markdown
- **文档标题**
  - 关联主题：<本节涵盖的核心内容>
  - https://（完整 URL）
  - 检索：`<检索关键词>`
```

## 示例

```markdown
- **Kubernetes 官方文档 — Workloads: Deployment**
  - 关联主题：滚动更新策略、maxSurge/maxUnavailable 配置、探针配置示例。
  - https://kubernetes.io/docs/concepts/workloads/controllers/deployment/
  - 检索：`Kubernetes Deployment rollingUpdate maxSurge maxUnavailable`
```

## 时效说明

- 以撰写时检索到的官方文档入口为准
- 日后发现 URL 失效，重新检索替换
- 优先官方文档/规范/权威教程，其次是被广泛引用的开源权威教程