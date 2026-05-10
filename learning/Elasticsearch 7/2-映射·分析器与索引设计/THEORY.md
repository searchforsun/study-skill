# 阶段 2：映射·分析器与索引设计

> 与 [`ROADMAP.md`](../ROADMAP.md) 阶段 2 对齐。动手印证见本目录 [`demo/README.md`](demo/README.md)。**生成说明**：推荐阅读链接基于官方 7.17 Reference 入口撰写；组件与默认值持续演进，**请以 [7.17 指南](https://www.elastic.co/guide/en/elasticsearch/reference/7.17/index.html) 与团队规范为准**。

阶段 1 建立了「集群 → 索引 → 文档」的操作闭环；本阶段深入索引的内部结构——**字段如何被索引，决定了你能怎么搜**。映射（mapping）和分析器（analyzer）是查询 DSL 的共同前提，也是从「能跑通」到「搜得准」的第一道分水岭。

## 本阶段知识地图

| 块 | 你要带走的抓手 |
|----|----------------|
| 一 | 映射基础：显式 vs 动态映射，何时严格、何时宽松 |
| 二 | text vs keyword：全文搜索与精确匹配的分工，多字段（multi-field）模式 |
| 三 | 分析器：内置分析器的选择边界，自定义 analyzer 的最小组合 |
| 四 | 索引参数：`index`、`doc_values`、`norms`、`copy_to` 的取舍逻辑 |
| 五 | 索引设计实战：为「标题 + 标签 + 时间」文档设计 mapping 并解释取舍 |

**路线要点 ↔ 本文章节**

| `ROADMAP.md` 阶段 2 要点 | 本文展开位置 |
|--------------------------|----------------|
| Mapping 显式 vs 动态 | **一** |
| text vs keyword、多字段 | **二** |
| 是否需要自定义 analyzer | **三** |
| copy_to、norms、index/doc_values 基本取舍 | **四** |
| 为「标题 + 标签 + 时间」设计 mapping | **五** |

---

## 一、映射基础：显式映射与动态映射

映射定义了索引中文档字段的类型、索引方式和分析器。**它是索引的「骨架」——没有 mapping，ES 也能存数据，但你在搜索、排序、聚合时会被隐式推断坑到。** 理解映射是消除「为什么这个词搜不到」「为什么排序报错」这类问题的关键。

| # | 知识点 | 抓住什么 |
|---|--------|----------|
| 1 | 显式映射（explicit mapping） | 建索引时或通过 update mapping API 手动定义字段类型与参数 |
| 2 | 动态映射（dynamic mapping） | ES 根据 JSON 文档自动推断字段类型；由 `dynamic` 参数控制 |
| 3 | `dynamic` 四种取值 | `true`（自动添加）、`runtime`（运行时字段）、`false`（忽略）、`strict`（拒绝） |
| 4 | 动态模板（dynamic templates） | 按字段名或类型匹配规则，批量定制动态映射行为 |

### 1.1 动态映射：便利的代价

**动态映射让你不写 mapping 就能入库，但类型推断有坑。** 例如整数字段 `"count": 100` 被推断为 `long`——通常情况下没问题，但如果后续写入了 `"count": "N/A"`，ES 会拒绝：类型不兼容。更隐蔽的是字符串：ES 7.x 默认同时映射为 `text`（可全文搜索）和 `keyword`（可聚合和精确匹配），这是合理的默认值，但也会增加索引体积——对于从不做全文搜索的字段（如 UUID、邮件地址），`text` 索引是无用开销。

**对于生产数据，应优先考虑 `dynamic: strict` 或 `dynamic: false`**，迫使自己在入库前定义关键字段。学习阶段可以从 `dynamic: true` 开始，但要有意识地在之后收紧。例如 `demo/` 中的索引设计练习就要求你把动态映射改成显式 mapping，对比两种方式下 `_mapping` 的输出差异。

### 1.2 显式映射：写出来的更有掌控

**显式 mapping 的关键不是一个「全字段清单」，而是对三类字段的决策覆盖。** 第一类是搜索字段（如文章标题、正文），需要确定 analyzer、是否多字段。第二类是过滤/排序/聚合字段（如状态码、类目标签、日期），需要确定用 `keyword` 还是数值类型、`doc_values` 是否开启。第三类是不被索引的辅助字段（如来源 URL、内部备注），可以用 `"index": false` 关闭索引。

显式映射一旦创建，已有字段的类型和多数参数不可更改（只能通过 reindex 重建索引）。这条约束是路线反模式「全靠动态映射上生产」的根源：你不知道字段的推断类型是什么，直到搜索或聚合报错。`demo/README.md` 中的「修改映射并重建索引」一步会让你亲手体验这个约束。

**知识卡片：动态 vs 显式取舍**

| 场景 | 推荐策略 | 理由 |
|------|----------|------|
| 日志/审计（字段不确定） | `dynamic: true` + 动态模板 | 量太大，预定义代价高；模板控制关键字段 |
| 业务索引（字段已知） | `dynamic: strict` | 拒绝意外字段，保护 schema |
| 半结构化数据 | `dynamic: runtime` | 新字段不建索引，查询时从 `_source` 计算 |
| 学习/原型 | `dynamic: true`，跑完改成显式 | 先快速验证，再理解结构 |

---

**本节提要（延伸学习）**

- **核心概念**：显式映射；动态映射；`dynamic`（true/runtime/false/strict）；动态模板；update mapping API 的限制
- **拓展提问提示词**

> 主题：Elasticsearch 7.17 显式映射与动态映射。核心概念：显式映射、动态映射、dynamic 四种取值（true/runtime/false/strict）、动态模板、update mapping API 限制。请拓展：1）动态映射将日期字符串推断为 `date` 类型的规则对时区敏感吗？2）`dynamic: runtime` 在什么场景下比 `dynamic: true` 更具性价比？3）官方 Mapping 章节中，除了 `properties` 和 `dynamic`，索引 mapping 根级别还有哪些关键配置项？

---

## 二、text vs keyword 与多字段模式

字符串字段在 7.x 中由 `text` 和 `keyword` 两个类型覆盖，走的是完全不同的索引路径。**把这两个类型比作「两种不同的索引方式」比「两种数据类型」更准确**——同一个字符串原始值只存一份（在 `_source` 中），但可以生成两套索引数据结构。用错类型是阶段 3 搜索踩坑的第一大来源。

| # | 知识点 | 抓住什么 |
|---|--------|----------|
| 1 | `text` | 经 analyzer 分词后建立倒排索引；用于全文搜索；默认不支持排序/聚合 |
| 2 | `keyword` | 原样作为一个词项建立索引；用于精确匹配、排序、聚合 |
| 3 | 多字段（multi-field / fields） | 同一字段映射为多种类型，如 `title` 为 `text`、`title.keyword` 为 `keyword` |
| 4 | `ignore_above` | 超长 keyword 值不索引的阈值；防止高开销词项 |

### 2.1 穿透：分词与否是一切的分岔

**`text` 字段写入时被分词器拆成词项列表，查询一样要被分析；`keyword` 字段写入和查询都不经过分析器。** 对于 `"The Quick Brown Fox"` 这条数据：

- 若映射为 `text`（standard analyzer），写入后被拆为 `[the, quick, brown, fox]`。搜索 `"quick fox"` 会匹配，因为查询也被分析为 `[quick, fox]` 两个词项，ES 在倒排索引中找到它们即可。
- 若映射为 `keyword`，写入后索引中只有 `"The Quick Brown Fox"` 这一个词项。搜索 `"quick"`（`term` 查询）**不会匹配**，因为大小写和完整值不一致。

**常见反模式是把 `term` 查询用于 `text` 字段**——没有分析的 `term` 会在 `text` 的倒排索引里找一个不存在的小写词项，结果为空。反之，把 `match` 查询用于 `keyword` 字段虽然不会报错，但其分析行为可能让你产生「模糊匹配」的错觉。

### 2.2 多字段（multi-field）：一套数据的两种视角

7.x 的默认字符串映射自动创建 `.keyword` 子字段——这是 7.x 替代旧版 `_all` 和 `string` 类型混乱后的统一方案。**每个 `text` 字段建议同时保留一个 `keyword` 子字段**，让你在同一字段上既做全文搜索又做排序/聚合。

例如 `demo/` 中文章标题字段 `title`：

```json
"title": {
  "type": "text",
  "analyzer": "standard",
  "fields": {
    "keyword": {
      "type": "keyword",
      "ignore_above": 256
    }
  }
}
```

你可以在搜索时对 `title` 做 `match`，同时对 `title.keyword` 做 `term` 聚合按标题精确分组。这两个视角操作的是不同的索引结构，共享同一份 `_source` 数据。

多字段还可以用于同一文本的不同分析器——例如对 `title` 使用 standard analyzer，对 `title.english` 使用 english analyzer（含词干提取），通过 `multi_match` 跨字段查询提升召回。这在阶段 3 会有更完整的示范。

---

**本节提要（延伸学习）**

- **核心概念**：`text`；`keyword`；倒排索引与分析路径；多字段（multi-field / fields）；`ignore_above`；`term` vs `match` 与字段类型的对应关系
- **拓展提问提示词**

> 主题：Elasticsearch 7.17 字符串字段类型 text vs keyword 与多字段模式。核心概念：text、keyword、倒排索引、分词、多字段（fields）、ignore_above、term 查询与 match 查询的字段类型适用边界。请拓展：1）能否在同一查询中对 `title`（text）做全文匹配同时按 `title.keyword` 聚合？2）`ignore_above` 的 256 默认值偏移对哪些场景不够用？3）动态映射将字符串同时映射为 text 和 keyword 的设计存在什么成本？

---

## 三、分析器：内置与自定义

分析器（analyzer）是 `text` 字段的加工管道：**输入一段原始文本，输出一组词项（terms）**。这三个组件按顺序执行——字符过滤器清洗、分词器拆分、词项过滤器再加工——缺一不可。选择正确的 analyzer，远比调查询参数更能决定搜索质量。

| # | 知识点 | 抓住什么 |
|---|--------|----------|
| 1 | 分析器三组件 | char_filter（预处理）→ tokenizer（分词）→ filter（后处理） |
| 2 | 内置分析器 | standard / simple / whitespace / keyword / english 等，各有适用边界 |
| 3 | 自定义分析器 | `type: custom`，自由组合三组件 |
| 4 | `_analyze` API | 不带索引上下文测试分析效果的最快手段 |

### 3.1 内置分析器：选对就够用一半

**`standard` 分析器是 7.x 默认值，以 Unicode 文本分段算法分词、转小写、去除大部分符号。** 对于英文场景它是个可靠起点，但在中文场景下不按词切分而按字切分——这直接决定了中文全文搜索的召回方式（阶段 3 会展开）。下表对比几个常用内置分析器对 `"The Quick Brown Fox!"` 的处理：

| 分析器 | 输出词项 | 特征 |
|--------|----------|------|
| `standard` | `[the, quick, brown, fox]` | Unicode 分词 + 小写化；默认 |
| `simple` | `[the, quick, brown, fox]` | 非字母字符分割 + 小写化 |
| `whitespace` | `[The, Quick, Brown, Fox!]` | 仅按空白分割，不转小写 |
| `keyword` | `[The Quick Brown Fox!]` | 不分词，整串作为一个词项 |
| `english` | `[the, quick, brown, fox]` | 同 standard + 词干提取（fox 本就不变） |

**选择逻辑**：想做全文搜索→起码 `standard`；想做精确匹配→用 `keyword` 类型字段而不是 keyword 分析器；英文文本需要词干→ `english` 或自定义。`_analyze` API 是你在选 analyzer 之前的试纸——`demo/scripts/` 中包含对同一文本用不同分析器的对比命令，建议每个都跑一遍。

### 3.2 自定义分析器：最低成本的「量身定制」

**当内置分析器的行为只是「差一点点」时，不要忍，用自定义分析器补上。** 一个自定义分析器只需指定 `tokenizer`（必需）+ 可选的 `char_filter` 和 `filter`。最经典的场景是：standard 分词够用，但想去掉 HTML 标签 + 停用词。

例如 `demo/` 中为文章内容设计的自定义分析器 `my_article_analyzer`：

```json
"analysis": {
  "analyzer": {
    "my_article_analyzer": {
      "type": "custom",
      "char_filter": ["html_strip"],
      "tokenizer": "standard",
      "filter": ["lowercase", "stop"]
    }
  }
}
```

对 `"<p>The Quick Brown <b>Fox</b></p>"` 先剥离 HTML（char_filter），再按 standard 分词，最后小写化 + 去除停用词——结果是 `[quick, brown, fox]` 三个有意义的词项。

**不需要自定义的情况**：字段不做全文搜索（直接用 `keyword` 类型）、英文标准场景（`english` 分析器已够）、非搜索字段（关掉 `index` 就够了）。避免在「标准值应当覆盖 90% 场景」的时候一头扎进分析器微调——本阶段的重点是建立分析管道的心智模型，不是成为分词专家。

---

**本节提要（延伸学习）**

- **核心概念**：analyzer；char_filter → tokenizer → filter 管道；内置分析器（standard/simple/whitespace/keyword/english）；自定义分析器；`_analyze` API
- **拓展提问提示词**

> 主题：Elasticsearch 7.17 分析器管道——内置与自定义。核心概念：analyzer、char_filter、tokenizer、token filter、standard/english/keyword 分析器、自定义分析器、_analyze API。请拓展：1）`standard` 分析器处理中文文本时按字切分对短语匹配有什么影响？2）在同一索引中对不同字段使用不同分析器是如何配置的？3）官方文档中 `ngram` tokenizer 与 `edge_ngram` tokenizer 各自适合哪种搜索场景？

---

## 四、索引参数取舍

映射不仅定义字段类型，还通过一系列布尔/选项参数控制索引的存储、搜索和排序行为。**这些参数是索引体积与查询能力的「开关面板」——默认值对多数场景是合理的，但知道每一项的代价才知道何时该关掉。**

| # | 知识点 | 抓住什么 |
|---|--------|----------|
| 1 | `index` | `false` 时字段不进入倒排索引，不可搜索但仍在 `_source` 中 |
| 2 | `doc_values` | 列式存储，排序/聚合的前提；默认开，纯搜索字段可关 |
| 3 | `norms` | 存储字段级评分归一化因子；只对参与评分的 text 字段有意义 |
| 4 | `copy_to` | 将多字段值复制到目标字段统一搜索，不存 `_source` |

### 4.1 `index`：搜索的开关

**`"index": false` 告诉 ES 不要在倒排索引中建立这个字段。** 字段仍保留在 `_source` 中（可通过 `GET` 读取），但不能作为搜索目标——`match`、`term`、`range` 都会返回空或报错。典型适用对象：大段纯展示文本（如文章原文链接、备注）、仅用于回显的字段。

关闭 `index` 并不省 `_source` 的存储空间，只减少倒排索引的体积。如果你连回显都不需要，可以在 mapping 级别关闭 `_source`（索引级别，`"enabled": false`）；但这样会失去 `_reindex` 和 `_update` 能力，极少推荐。`demo/` 中包含「标记一个字段 `index: false` 后对比搜索行为」的实验。

### 4.2 `doc_values`：排序和聚合的基石

**`doc_values` 以列式结构存储字段值，是排序、聚合、脚本访问的底层依赖。** 默认对除 `text` 和 `annotated_text` 以外的所有字段开启。如果你的 `keyword` 或数值字段只用于过滤（`term`/`range`）和显示，不参与排序和聚合，可以设置 `"doc_values": false` 来节省磁盘空间——但要注意，关闭后再做聚合会直接报错。

text 字段本身不支持 `doc_values` 和聚合，这正是 `multi-field` 模式存在的根本原因：text 做搜索，`.keyword` 子字段利用 `doc_values` 做聚合。一开一关之间是索引体积与查询能力的权衡——100 万文档的 `keyword` 的 `doc_values` 可能占几十 MB，但 100 个这样的字段就会显著影响内存和磁盘。

### 4.3 `norms`：评分的开关

**`norms` 存储每个字段的归一化因子（字段长度、索引时权重），让 `match` 查询能对短文档给更高分。** 这是相关性评分的组成部分，但存储开销不可忽略。如果你的 text 字段只用于过滤（不参与评分排序）、或者你对默认 TF-IDF/BM25 评分不关心，可以设 `"norms": false` 来节省空间。

**注意**：`norms` 只能关闭，不能事后重开——你需要在 mapping 决策时就想好。

### 4.4 `copy_to`：多字段汇聚搜索

**`copy_to` 将多个字段的值拼接到一个隐藏目标字段，让你用一次查询搜索多个字段。** 例如将 `title`、`tags`、`summary` 三字段复制到 `full_search`，查询时只搜 `full_search` 即可。与在查询体写多字段 `multi_match` 的区别是：`copy_to` 在索引时完成拼接，查询更快；但增加了索引体积，且复制后的值不能反过来取值（因为不存 `_source`）。

`demo/README.md` 中包含 `copy_to` 与 `multi_match` 的并排对比实验——分别建两个索引，写相同数据，观察查询路径和结果差异。

---

**本节提要（延伸学习）**

- **核心概念**：`index`；`doc_values`；`norms`；`copy_to`；`_source`；倒排索引 vs 列式存储
- **拓展提问提示词**

> 主题：Elasticsearch 7.17 索引参数取舍。核心概念：index、doc_values、norms、copy_to、enabled、_source、倒排索引、列式存储。请拓展：1）关闭 `_source` 后 `_reindex` 为何无法工作？2）如果 90% 的查询都是时间范围过滤 + `keyword` 精确匹配，`doc_values` 和 `norms` 应该如何调整？3）`copy_to` 在包含大量字段时对 indexing throughput 的影响大致是什么量级？

---

## 五、索引设计实战：标题 + 标签 + 时间

本节的目的是：**用前面四节的知识点，设计一个具体的 mapping，并解释每一步为什么这样做。** 场景设定：一个文章搜索索引，每条文档包含标题、标签列表、正文摘要、发布时间和作者信息。

### 5.1 需求拆解

| 需求 | 涉及字段 | 索引行为 |
|------|----------|----------|
| 按标题全文搜索 | `title` | text + analyzer |
| 按标签精确过滤 | `tags` | keyword 数组 |
| 按标签聚合统计 | `tags` | 需要 doc_values |
| 标题精确去重/排序 | `title` | 需要 keyword 子字段 |
| 发布时间范围过滤 + 排序 | `publish_time` | date 类型 |
| 正文摘要全文搜索 | `summary` | text，stored 可选 |
| 作者名精确过滤 | `author` | keyword |
| 全文跨字段搜索 | 多字段 | copy_to |

### 5.2 完整 mapping 与取舍说明

```json
PUT /articles
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "analysis": {
      "analyzer": {
        "my_article_analyzer": {
          "type": "custom",
          "char_filter": ["html_strip"],
          "tokenizer": "standard",
          "filter": ["lowercase", "stop"]
        }
      }
    }
  },
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "title": {
        "type": "text",
        "analyzer": "my_article_analyzer",
        "norms": true,
        "fields": {
          "keyword": {
            "type": "keyword",
            "ignore_above": 256
          }
        }
      },
      "tags": {
        "type": "keyword"
      },
      "summary": {
        "type": "text",
        "analyzer": "my_article_analyzer",
        "norms": false
      },
      "publish_time": {
        "type": "date",
        "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
      },
      "author": {
        "type": "keyword"
      },
      "full_search": {
        "type": "text",
        "analyzer": "my_article_analyzer"
      }
    }
  }
}
```

**逐字段解释**：

- **`dynamic: strict`**：文章字段是预先设计好的，拒绝意外字段写入，保护 schema。
- **`title` → text + keyword 子字段**：全文搜索走 `title`，排序或精确标题匹配走 `title.keyword`。`norms: true` 因为标题长度差异会影响相关性（短标题命中权重应高于长标题）。
- **`tags` → keyword 数组**：标签用作文档分类和精确过滤，不做全文搜索，keyword 即可。数组类型由 ES 自动处理——索引时写入 `["java", "elasticsearch"]`，ES 会为每个值建独立词项。
- **`summary` → text, norms: false**：摘要用于搜索但不参与评分——只要命中即可，`norms` 关掉省空间。
- **`publish_time` → date, 多格式**：接受标准日期时间、日期和毫秒时间戳。阶段 3 会展开 date 范围查询和 `date_histogram` 聚合。
- **`author` → keyword**：作者名用于精确匹配和聚合。
- **`full_search` → text（copy_to 目标）**：在索引脚本中通过 `copy_to` 汇聚 `title`、`tags`、`summary` 的值。查询时只需搜 `full_search` 就能跨字段召回。

**修正**：上述 mapping 中 `copy_to` 需要在来源字段上声明——即在 `title`、`tags`、`summary` 上各加 `"copy_to": "full_search"`。这正是 `demo/` 中实际使用的版本，见 `demo/scripts/`。

### 5.3 反模式检视

**别为「所有字段都能搜」而无脑加 copy_to。** `copy_to` 在索引时会增加写入 CPU 与倒排索引体积。如果你只有 3 个字段，`multi_match` 查询就足够；当字段数量到 10+ 且查询逻辑固定时，`copy_to` 才体现出查询简洁性和提升速度的优势。本 demo 中两种方式都会演示，让你亲眼看到查询体写法的差异。

**别在标签类 keyword 字段上做 match 查询。** 标签 `"Java"` 用 `term` 查不到（大小写）说明你没有对 keyword 查询用对姿势——`term` 不分析、大小写敏感。实际写入的值是 `"java"` 还是 `"Java"` 决定了你能不能搜到。数据清洗和 mapping 是同一枚硬币的两面。

---

**本节提要（延伸学习）**

- **核心概念**：索引设计流程（需求→字段→类型→参数）；`dynamic: strict`；`copy_to` vs `multi_match`；date 多格式；keyword 数组
- **拓展提问提示词**

> 主题：Elasticsearch 7.17 索引设计实战——以文章搜索为例。核心概念：索引设计流程、dynamic: strict、text+keyword 多字段、date 多格式、copy_to vs multi_match、keyword 数组。请拓展：1）如果 `tags` 字段的值可能超过 10000 个不同标签，对 `terms` 聚合有什么影响以及如何缓解？2）`date` 字段如果只存储精确到天的数据，用什么 format 最省空间？3）在保持 `dynamic: strict` 的前提下，如何在不重建索引的情况下临时添加一个新字段做原型验证？

---

## 推荐阅读

> 说明在前、链接行仅 URL（复制时不会夹带额外字符）。

- **Elasticsearch Reference 7.17 — Mapping**
  - 关联主题：映射总览、字段类型、动态映射、显式映射、运行时字段。
  - https://www.elastic.co/guide/en/elasticsearch/reference/7.17/mapping.html
  - 检索：`Elasticsearch 7.17 mapping dynamic explicit runtime`

- **Elasticsearch Reference 7.17 — Explicit mapping**
  - 关联主题：update mapping API 限制、字段类型不可变规则。
  - https://www.elastic.co/guide/en/elasticsearch/reference/7.17/explicit-mapping.html
  - 检索：`Elasticsearch 7.17 explicit mapping update API`

- **Elasticsearch Reference 7.17 — Text type**
  - 关联主题：text 字段参数、分词器、倒排索引行为。
  - https://www.elastic.co/guide/en/elasticsearch/reference/7.17/text.html
  - 检索：`Elasticsearch 7.17 text type mapping fields`

- **Elasticsearch Reference 7.17 — Text analysis**
  - 关联主题：分析器管道、内置分析器、自定义分析器完整组件。
  - https://www.elastic.co/guide/en/elasticsearch/reference/7.17/analysis.html
  - 检索：`Elasticsearch 7.17 analysis analyzer tokenizer filter`

- **Elasticsearch Reference 7.17 — Mapping parameters (copy_to / norms / doc_values)**
  - 关联主题：copy_to 多字段汇聚、norms 评分、doc_values 列式存储。
  - https://www.elastic.co/guide/en/elasticsearch/reference/7.17/mapping-params.html
  - 检索：`Elasticsearch 7.17 mapping parameters copy_to doc_values norms`
