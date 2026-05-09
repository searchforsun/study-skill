# study-skill

本仓库用于**系统化学习编程与各类技术专题**：用 Agent Skill 约束产出格式，在 `learning/` 下维护路线、进度、理论与可运行示例。

## 项目作用

- **学习物料**：每个专题在 `learning/<专题名>/` 下包含 `ROADMAP.md`、`PROGRESS.md`，按阶段提供 `THEORY.md` 与 `demo/`（含 `README.md`），便于按阶段推进与复盘。
- **技能（Skill）**：`.cursor/skills/tech-domain-learning/` 中的 **`tech-domain-learning`** 技能定义了如何生成与维护上述结构（命名、理论篇幅与版式、演示说明等）。在支持 [Agent Skills](https://agentskills.io/) 的代理里启用后，可由 AI 按同一套约定辅助你开新专题或续写阶段。  
  **GitHub 上浏览/下载该技能目录（main）：** [`.cursor/skills/tech-domain-learning`](https://github.com/searchforsun/study-skill/tree/main/.cursor/skills/tech-domain-learning)

若你只关心学习笔记，可直接阅读 `learning/`；若希望在 Cursor、Claude Code、Codex 等环境中复用同一套工作流，请按下面「如何安装技能」操作。

## 仓库结构（摘要）

| 路径 | 说明 |
| --- | --- |
| `learning/` | 各技术专题的学习目录（路线、进度、阶段理论与 demo） |
| `.cursor/skills/tech-domain-learning/` | Cursor 项目级技能：`SKILL.md` + `reference/` 细则 |

## 如何安装 / 使用技能

技能本质是**含 YAML 头信息与正文的 `SKILL.md`**，通常放在**技能名文件夹**内；部分工具还会加载同目录下的 `reference/`、`scripts/` 等。本仓库技能**依赖** `reference/` 中的链接与模板，复制时请保留整个 `tech-domain-learning` 文件夹，不要只拷贝单个文件。

### Cursor

1. **整仓打开（推荐）**  
   用 Cursor 打开本仓库根目录。技能位于项目内的 `.cursor/skills/`，Cursor 会按 [Agent Skills 文档](https://cursor.com/docs/skills) 从项目路径发现技能。

2. **全局安装（任意项目可用）**  
   将文件夹  
   `tech-domain-learning`  
   复制到用户目录下的技能根路径，例如：
   - Windows：`%USERPROFILE%\.cursor\skills\tech-domain-learning\`
   - macOS / Linux：`~/.cursor/skills/tech-domain-learning/`  

   复制后若未生效，可在命令面板执行 **Developer: Reload Window** 重载窗口。

3. **其他兼容路径**  
   官方文档说明 Cursor 也可从 `.agents/skills/`、`~/.agents/skills/` 以及部分与 Claude Code / Codex 对齐的目录加载；细节以 [Cursor Skills](https://cursor.com/docs/skills) 为准。

### Claude Code

Claude Code 使用与 Agent Skills 对齐的目录布局，说明见 [Extend Claude with skills](https://docs.anthropic.com/en/docs/claude-code/skills)。

- **仅当前项目**：在本仓库或目标项目根目录创建 `.claude/skills/`，将本仓库的  
  `.cursor/skills/tech-domain-learning/`  
  **整体复制**为  
  `.claude/skills/tech-domain-learning/`  
  （其中必须包含 `SKILL.md` 与 `reference/`。）

- **所有项目通用**：复制到 `~/.claude/skills/tech-domain-learning/`（Windows 一般为 `C:\Users\<用户名>\.claude\skills\tech-domain-learning\`）。

在对话中可通过描述匹配技能的 `description` 由模型自动选用，或在 Claude Code 中用 **`/技能目录名`** 形式直接调用（目录名即技能文件夹名，例如 `tech-domain-learning`）。若新建顶层技能目录后未被识别，可重启一次 Claude Code。

### OpenAI Codex（CLI / IDE 集成）

Codex 侧的「技能」发现路径以 OpenAI 文档为准，例如 [Agent Skills（Codex）](https://developers.openai.com/codex/skills/)：常见做法包括在工作区或用户目录下的 **`.agents/skills/`** 中放置与上表相同的 `tech-domain-learning` 文件夹（仍含完整 `reference/`）。

若你使用的版本同时支持 **`.codex/skills/`**（与 Cursor 文档中「兼容目录」描述一致），也可将同一文件夹复制到该路径。以当前安装的 Codex 版本说明为准。

### 从 Git 获取本仓库

```bash
git clone https://github.com/searchforsun/study-skill.git study-skill
cd study-skill
```

之后按上文将 `tech-domain-learning` 复制到对应工具的技能目录，或在 Cursor 中直接打开本仓库即可。

## 相关链接

- [本仓库 `tech-domain-learning` 技能目录（GitHub）](https://github.com/searchforsun/study-skill/tree/main/.cursor/skills/tech-domain-learning)
- [Agent Skills 开放标准](https://agentskills.io/)
- [Cursor — Agent Skills](https://cursor.com/docs/skills)
- [Claude Code — Skills](https://docs.anthropic.com/en/docs/claude-code/skills)
- [OpenAI Codex — Agent Skills](https://developers.openai.com/codex/skills/)

## 许可与贡献

学习笔记与技能内容随仓库维护；若 fork 后修改技能路径或 `reference/` 内链接，请同步检查 `SKILL.md` 中的相对路径是否仍有效。
