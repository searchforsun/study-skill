# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

This is a **tech learning knowledge base** — not a software product. It stores systematic learning notes (roadmaps, theory, runnable demos) under `learning/` and is governed by a Claude Code skill at [github.com/searchforsun/tech-domain-learning](https://github.com/searchforsun/tech-domain-learning) that defines the conventions for creating and maintaining all content.

The README is at `README.md` and is the canonical project description.

## Skill-driven workflow

The [tech-domain-learning](https://github.com/searchforsun/tech-domain-learning) skill (invocable as `/tech-domain-learning`) defines four workflows that drive all content creation:

- **Workflow A (init)**: Given a tech domain name, research current ecosystem, generate `ROADMAP.md` and initial `PROGRESS.md`, then ask user to review before proceeding.
- **Workflow B (generate stage)**: For the current (first unfinished) stage, create `THEORY.md` + `demo/` (with `README.md`) following the quality rules in `reference/theory.md`. Run WebSearch to get real URLs for recommended reading.
- **Workflow C (supplement)**: When the user asks questions about the current stage, amend `THEORY.md` and/or demo files incrementally — never overwrite user notes.
- **Workflow D (progress update)**: When a stage is complete, update `PROGRESS.md` (status, date, duration, key takeaways) and prompt the user about the next stage.

The skill's `SKILL.md` is the workflow authority; `reference/` files are the quality/format authorities. When in doubt, read `reference/theory.md` before writing any `THEORY.md`.

## Directory conventions

```
learning/<Chinese domain name>/
├── ROADMAP.md            # Full learning roadmap
├── PROGRESS.md           # Current stage, history, status
└── 01-<Chinese stage name>/
    ├── THEORY.md         # Theory content (strict format, see reference/theory.md)
    └── demo/
        ├── README.md     # Required: file overview, run commands, learning order (Chinese)
        └── ...
```

- `THEORY.md` must use `## 一、`, `## 二、`… as top-level sections.
- Each `##` section must end with a **核心概念** (core concepts) list and an **拓展提问提示词** (standalone AI prompt block).
- `demo/README.md` must include: file-to-knowledge mapping table, environment requirements, run commands, learning suggestions.
- Roadmap stages: 4–8 total, ASCII arrow diagram required in `ROADMAP.md`.

## Learning domain status

Current learning status lives in `PROGRESS.md` within each domain folder (e.g. `learning/Java 分布式架构/PROGRESS.md`). The README's "Current learning domains" table is the canonical overview — do not duplicate stage status in CLAUDE.md.

## Running demo code

Java demos are Maven projects. From a `demo/` directory:

```bash
# Compile and run a specific class
mvn -q compile exec:java -Dexec.mainClass=com.study.distributed.stage01.RaceConditionDemo

# Windows PowerShell (quote the -D argument)
mvn -q compile exec:java "-Dexec.mainClass=com.study.distributed.stage01.RaceConditionDemo"
```

Docker Compose demos (e.g., RocketMQ stage 1): `docker-compose up -d` from the demo directory.

## Key quality rules (from reference/theory.md)

- Every `THEORY.md` section needs: **index layer** (knowledge point table), **argument layer** (causal reasoning with `###` sub-headings), **concrete layer** (examples, diagrams, demo references).
- No section may consist solely of tables/lists — narrative paragraphs are required.
- Recommended reading URLs must be real and retrieved via WebSearch at write time; never invent URLs.
- Code comments in Chinese; filenames follow toolchain conventions (English for code, Chinese for directories).
