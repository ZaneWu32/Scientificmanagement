# 需求洞察与研究洞察 RAG 轻量实施计划

## 1. 文档说明

| 项目 | 内容 |
| :--- | :--- |
| 文档名称 | 需求洞察与研究洞察 RAG 轻量实施计划 |
| 版本 | v0.3 |
| 日期 | 2026-05-19 |
| 面向对象 | 前端开发、后端开发、测试、项目负责人 |
| 文档定位 | 用于指导桐乡项目一期 AI 能力最小可落地实施，避免过度工程化 |
| 适用范围 | 成果库数千至上万条、一期开通 3-5 个白名单数据源、小团队交付 |

## 2. 核心结论

上一版方案按中大型平台标准设计，包含 Elasticsearch 向量检索、Index Alias、Redis 分布式锁、复杂任务进度、MinIO 双 Bucket 等能力。对于桐乡一期项目，这些设计明显超出实际交付需要。

本次修订采用轻量方案：

- 一期必须先做检索召回层，LLM 不能扫描全量成果库。
- 一期需求匹配采用 `成果级检索召回 + LLM 重排与解释`。
- 一期优先使用 MySQL FULLTEXT 或等价轻量检索；如压测不满足，再采用轻量 Elasticsearch 文本索引。
- 一期不做附件全文向量切片，不做 ES dense_vector，不做 Index Alias/Reindex 标准流程。
- 一期采集调度采用 `@Scheduled + 数据源状态字段`。
- 一期 rebuild 采用 `@Async + MySQL task 记录`。
- 一期文件存储继续沿用现有 local 或单一 MinIO bucket/prefix，不做复杂隔离。
- 一期研究洞察不与需求洞察并行重投入，先做轻量管理视图，待需求洞察稳定后增强。
- ES 向量检索、附件 chunk 级 RAG、Redis 分布式锁、Index Alias、复杂权限索引、MinIO 多 Bucket 统一作为二期/三期扩展。

一期核心原则：

**以跑通业务闭环为第一目标，基础设施选型以够用为标准，不以未来扩展性为理由引入当前用不上的复杂度。**

## 3. 项目实际约束

### 3.1 业务规模

| 维度 | 一期实际情况 | 设计影响 |
| :--- | :--- | :--- |
| 成果库规模 | 预计数千至上万条 | 必须先检索召回 Top N，再交给 LLM 匹配解释 |
| 数据源数量 | 3-5 个白名单来源 | `@Scheduled` 轮询足够 |
| 并发规模 | 管理员少量使用 | 不需要 Redis 分布式锁 |
| 文件规模 | 一期以成果附件和需求快照为主 | local 或单 bucket prefix 足够 |
| 研究洞察成熟度 | 依赖需求洞察和成果数据质量 | 不应与需求洞察同等投入 |
| 团队规模 | 小团队交付 | 优先减少中间件和运维复杂度 |

### 3.2 技术基线

当前代码已有：

- 前端需求洞察展示壳：`research-management-system/src/views/insights/DemandInsights.vue`
- 前端研究洞察展示壳：`research-management-system/src/views/admin/ResearchInsights.vue`
- 前端需求接口封装：`research-management-system/src/api/demand.ts`
- 前端研究洞察接口封装：`research-management-system/src/api/researchInsights.ts`
- 后端 LLM Chat 与 Embedding 封装：`achmanager-backend/src/main/java/com/achievement/client/LlmClient.java`
- 后端成果详情与权限查询：`achmanager-backend/src/main/java/com/achievement/service/impl/AchievementMainsServiceImpl.java`
- 后端附件文本提取：`achmanager-backend/src/main/java/com/achievement/utils/AttachmentContentExtractor.java`
- 后端 MinIO 存储实现：`achmanager-backend/src/main/java/com/achievement/service/impl/MinioFileStorageServiceImpl.java`

当前缺口：

- 缺少真实 `/demand/*` 后端接口。
- 缺少真实 `/research-insights/*` 后端接口。
- 缺少成果检索表和检索召回服务。
- 缺少需求池、匹配结果、数据源状态等 MySQL 表。
- 前端页面仍依赖 mock 数据。
- 需求采集、结构化、匹配还没有形成真实闭环。

## 4. 一期目标与边界

### 4.1 一期目标

一期只聚焦一条主线：

```text
白名单数据源
 -> 需求入池
 -> LLM 摘要/关键词/标签
 -> 成果级检索召回 Top 50-100
 -> LLM 重排并输出候选匹配、理由、证据片段
 -> 人工确认/跟进
```

必须完成：

- 数据源管理。
- 需求采集入池。
- 需求结构化。
- 需求与成果检索召回。
- 候选成果 LLM 重排与解释。
- 匹配理由和证据片段。
- 人工确认与状态流转。
- 轻量运营指标。

### 4.2 一期研究洞察目标

研究洞察一期定位为轻量增值视图，不做复杂 RAG 体系。

一期只做：

- 基于成果关键词、成果类型、年份、部门的主题聚合。
- 基于需求池关键词的外部需求信号统计。
- 管理摘要可由 LLM 根据聚合结果生成。
- 主题详情展示内部代表成果和外部需求信号。

暂不做：

- 向量化主题聚类。
- 复杂关系网络。
- ES 证据检索。
- 自动学科布局建议。
- 高复杂度趋势模型。

### 4.3 一期不做清单

| 能力 | 一期处理 | 后续触发条件 |
| :--- | :--- | :--- |
| 检索召回层 | 必须引入 | 成果库数千至上万条，不能让 LLM 扫全库 |
| MySQL FULLTEXT | 一期优先方案 | 中文分词、召回质量或性能不足时换轻量 ES |
| 轻量 Elasticsearch 文本索引 | 一期备选方案 | MySQL FULLTEXT 压测不达标时采用 |
| Elasticsearch 向量检索 | 不引入 | 需要语义召回且关键词召回明显不足 |
| BM25 + 向量混合检索 | 不引入 | 已引入 ES 且匹配质量仍不足 |
| Index Alias + Reindex | 不引入 | 使用 ES 后再考虑 |
| Redis 分布式锁 | 不引入 | 多实例部署且出现重复采集 |
| ThreadPoolTaskScheduler 动态调度 | 不引入 | 数据源数量明显增加，需要动态频率 |
| MinIO 多 Bucket + IAM Policy | 不引入 | 文件权限隔离成为实际问题 |
| Strapi 新增主题词典内容类型 | 不引入 | 主题词典需要运营人员频繁后台维护 |
| rebuild 进度轮询 | 不引入 | rebuild 超过 5 分钟或需要多人并发操作 |
| 附件全文切片 | 不作为主路径 | 需要引用附件具体段落作为证据 |
| 通用知识库问答 | 不引入 | 甲方明确要求对话式问答 |

## 5. 一期推荐架构

### 5.1 组件职责

| 组件 | 一期职责 |
| :--- | :--- |
| Vue 前端 | 展示需求池、匹配结果、跟进状态、轻量研究洞察 |
| Spring Boot | 业务接口、采集调度、LLM 调用、匹配编排、权限控制 |
| MySQL | 需求、数据源、匹配结果、任务状态、研究主题结果、成果检索表 |
| Strapi | 继续承载成果、动态字段、附件关系 |
| local/MinIO | 文件和需求原始快照存储 |
| MySQL FULLTEXT/轻量 ES | 成果级检索召回 Top N |
| LLM | 摘要、标签、候选成果重排、匹配理由、管理摘要 |

### 5.2 一期数据流

```mermaid
flowchart LR
    A["白名单数据源"] --> B["@Scheduled 定时采集"]
    B --> C["需求原文入库"]
    C --> D["LLM 摘要/关键词/标签"]
    D --> E["成果级检索召回 Top N"]
    E --> F["LLM 重排与解释"]
    F --> G["保存 demand_matches"]
    G --> H["前端展示"]
    H --> I["人工确认/跟进"]
```

### 5.3 一期为什么不做复杂 ES 和附件切片

成果库数千至上万条时，必须做检索召回，但不等于必须上完整 RAG 基础设施。

一期要避免的是以下复杂度：

- 部署成本。
- 索引同步成本。
- Mapping 维护成本。
- 权限过滤复杂度。
- 数据一致性问题。
- 测试和运维成本。

因此一期推荐先做“成果级检索”，即每条成果生成一条检索文本，不对附件全文做默认切片。

成果级检索文本包括：

- 成果标题。
- 成果摘要。
- 成果关键词。
- 成果类型。
- 关键动态字段。
- 项目名称。

附件全文不作为一期主路径。只有在摘要质量明显不足、需要引用具体段落作为证据时，再做离线切片。

## 6. 一期匹配方案

### 6.1 核心思路

成果库达到数千至上万条后，不能让 LLM 直接读取全库。正确流程是：

```text
需求文本
 -> 检索召回 Top 50-100 成果
 -> LLM 只处理候选成果
 -> 输出 Top N 匹配结果、理由、证据片段
```

这仍然是轻量方案，因为一期只做“成果级检索”，不做“附件 chunk 级 RAG”。

### 6.2 成果级检索文本

建议新增一张成果检索表，将 Strapi/MySQL 中分散的成果字段整理成一条搜索文档。

每条成果一条记录，字段来源包括：

- 成果标题。
- 成果类型。
- 成果摘要。
- 成果关键词。
- 年份。
- 项目名称。
- 负责人或创建者。
- 重要动态字段。

不默认纳入：

- 附件全文。
- PDF/Word 长文本全文。
- 过程文档全文。

这样可以避免一开始就做切片、向量索引和复杂证据定位。

### 6.3 首选方案：MySQL FULLTEXT + LLM 重排

适用条件：

- 成果规模为数千至上万条。
- 主要匹配依据是标题、摘要、关键词、成果类型和核心动态字段。
- 一期团队希望减少中间件部署成本。

流程：

```text
需求标题 + 摘要 + 关键词
 -> MySQL FULLTEXT 召回 Top 100
 -> 规则加权排序 Top 50
 -> LLM 重排并生成解释
 -> 保存 demand_matches
```

建议召回字段：

- `title`
- `summary`
- `keywords_text`
- `type_name`
- `project_name`
- `search_text`

中文检索注意：

- MySQL 中文 FULLTEXT 需确认部署环境是否支持 `ngram` parser。
- 如果不能使用 ngram，可在 `keywords_text` 和 `search_text` 中写入后端标准化后的关键词串，降低中文分词依赖。
- 召回服务必须做压测，确保 Top 100 召回在可接受时间内完成。

规则加权建议：

| 信号 | 加权建议 |
| :--- | :--- |
| 标题命中 | 高 |
| 关键词命中 | 高 |
| 摘要命中 | 中 |
| 成果类型相关 | 中 |
| 年份较新 | 低 |
| 同地区或同部门 | 低 |

LLM 输入控制：

- 单次最多传 50 条候选成果。
- 每条成果只传标题、类型、摘要、关键词、负责人、年份。
- 若候选成果摘要过长，后端先截断。
- LLM 不接触全量成果库。

LLM 输出结构：

```json
{
  "matches": [
    {
      "resultId": "achievementDocumentId",
      "resultTitle": "成果标题",
      "matchScore": 0.86,
      "reason": "匹配理由",
      "sourceSnippet": "证据片段",
      "fitTags": ["关键词重合", "场景相近"]
    }
  ]
}
```

### 6.4 备选方案：轻量 Elasticsearch 文本索引

如果 MySQL FULLTEXT 在中文分词、召回质量或响应时间上不满足要求，可以在一期采用轻量 ES，但仅限文本检索。

轻量 ES 一期边界：

- 单一索引。
- 每条成果一条文档。
- 不做 dense_vector。
- 不做附件全文切片。
- 不做 Index Alias。
- 不做复杂 Reindex 流程。
- 不做 Redis 锁。
- 不做实时进度轮询。

ES 文档字段：

| 字段 | 说明 |
| :--- | :--- |
| achievementDocId | 成果 documentId |
| title | 标题 |
| summary | 摘要 |
| keywords | 关键词 |
| typeName | 成果类型 |
| projectName | 项目名称 |
| year | 年份 |
| searchText | 拼接后的检索文本 |
| visibilityRange | 可见范围 |
| updatedAt | 更新时间 |

轻量 ES 只解决“快速召回候选成果”问题，不承担业务状态存储。

### 6.5 附件切片边界

一期不做附件全文默认切片。

不切片的数据：

- 成果标题。
- 成果摘要。
- 成果关键词。
- 成果类型。
- 核心动态字段。

可选轻量处理：

- 当成果摘要缺失时，提取附件前 3000-5000 字生成一个补充摘要。
- 补充摘要写回成果检索表，仍然按成果级检索处理。

需要正式切片的触发条件：

- 匹配理由必须引用附件具体段落。
- 甲方要求全文证据定位。
- 附件正文是主要知识来源，摘要字段明显不足。
- LLM 上下文无法容纳候选成果摘要。
- 后续要做通用知识库问答。

正式切片应作为二期能力，采用离线解析、离线分块、离线索引，不在用户点击匹配时现场切片。

## 7. 一期数据表设计

### 7.1 `achievement_search_docs`

成果检索表。用于把成果主表、动态字段和必要的展示字段整理成一条可检索文档，支撑 MySQL FULLTEXT 或轻量 ES 同步。

| 字段 | 说明 |
| :--- | :--- |
| id | 主键 |
| achievement_doc_id | 成果 documentId |
| title | 成果标题 |
| type_name | 成果类型名称 |
| type_code | 成果类型编码 |
| summary | 成果摘要 |
| keywords_text | 关键词拼接文本 |
| project_name | 项目名称 |
| year | 年份 |
| owner_name | 负责人或创建者 |
| visibility_range | 可见范围 |
| search_text | 标题、摘要、关键词、动态字段拼接后的检索文本 |
| search_hash | 检索文本 hash |
| indexed_at | 最近同步时间 |
| created_at | 创建时间 |
| updated_at | 更新时间 |
| is_delete | 逻辑删除 |

建议索引：

- `achievement_doc_id` 唯一索引。
- `type_code`、`year`、`visibility_range` 普通索引。
- `title`、`summary`、`keywords_text`、`search_text` 建 FULLTEXT 索引。

同步方式：

- 成果新增、更新、审核通过后同步单条。
- 每晚低峰期可全量刷新一次。
- 一期不需要复杂索引版本和 alias。

### 7.2 `demand_sources`

数据源配置表。

| 字段 | 说明 |
| :--- | :--- |
| id | 主键 |
| name | 数据源名称 |
| type | `rss`、`html`、`api`、`file` |
| industry | 行业 |
| region | 地域 |
| base_url | 地址 |
| frequency_hours | 采集频率 |
| enabled | 是否启用 |
| status | `healthy`、`warning`、`error`、`idle` |
| last_run_at | 最近运行时间 |
| last_success_at | 最近成功时间 |
| failure_reason | 失败原因 |
| created_at | 创建时间 |
| updated_at | 更新时间 |
| is_delete | 逻辑删除 |

一期不需要：

- 复杂认证模型。
- 加密 credentials 表。
- 多租户隔离。
- 动态调度表达式。

如确实需要 API Key 或 Cookie，可先用一个 `credentials_json` 字段保存，并做好脱敏展示。

### 7.3 `demand_items`

需求池主表。

| 字段 | 说明 |
| :--- | :--- |
| id | 主键 |
| title | 需求标题 |
| raw_content | 原始正文 |
| summary | 原始摘要或抽取摘要 |
| llm_summary | LLM 摘要 |
| keywords_json | 关键词 JSON |
| tags_json | 标签 JSON |
| industry | 行业 |
| region | 地域 |
| source_id | 数据源 ID |
| source_site | 来源站点 |
| source_url | 原始链接 |
| captured_at | 抓取时间 |
| confidence | 可信度 |
| best_match_score | 最佳匹配分 |
| status | 需求状态 |
| content_hash | 去重 hash |
| created_at | 创建时间 |
| updated_at | 更新时间 |
| is_delete | 逻辑删除 |

状态建议：

- `new`：新入池。
- `reviewing`：研判中。
- `matched`：已生成匹配结果。
- `in_follow_up`：跟进中。
- `invalid`：无效。
- `archived`：归档。

### 7.4 `demand_matches`

需求匹配结果表。

| 字段 | 说明 |
| :--- | :--- |
| id | 主键 |
| demand_id | 需求 ID |
| achievement_doc_id | 成果 documentId |
| result_title | 成果标题快照 |
| result_type | 成果类型快照 |
| owner | 成果负责人 |
| match_score | 匹配分 |
| reason | 匹配理由 |
| source_snippet | 证据片段 |
| fit_tags_json | 标签 JSON |
| confirm_status | `pending`、`confirmed`、`rejected` |
| confirmed_by | 确认人 |
| confirmed_at | 确认时间 |
| created_at | 创建时间 |
| updated_at | 更新时间 |

### 7.5 `demand_follow_ups`

跟进记录表。

| 字段 | 说明 |
| :--- | :--- |
| id | 主键 |
| demand_id | 需求 ID |
| owner_id | 跟进人 ID |
| owner_name | 跟进人名称 |
| status | 跟进状态 |
| next_action | 下一步动作 |
| due_at | 截止时间 |
| note | 备注 |
| created_at | 创建时间 |
| updated_at | 更新时间 |

### 7.6 `system_tasks`

简单任务表，用于记录采集、重匹配、研究洞察生成等异步任务。

| 字段 | 说明 |
| :--- | :--- |
| id | 主键 |
| task_type | `crawl`、`rematch`、`research_rebuild` |
| status | `pending`、`running`、`success`、`failed` |
| target_id | 目标 ID，可为空 |
| message | 成功或失败说明 |
| started_at | 开始时间 |
| finished_at | 完成时间 |
| created_at | 创建时间 |

一期不做精细进度百分比。任务通常较短，记录状态和错误原因即可。

### 7.7 `research_topics`

轻量研究主题表。

| 字段 | 说明 |
| :--- | :--- |
| id | 主键 |
| name | 主题名称 |
| stage | `emerging`、`strong`、`cross`、`gap` |
| keywords_json | 关键词 |
| description | 主题说明 |
| external_heat | 外部热度 |
| internal_strength | 内部基础 |
| growth_rate_12m | 近 12 月增速 |
| recommendation | 建议动作 |
| explanation | 解释依据 |
| internal_results_json | 内部代表成果 |
| external_signals_json | 外部需求信号 |
| trend_json | 趋势数组 |
| generated_at | 生成时间 |

一期先用 JSON 字段保存证据列表，减少表数量。二期再拆 `research_topic_evidence`。

## 8. 后端实施步骤

### 8.1 新增 LLM 用途

文件：

- `achmanager-backend/src/main/java/com/achievement/constant/LlmUsage.java`

建议新增：

```java
DEMAND("demand"),
RESEARCH_INSIGHT("researchInsight")
```

一期可以暂不新增独立 embedding usage，因为默认不做向量检索。

### 8.2 实现需求数据源管理

新增：

- `DemandSourceController`
- `DemandSourceService`
- `DemandSourceMapper`
- `DemandSource` 实体

接口：

| 方法 | 路径 | 说明 |
| :--- | :--- | :--- |
| GET | `/system/crawler-sources` | 查询数据源 |
| POST | `/system/crawler-sources` | 新增数据源 |
| PUT | `/system/crawler-sources/{id}` | 更新数据源 |
| DELETE | `/system/crawler-sources/{id}` | 删除数据源 |
| POST | `/system/crawler-sources/{id}/test` | 测试数据源 |

前端已有对应封装：

- `research-management-system/src/api/system.ts`

### 8.3 实现简单采集调度

一期采用固定频率扫描。

示例：

```java
@Scheduled(fixedDelay = 3600000)
public void runCrawlerTasks() {
    demandSourceRepository.findAllByEnabled(true)
        .forEach(this::crawlIfDue);
}
```

`crawlIfDue` 逻辑：

- 判断 `last_run_at + frequency_hours` 是否到期。
- 到期则执行采集。
- 成功更新 `last_success_at` 和 `status=healthy`。
- 失败更新 `status=error` 和 `failure_reason`。

一期支持的数据源类型：

- RSS。
- 简单 HTML 列表页。
- 简单 API。

不支持：

- 强登录态。
- 强反爬。
- 验证码。
- 复杂 JS 渲染页面。

### 8.4 实现需求入池与去重

新增：

- `DemandController`
- `DemandService`
- `DemandMapper`
- `DemandItem` 实体

去重方式：

- 标题 + 来源 URL hash。
- 或标题 + 正文前 500 字 hash。

入池后状态：

- 初始为 `new`。

### 8.5 实现需求结构化

新增：

- `DemandStructuringService`

输入：

- 需求标题。
- 需求正文。
- 来源站点。
- 行业、地域默认值。

LLM 输出：

```json
{
  "summary": "简短摘要",
  "keywords": ["关键词1", "关键词2"],
  "tags": ["标签1", "标签2"],
  "industry": "行业",
  "region": "地域",
  "confidence": 0.86
}
```

要求：

- 后端校验 JSON。
- LLM 失败时保留原始需求，不阻塞入池。
- 失败需求可后续人工重试结构化。

### 8.6 实现成果检索表同步

新增：

- `AchievementSearchDoc` 实体。
- `AchievementSearchDocMapper`。
- `AchievementSearchSyncService`。

同步来源：

- `documentId`
- `title`
- `summary`
- `keywords`
- `typeName`
- `year`
- `projectName`
- `creatorName`
- 关键动态字段

实现要求：

- 成果新增、更新、审核通过后同步单条检索记录。
- 每晚低峰期可全量刷新一次。
- 生成 `search_text`，用于 FULLTEXT 检索。
- 同步失败记录日志，不影响成果主流程。
- 如果采用轻量 ES，则由该表同步到 ES 单一索引。

### 8.7 实现成果级召回

新增：

- `AchievementRecallService`

召回策略：

- 优先查已审核、已发布成果。
- 使用 MySQL FULLTEXT 查询 `achievement_search_docs`。
- 按标题命中、关键词命中、摘要命中、年份、类型等规则加权。
- 召回 Top 100 后截断为 Top 50 交给 LLM。

如果没有命中：

- 取近 2-3 年已审核成果中摘要较完整的前 30 条作为兜底。
- 同时降低匹配置信度。

如果 MySQL FULLTEXT 压测不达标：

- 保持业务表不变。
- 将 `achievement_search_docs` 同步到轻量 ES 单一索引。
- 仍按成果级文档召回，不做附件 chunk。

### 8.8 实现 LLM 重排与解释

新增：

- `DemandMatchService`

流程：

```text
读取需求
 -> 成果级召回 Top 50
 -> 拼接候选成果摘要
 -> 调用 LLM 重排和解释
 -> 解析 Top N
 -> 保存 demand_matches
 -> 更新 demand_items.best_match_score/status
```

接口：

| 方法 | 路径 | 说明 |
| :--- | :--- | :--- |
| GET | `/demand` | 需求列表 |
| GET | `/demand/{id}` | 需求详情 |
| POST | `/demand/{id}/rematch` | 重新匹配 |
| PATCH | `/demand/{id}/status` | 更新状态 |
| POST | `/demand/{id}/confirm-match` | 确认匹配 |
| GET | `/demand/sources` | 数据源健康状态 |

前端已有对应封装：

- `research-management-system/src/api/demand.ts`

### 8.9 实现轻量研究洞察

新增：

- `ResearchInsightsController`
- `ResearchInsightsService`

一期生成方式：

- 从成果表统计关键词。
- 从需求表统计关键词。
- 主题词可以先写在 YAML 或 Java 常量中。
- 根据关键词映射聚合主题。
- 用 LLM 生成描述、解释和建议。

主题词配置建议：

```yaml
research-themes:
  - name: 智能制造
    keywords: [机器视觉, 缺陷识别, 边缘计算, 工业质检]
  - name: 储能安全
    keywords: [储能电池, 健康评估, 安全预警, 寿命预测]
```

不建议一期在 Strapi 新建主题词典内容类型。等甲方需要运营人员频繁维护主题词时再做后台化。

接口：

| 方法 | 路径 | 说明 |
| :--- | :--- | :--- |
| GET | `/research-insights/topics` | 获取主题池 |
| GET | `/research-insights/executive-insights` | 获取管理摘要 |
| GET | `/research-insights/topics/{id}/trend` | 获取主题趋势 |
| POST | `/research-insights/rebuild` | 异步重建研究洞察 |

`rebuild` 实现：

```java
@Async
public void rebuildResearchInsights() {
    // 写 system_tasks running
    // 聚合成果与需求
    // 调用 LLM 生成摘要
    // 保存 research_topics
    // 写 system_tasks success/failed
}
```

一期不需要 Redis TTL 和进度轮询。

## 9. 前端实施步骤

### 9.1 需求洞察页面接真实接口

文件：

- `research-management-system/src/views/insights/DemandInsights.vue`

改造：

- 移除 `@/mocks/insightShowcase` 的 `demandItems` 和 `demandSources`。
- 页面初始化调用 `getDemands()`。
- 选中需求调用 `getDemandDetail(id)`。
- 数据源状态调用 `getDemandSources()`。
- 增加重匹配按钮，调用 `rematchDemand(id)`。
- 增加确认匹配按钮，调用 `confirmMatch(demandId, resultId)`。
- 增加状态更新按钮，调用 `updateDemandStatus(...)`。
- 去掉 `Mock 演示数据` 标签。

需要补充状态：

- `loadingDemands`
- `loadingDetail`
- `rematching`
- `updatingStatus`
- `confirmingMatchId`

### 9.2 系统设置数据源接真实接口

文件：

- `research-management-system/src/views/admin/SystemSettings.vue`
- `research-management-system/src/api/system.ts`

改造：

- 接入数据源列表。
- 接入新增、编辑、删除。
- 接入启停。
- 接入测试连接。
- 展示 `lastRunAt`、`lastSuccessAt`、`status`、`failureReason`。

### 9.3 研究洞察页面接真实接口

文件：

- `research-management-system/src/views/admin/ResearchInsights.vue`
- `research-management-system/src/api/researchInsights.ts`

改造：

- 移除 `researchTopics` mock。
- 调用 `getResearchTopics()`。
- 调用 `getExecutiveInsights()`。
- 调用 `getTopicTrend(topicId)`。
- 内部代表成果支持跳转成果详情。
- 外部需求信号支持跳转需求详情。
- 管理员可触发 `/research-insights/rebuild`。
- 去掉 `Mock 演示数据` 标签。

### 9.4 前端展示边界

页面文案需要明确：

- 匹配结果是推荐候选，不是自动决策。
- 管理摘要是辅助研判，不替代资源配置决策。
- 跟进状态必须由人工确认后进入。

## 10. 文件存储策略

### 10.1 一期策略

一期不强制切换 MinIO。可选：

#### 方案 A：保持 local

适合：

- 开发测试。
- 文件量很小。
- 单机部署。

#### 方案 B：单 Bucket + prefix

适合：

- 已有 MinIO 环境。
- 希望和后续部署方向保持一致。

路径建议：

```text
attachments/...
rag/snapshots/...
rag/parsed/...
```

一期不做：

- 双 bucket。
- IAM Policy 细粒度隔离。
- 生命周期 TTL。
- 冷热分层。

### 10.2 后续升级触发条件

满足以下条件时再升级：

- 文件量明显增长。
- 多系统共用 MinIO。
- 不同文件类型需要独立权限策略。
- 需要自动清理临时文件。

## 11. 权限方案

### 11.1 一期原则

- 所有业务接口走 Spring Boot 鉴权。
- 需求洞察管理能力仅管理员可操作。
- 普通科研人员可浏览允许范围内的需求或成果。
- 成果详情继续复用现有权限判断。

### 11.2 匹配结果权限

需求匹配由管理员使用时：

- 可以查看候选成果标题、类型、摘要级信息。
- 点击成果详情时仍走现有成果详情接口。
- 如果无全文权限，不展示附件全文内容。

### 11.3 研究洞察权限

- 管理员和管理层角色可访问研究洞察。
- 主题中的代表成果跳转时仍由成果详情接口二次鉴权。
- 管理摘要不直接输出受限附件全文。

## 12. 排期建议

### 第 1 阶段：需求洞察主线，约 2 周

目标：

- 真实需求池可用。
- 真实需求匹配可用。
- 前端需求洞察脱离 mock。

后端任务：

- 建 `achievement_search_docs`、`demand_sources`、`demand_items`、`demand_matches`、`demand_follow_ups`。
- 实现数据源 CRUD。
- 实现 `@Scheduled` 采集。
- 实现需求结构化。
- 实现成果检索表同步。
- 实现 MySQL FULLTEXT 召回 Top N。
- 实现 LLM 重排与解释。
- 实现 `/demand/*` 接口。

前端任务：

- 需求列表接真实接口。
- 需求详情接真实接口。
- 重匹配、确认、状态更新接真实接口。
- 数据源健康状态接真实接口。

验收：

- 能新增一个数据源。
- 能采集或手动导入需求。
- 能生成摘要和关键词。
- 能返回候选成果、匹配分、理由、证据片段。
- 能人工确认并进入跟进。

### 第 2 阶段：数据源与运营看板完善，约 1 周

目标：

- 白名单数据源稳定可管理。
- 需求池有基础运营指标。

任务：

- 数据源测试连接。
- 数据源失败原因展示。
- 去重优化。
- 需求状态统计。
- 跟进数量、有效需求数、匹配数量展示。

验收：

- 数据源异常可见。
- 去重后不会反复入池。
- 管理员能看到入池、已匹配、跟进中、无效数量。

### 第 3 阶段：轻量研究洞察，约 1-2 周

前置条件：

- 需求洞察已有一定真实数据。
- 成果关键词和摘要质量基本可用。

目标：

- 研究洞察脱离 mock。
- 能形成主题池和管理摘要。

任务：

- 建 `research_topics`。
- 用 YAML 或 Java 常量维护主题词。
- 聚合成果关键词和需求关键词。
- 生成主题描述、解释、建议。
- 接入研究洞察前端。

验收：

- 能展示主题池。
- 能展示内部代表成果。
- 能展示外部需求信号。
- 能展示轻量趋势。
- 能生成管理摘要。

## 13. 测试重点

### 13.1 后端测试

- 数据源 CRUD。
- 定时采集是否按频率执行。
- 采集失败是否记录失败原因。
- 需求去重是否有效。
- LLM 结构化失败是否降级。
- LLM 匹配输出 JSON 是否可解析。
- 重匹配是否覆盖旧结果或生成新版本。
- 状态流转是否正确。
- 成果详情权限是否仍然生效。

### 13.2 前端测试

- 需求列表 loading。
- 空需求池展示。
- 需求详情展示。
- 重匹配按钮防重复点击。
- 匹配确认后状态刷新。
- 数据源异常展示。
- 研究主题筛选。
- 主题详情展示。
- mock 标签是否移除。

### 13.3 验收测试

需求洞察验收脚本：

1. 新增一个白名单数据源。
2. 手动或定时采集需求。
3. 查看需求池。
4. 打开需求详情。
5. 查看摘要、关键词、标签。
6. 点击重新匹配。
7. 查看候选成果、匹配分、理由、证据片段。
8. 确认一个匹配。
9. 将需求状态改为跟进中。

研究洞察验收脚本：

1. 管理员触发研究洞察重建。
2. 查看主题池。
3. 打开某个主题。
4. 查看内部代表成果。
5. 查看外部需求信号。
6. 查看趋势和建议动作。

## 14. 后续升级路径

### 14.1 从 MySQL FULLTEXT 升级到轻量 ES 的触发条件

满足以下任一条件再考虑轻量 ES 文本索引：

- 需求匹配接口响应超过 5 秒。
- MySQL FULLTEXT 召回质量明显不足。
- MySQL FULLTEXT 中文分词效果明显不满足业务。
- 成果检索字段和过滤条件明显复杂化。

轻量 ES 仍保持：

- 单一索引。
- 每条成果一条文档。
- 不做 dense_vector。
- 不做附件全文切片。
- 不做 Index Alias。

### 14.2 引入附件切片和向量检索的触发条件

满足以下任一条件再考虑附件 chunk 和向量检索：

- 需要跨附件全文检索。
- 需要引用附件具体段落作为证据。
- 成果摘要和关键词长期不足，无法支撑匹配。
- 需要复杂权限过滤和高亮证据片段。
- 甲方明确要求通用知识库问答。

### 14.3 引入 Redis 锁的触发条件

满足以下任一条件再考虑：

- 后端多实例部署。
- 同一数据源重复采集。
- 采集任务出现并发写冲突。

### 14.4 引入复杂任务进度的触发条件

满足以下任一条件再考虑：

- rebuild 超过 5 分钟。
- 用户需要实时看到百分比。
- 多人同时触发 rebuild。

### 14.5 引入 Strapi 主题词典的触发条件

满足以下任一条件再考虑：

- 主题词需要甲方运营人员频繁调整。
- 主题词存在审批流程。
- 主题词需要版本管理。

## 15. 任务拆分清单

### 15.1 后端任务

| 编号 | 任务 | 优先级 |
| :--- | :--- | :--- |
| BE-01 | 新增成果检索表和需求相关 MySQL 表 | P0 |
| BE-02 | 实现数据源 CRUD | P0 |
| BE-03 | 实现 `@Scheduled` 采集调度 | P0 |
| BE-04 | 实现需求入池与去重 | P0 |
| BE-05 | 实现 LLM 需求结构化 | P0 |
| BE-06 | 实现成果检索表同步 | P0 |
| BE-07 | 实现 MySQL FULLTEXT 召回 | P0 |
| BE-08 | 实现 `/demand/*` 接口 | P0 |
| BE-09 | 实现人工确认与状态流转 | P0 |
| BE-10 | 实现 LLM 重排与解释 | P0 |
| BE-11 | 新增 `system_tasks` 简单任务表 | P1 |
| BE-12 | 实现轻量研究主题生成 | P1 |
| BE-13 | 实现 `/research-insights/*` 接口 | P1 |

### 15.2 前端任务

| 编号 | 任务 | 优先级 |
| :--- | :--- | :--- |
| FE-01 | 需求洞察移除 mock 数据依赖 | P0 |
| FE-02 | 需求列表接真实接口 | P0 |
| FE-03 | 需求详情接真实接口 | P0 |
| FE-04 | 重匹配操作接真实接口 | P0 |
| FE-05 | 匹配确认和状态更新接真实接口 | P0 |
| FE-06 | 数据源健康状态接真实接口 | P0 |
| FE-07 | 系统设置数据源管理接真实接口 | P1 |
| FE-08 | 研究洞察移除 mock 数据依赖 | P1 |
| FE-09 | 主题池和管理摘要接真实接口 | P1 |
| FE-10 | loading、错误、空状态补齐 | P0 |

### 15.3 测试任务

| 编号 | 任务 | 优先级 |
| :--- | :--- | :--- |
| QA-01 | 需求洞察主流程测试 | P0 |
| QA-02 | 数据源采集测试 | P0 |
| QA-03 | LLM 输出异常测试 | P0 |
| QA-04 | 权限回归测试 | P0 |
| QA-05 | 研究洞察轻量视图测试 | P1 |

## 16. 最终建议

桐乡一期不应按中大型知识平台建设 RAG，而应按“成果转化辅助工具”建设最小闭环。

推荐执行顺序：

1. 先做需求洞察真实闭环。
2. 再完善数据源和运营指标。
3. 最后做轻量研究洞察。
4. 如果 MySQL FULLTEXT 压测不达标，再升级轻量 ES 文本索引。
5. 等业务确实需要附件证据或知识库问答时，再做切片、向量检索、Redis 锁、复杂 rebuild、MinIO 多 Bucket 等企业级能力。

一句话原则：

**一期用最少的新基础设施把业务跑通，二期再根据真实瓶颈升级技术架构。**
