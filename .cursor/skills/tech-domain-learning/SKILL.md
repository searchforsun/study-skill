---
name: tech-domain-learning
description: >-
  面向编程或任意技术领域的系统化学习：生成 `ROADMAP.md` 与 `PROGRESS.md`（Markdown），按阶段以独立文件夹交付 **`THEORY.md`**（细则见 `reference/theory.md`：篇幅、反提纲与**可读版式**（知识点显性化：表/分条 + `###` 分块、控制密段）、**结构化（最外 `## 一、`…、内层有序/无序、表格/图例）**、**每个 `## 一、`… 知识块末「核心概念 + 拓展提问提示词」**；路线核心主题逐项展开）与 **`demo/`**（含 `README.md`）；约定文件位于技能目录下 **`reference/`** 子文件夹；标准路径前缀 **`learning/`**。支持学习过程中问答以补充文档与代码注释，并在每阶段结束后更新进度。
---

# 编程技术领域 — 系统学习

用户传入**技术方向**后，按本工作流执行。技能根目录仅 **`SKILL.md`**；**分主题约定**在 [`reference/`](reference/) 下，按链接打开对应文件即可，无需单独索引页。

## 核心原则

1. **命名分层**：路径与命名见 [reference/naming-and-layout.md](reference/naming-and-layout.md)；Markdown 正文与 `demo/README.md` 说明用中文，代码注释以中文为主。
2. **时效性**：生成学习路线前，用 **WebSearch**（或等价检索）核对当前主流版本、官方文档入口、常见工具链，避免过时表述；在路线中注明「生成日期」与「请以官方文档为准」。
3. **结构**：路线须覆盖 **基础 → 实践（项目/动手）→ 优化与应用**（性能、工程化、安全、运维等按领域选取）；并在 `ROADMAP.md` 中包含一节 **箭头路线图（由上到下）**——见 [reference/roadmap-progress.md](reference/roadmap-progress.md)。
4. **单一事实来源**：同一专题下，`PROGRESS.md` 是唯一进度真相；阶段文件夹名与路线中的阶段名保持一致（建议 `序号-阶段中文名`）。
5. **审阅门禁**：交付 `ROADMAP.md` 与初始 `PROGRESS.md` 后，在**对话**中请用户审阅路线、按需调整并确认是否开始学习；确认前不生成阶段 1 的完整物料（除非用户明确要求跳过）。
6. **理论知识质量**：各阶段 **`THEORY.md` 以 [reference/theory.md](reference/theory.md) 为唯一细纲**

## 目录约定

默认根路径 **`learning/<技术专题中文简称>/`**（专题名简短中文）。根目录 **`ROADMAP.md`**、**`PROGRESS.md`**；每阶段 **`THEORY.md`** + **`demo/`**（含 **`README.md`**）。目录树与布局见 [reference/naming-and-layout.md](reference/naming-and-layout.md)。

## 工作流 A：初始化（首次）

1. 确认技术方向、专题根目录（默认 `learning/<技术专题中文简称>/`）。
2. **检索**：针对该技术栈做针对性 WebSearch，整理「当前生态要点」（版本、官方文档、主流工具）。
3. 编写 **`ROADMAP.md`**：阶段清晰、每阶段目标与产出，从基础到实践到进阶；表格 + 分阶段详情。结构模板见 [reference/roadmap-progress.md](reference/roadmap-progress.md)。
4. 初始化 **`PROGRESS.md`**：`当前阶段` 为「未开始」或「阶段 1 — …（未开始）」；阶段清单表与路线一致；「阶段完成记录」为空。
5. **暂停并交付**：请用户阅读 `ROADMAP.md`，确认是否需要调整阶段划分或深度；**询问是否确认开始学习**。
6. 用户确认开始后 → 进入 **工作流 B**，从阶段 1 开始。

## 工作流 B：生成某一阶段内容

**前置**：读取 `PROGRESS.md`，确认「当前阶段」与路线一致；若用户说「下一阶段」，则根据进度将目标设为**第一个未完成的阶段**。

对该阶段执行：

1. 新建文件夹：`learning/<技术专题中文简称>/<序号>-<阶段中文名>/`（名称与 `ROADMAP.md` 中一致）。
2. **`THEORY.md`**：严格按 [reference/theory.md](reference/theory.md) 撰写；文末 **推荐阅读**遵循 [reference/theory-recommended-reading.md](reference/theory-recommended-reading.md)，须在本步骤 **WebSearch**（或核对官网）后写入 **真实 URL** 与检索关键词。
3. **`demo/`**：
   - 放置可运行或可操作的示例（按技术类型：代码、Notebook、SQL、Dockerfile、前端组件等）。
   - **`demo/README.md`（必需）**：须满足 [reference/demo-readme.md](reference/demo-readme.md)；每个文件/子目录的作用、推荐学习顺序、环境要求、运行命令；说明如何按步骤修改实验（全文中文）。
4. 代码与脚本中加入**适量中文注释**；说明复杂处即可，不必逐行注释。
5. 将 `PROGRESS.md` 中该阶段状态更新为 **进行中**（若尚未标记）。

## 工作流 C：学习过程中的问答与补充

当用户针对**当前阶段**提问或要求补充时：

1. 定位专题目录与当前阶段文件夹（以 `PROGRESS.md` 为准）。
2. 在 **`THEORY.md`** 中追加小节（如「常见问题与补充」）或修订原有段落；若新增或改写 **`## 一、`… 大节**，须同步补全该节末尾 **核心概念 + 拓展提问提示词**（见 [reference/theory.md](reference/theory.md)）。
3. 在 **`demo/`** 相关文件中补充注释或小幅重构以增强可读性；**同步更新 `demo/README.md`** 若新增文件或改变学习顺序。

## 工作流 D：阶段学完后的进度更新

用户表示本阶段学完或验收通过后：

1. 更新 **`PROGRESS.md`**（模板与检查项见 [reference/roadmap-progress.md](reference/roadmap-progress.md)、[reference/progress-update-checklist.md](reference/progress-update-checklist.md)）：
   - 该阶段：**已完成**；填写 **完成日期**；**本阶段学习时长**（由用户提供「约 X 小时」则记入，未提供可写「待填」）。
   - **简要收获**：与用户协作写 2–5 条要点。
   - **当前阶段**：推进到下一阶段名称 +「未开始」，若已全部完成则写「全部完成」。
2. 在回复中**明确提示**：可以开始下一阶段；给出下一阶段文件夹路径、`THEORY.md` 与 `demo/README.md` 入口。

## 边界与质量

- 不虚构不存在的 API；不确定处标注「请查官方文档」并给出 **可访问的文档 URL**（撰写阶段文稿时检索得到）或明确检索关键词；推荐阅读的写法遵循 [reference/theory-recommended-reading.md](reference/theory-recommended-reading.md)。
- 路线阶段数建议 **4–8** 个，过少则合并，过多则归类。
- 尊重用户已有文件：若 `learning/<技术专题中文简称>/` 已存在，**读取并增量更新**，不要无意覆盖用户笔记（合并前先读现有内容）。若用户早期沿用旧路径（如历史仓库中的 `学习/`、`学习路线.md`），可在同一仓库内**兼容已有路径**，新建专题优先采用本技能约定的英文标准目录与文件名。

## 快速检查清单

- [ ] 路线含生成日期与检索意识，且含自上而下箭头路线图  
- [ ] 进度文件与路线阶段一一对应  
- [ ] 专题根为 `learning/<专题中文简称>/`，专题与阶段文件夹名为中文；每阶段有 `THEORY.md`（符合 [reference/theory.md](reference/theory.md) 自检：反提纲 + **可读版式** + **各 `## 一、`… 本节提要**）+ `demo/README.md`  
- [ ] 阶段结束已更新进度并提示下一入口  
