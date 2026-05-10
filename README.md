# study-skill

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Agent Skills](https://img.shields.io/badge/Agent_Skills-compatible-6e47ff?logo=openai)](https://agentskills.io/)
[![Cursor](https://img.shields.io/badge/Cursor-ready-00b27e)](https://cursor.com/docs/skills)
[![Claude Code](https://img.shields.io/badge/Claude_Code-ready-d97757)](https://docs.anthropic.com/en/docs/claude-code/skills)

**AI 驱动的系统化技术学习框架** —— 用 Agent Skill 将任意技术专题拆解为路线、阶段讲义与可运行 Demo，由 AI 按统一质量约定持续产出，你只需专注学习与复盘。

---

## 为什么用这个仓库

| 痛点 | 本仓库解法 |
|------|-----------|
| 收藏一堆链接从不打开 | **AI 按阶段交付** `THEORY.md`，因果叙事 + 关键概念 + 拓展提问，一份文档读完一个主题 |
| 教程版本过时、URL 失效 | 技能强制 **知识来源四准则**（稳定 / 可信 / 真实 / 前沿），WebSearch 验证后写入 |
| 看懂了但写不出代码 | 每阶段配套 **可运行 `demo/`**，本地跑起来改参数做实验 |
| 学习进度不可追踪 | `PROGRESS.md` 是唯一进度真相，阶段状态与完成记录一目了然 |
| 换个方向又要从零规划 | 同一套技能对任何技术方向复用，说出方向即可生成路线 |

## 学习工作流

```
用户说出技术方向
       |
       v
  ROADMAP.md          <-- 4~8 阶段路线图，含 ASCII 箭头图 + 阶段详情表
       |
       v
  按阶段推进
       |
       +---> THEORY.md     <-- 理论讲义（索引表 + 因果论证 + 具象示例 + 核心概念 + 拓展提问）
       +---> demo/          <-- 可运行代码（Maven / Docker Compose / SQL 等，含 README.md）
       |
       v
  PROGRESS.md          <-- 追踪每阶段状态、日期、时长、收获
```

## 快速开始

```bash
# 1. 克隆仓库
git clone https://github.com/searchforsun/study-skill.git
cd study-skill

# 2. 仅浏览学习笔记 —— 直接打开 learning/
#    各专题下按阶段编号排列，从 ROADMAP.md 开始

# 3. 用到 AI 编辑器中 —— 安装技能（见下方）
```

## 技能安装

将 `tech-domain-learning` 技能安装到你的 AI 编辑器中，之后在对话里说 **"帮我学 <技术方向>"** 即可触发。

<details>
<summary><b>Cursor</b></summary>

**方式一：整仓打开（推荐）**

用 Cursor 直接打开本仓库根目录，`.cursor/skills/` 下的技能自动被识别。

**方式二：全局安装**

将 `tech-domain-learning/` 文件夹复制到：

```
Windows:  %USERPROFILE%\.cursor\skills\tech-domain-learning\
macOS:    ~/.cursor/skills/tech-domain-learning/
```

复制后在命令面板执行 **Developer: Reload Window**。

</details>

<details>
<summary><b>Claude Code（克隆后可直接使用）</b></summary>

本仓库已包含 `.claude/skills/tech-domain-learning/` 副本，克隆后即用。

```bash
git clone https://github.com/searchforsun/study-skill.git
cd study-skill
```

在 Claude Code 中说 **"帮我学 <技术方向>"** 即可触发，或直接 `/tech-domain-learning`。

如需在其他项目使用，可复制技能：
```bash
cp -r tech-domain-learning/ ~/.claude/skills/tech-domain-learning/
```

</details>

<details>
<summary><b>OpenAI Codex</b></summary>

复制到工作区或用户目录的 `.agents/skills/`（部分版本支持 `.codex/skills/`），详见 [Codex Agent Skills 文档](https://developers.openai.com/codex/skills/)。

</details>

## 质量约定

技能在生成每份内容后自检以下 6 条门禁：

| # | 检查项 | 规则 |
|---|--------|------|
| 1 | 时效标记 | 路线含生成日期 + "以官方文档为准"免责声明 |
| 2 | 路线图 | 4~8 阶段，含自上而下 ASCII 箭头图 |
| 3 | 反提纲 | 每节有连续因果叙述，禁止纯表格/列表充数 |
| 4 | 版式一致 | `## 一、` 顺排，每节末含核心概念 + 拓展提问提示词 |
| 5 | 知识来源 | URL/API/版本号经 WebSearch 验证，引自官方文档或权威出版物 |
| 6 | URL 真实 | 推荐阅读链接经 WebSearch 验证，链接行仅 URL |

## 仓库结构

```
study-skill/
├── README.md
├── learning/                              # 学习物料
│   ├── Java 分布式架构/
│   │   ├── ROADMAP.md
│   │   ├── PROGRESS.md
│   │   └── 01-并发核心与竞态条件/
│   │       ├── THEORY.md
│   │       └── demo/
│   │           └── README.md
│   ├── RocketMQ 消息队列/
│   └── ...（更多专题）
├── .cursor/skills/tech-domain-learning/   # Cursor 项目技能
│   ├── SKILL.md
│   └── reference/
│       ├── theory.md
│       ├── demo-readme.md
│       ├── roadmap-progress.md
│       ├── naming-and-layout.md
│       └── theory-recommended-reading.md
└── .claude/skills/tech-domain-learning/   # Claude Code 技能（同步副本）
    └── ...（同 .cursor 结构）
```

## 相关链接

- [Agent Skills 开放标准](https://agentskills.io/)
- [Cursor — Agent Skills](https://cursor.com/docs/skills)
- [Claude Code — Skills](https://docs.anthropic.com/en/docs/claude-code/skills)
- [OpenAI Codex — Agent Skills](https://developers.openai.com/codex/skills/)

## 许可

MIT. 学习笔记与技能内容随仓库维护；fork 后若修改 `reference/` 内链接，请同步检查 `SKILL.md` 中的相对路径。
