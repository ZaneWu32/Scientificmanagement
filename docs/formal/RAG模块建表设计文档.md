# RAG 模块建表设计文档

> 版本：v1.0 | 日期：2026-05-19
> 数据库：`strapi`（与现有表同库，字符集统一为 `utf8mb4_unicode_ci`）
> 共 7 张新表，全部新建，不修改任何现有 Strapi 表

---

## 设计原则

1. **不建跨域外键**：新 RAG 表与现有 Strapi 表之间只用 `achievement_doc_id`（varchar）做软引用，不建 MySQL FOREIGN KEY，避免 Strapi 自动维护时产生外键冲突。
2. **字段类型对齐**：`year` 字段使用 `VARCHAR(20)` 与 `achievement_mains.year`（varchar(255)）对齐。
3. **逻辑删除统一**：沿用现有表的 `is_delete INT DEFAULT 0` 模式（0=正常，1=删除）。
4. **时间字段统一**：使用 `datetime(6)` 与现有表保持一致。
5. **FULLTEXT 说明**：`achievement_search_docs` 的中文全文索引依赖 ngram parser，需确认 MySQL 部署参数 `ft_min_word_len=1` 或 `ngram_token_size=2`。

---

## 表 1：`achievement_search_docs`（成果检索文档表）

### 用途

将 Strapi 中分散的成果字段（`achievement_mains`、`achievement_types`、`achievement_field_values`）整合为一条可检索文档，供 MySQL FULLTEXT 召回使用。每条成果对应一条记录。

### 同步来源

| 字段 | 来源 |
|------|------|
| `achievement_doc_id` | `achievement_mains.document_id` |
| `title` | `achievement_mains.title` |
| `summary` | `achievement_mains.summary`（超长时截断） |
| `keywords_text` | `achievement_mains.keywords`（JSON 数组，解析后空格拼接） |
| `authors_text` | `achievement_mains.authors`（JSON 数组，解析后拼接） |
| `project_name` | `achievement_mains.project_name` |
| `year` | `achievement_mains.year` |
| `visibility_range` | `achievement_mains.visibility_range` |
| `type_name`/`type_code` | 通过 `achievement_mains_achievement_type_id_lnk` → `achievement_types` |
| `search_text` | 后端拼接：标题 + 摘要 + 关键词 + 类型 + 项目名 + 动态字段重要值 |

### 同步触发时机

- 成果审核通过时（`achievement_status = 'APPROVED'`）触发单条同步
- 每晚低峰期全量刷新一次
- `search_hash` 用于判断内容是否变化，相同则跳过更新

### 字段定义

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|--------|------|--------|--------|------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | 主键 |
| `achievement_doc_id` | VARCHAR(255) | NO | - | 成果 documentId，唯一索引 |
| `title` | VARCHAR(500) | YES | NULL | 成果标题 |
| `type_name` | VARCHAR(100) | YES | NULL | 成果类型名称 |
| `type_code` | VARCHAR(50) | YES | NULL | 成果类型编码 |
| `summary` | TEXT | YES | NULL | 成果摘要（截断至 2000 字） |
| `keywords_text` | TEXT | YES | NULL | 关键词拼接，空格分隔 |
| `authors_text` | VARCHAR(500) | YES | NULL | 作者/负责人拼接 |
| `project_name` | VARCHAR(500) | YES | NULL | 项目名称 |
| `year` | VARCHAR(20) | YES | NULL | 年份（与原表类型一致） |
| `visibility_range` | VARCHAR(50) | YES | NULL | 可见范围 |
| `search_text` | LONGTEXT | YES | NULL | 全量拼接检索文本（FULLTEXT 主体） |
| `search_hash` | VARCHAR(64) | YES | NULL | search_text 的 MD5，用于增量判断 |
| `indexed_at` | datetime(6) | YES | NULL | 最近同步时间 |
| `created_at` | datetime(6) | YES | NULL | 创建时间 |
| `updated_at` | datetime(6) | YES | NULL | 更新时间 |
| `is_delete` | INT | YES | 0 | 逻辑删除（0=正常，1=删除） |

### 索引

| 索引名 | 类型 | 字段 | 说明 |
|--------|------|------|------|
| `PRIMARY` | PRIMARY | `id` | 主键 |
| `uk_achievement_doc_id` | UNIQUE | `achievement_doc_id` | 成果唯一标识 |
| `idx_type_code` | 普通 | `type_code` | 按类型召回过滤 |
| `idx_year` | 普通 | `year` | 按年份过滤 |
| `idx_visibility_range` | 普通 | `visibility_range` | 按可见范围过滤 |
| `idx_indexed_at` | 普通 | `indexed_at` | 同步时间查询 |
| `ft_search` | FULLTEXT WITH PARSER ngram | `title, summary, keywords_text, search_text` | 中文全文检索核心索引 |

---

## 表 2：`demand_sources`（需求数据源配置表）

### 用途

白名单数据源管理，`@Scheduled` 采集调度根据此表决定采集哪些数据源、多久采集一次。

### 字段定义

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|--------|------|--------|--------|------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | 主键 |
| `name` | VARCHAR(200) | NO | - | 数据源名称 |
| `type` | VARCHAR(20) | NO | - | 类型：`rss` / `html` / `api` / `file` |
| `industry` | VARCHAR(100) | YES | NULL | 所属行业 |
| `region` | VARCHAR(100) | YES | NULL | 所属地域 |
| `base_url` | VARCHAR(1000) | NO | - | 采集地址 |
| `credentials_json` | TEXT | YES | NULL | 认证信息 JSON（API Key 等，展示时脱敏） |
| `frequency_hours` | INT | NO | 24 | 采集频率（小时） |
| `enabled` | TINYINT(1) | NO | 1 | 是否启用 |
| `status` | VARCHAR(20) | NO | 'idle' | `healthy` / `warning` / `error` / `idle` |
| `last_run_at` | datetime(6) | YES | NULL | 最近运行时间 |
| `last_success_at` | datetime(6) | YES | NULL | 最近成功时间 |
| `failure_reason` | VARCHAR(500) | YES | NULL | 最近失败原因 |
| `created_at` | datetime(6) | YES | NULL | 创建时间 |
| `updated_at` | datetime(6) | YES | NULL | 更新时间 |
| `is_delete` | INT | YES | 0 | 逻辑删除 |

### 索引

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | PRIMARY | `id` |
| `idx_enabled_status` | 普通 | `enabled, status` |

---

## 表 3：`demand_items`（需求池主表）

### 用途

存储从数据源采集的原始需求及 LLM 结构化结果，是匹配主链路的起点。`content_hash` 唯一索引用于自动去重。

### 字段定义

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|--------|------|--------|--------|------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | 主键 |
| `title` | VARCHAR(500) | NO | - | 需求标题 |
| `raw_content` | LONGTEXT | YES | NULL | 原始正文 |
| `summary` | TEXT | YES | NULL | 原始摘要（来源站点提供） |
| `llm_summary` | TEXT | YES | NULL | LLM 生成的摘要 |
| `keywords_json` | TEXT | YES | NULL | LLM 提取的关键词（JSON 数组） |
| `tags_json` | TEXT | YES | NULL | LLM 提取的标签（JSON 数组） |
| `industry` | VARCHAR(100) | YES | NULL | 行业（LLM 识别或数据源默认值） |
| `region` | VARCHAR(100) | YES | NULL | 地域（LLM 识别或数据源默认值） |
| `source_id` | BIGINT UNSIGNED | YES | NULL | 数据源 ID（软关联 demand_sources.id） |
| `source_site` | VARCHAR(200) | YES | NULL | 来源站点名称 |
| `source_url` | VARCHAR(1000) | YES | NULL | 原始链接 |
| `captured_at` | datetime(6) | YES | NULL | 抓取时间 |
| `confidence` | DECIMAL(4,3) | YES | NULL | LLM 结构化置信度（0.000-1.000） |
| `best_match_score` | DECIMAL(4,3) | YES | NULL | 最佳匹配分（匹配后回写） |
| `status` | VARCHAR(30) | NO | 'new' | `new` / `reviewing` / `matched` / `in_follow_up` / `invalid` / `archived` |
| `content_hash` | VARCHAR(64) | YES | NULL | 去重 hash（title+url 或正文前 500 字 MD5） |
| `created_at` | datetime(6) | YES | NULL | 创建时间 |
| `updated_at` | datetime(6) | YES | NULL | 更新时间 |
| `is_delete` | INT | YES | 0 | 逻辑删除 |

### 索引

| 索引名 | 类型 | 字段 | 说明 |
|--------|------|------|------|
| `PRIMARY` | PRIMARY | `id` | 主键 |
| `uk_content_hash` | UNIQUE | `content_hash` | 去重核心，防重复入池 |
| `idx_status` | 普通 | `status` | 按状态筛选 |
| `idx_source_id` | 普通 | `source_id` | 按数据源筛选 |
| `idx_industry` | 普通 | `industry` | 按行业筛选 |
| `idx_captured_at` | 普通 | `captured_at` | 按抓取时间排序 |

---

## 表 4：`demand_matches`（需求匹配结果表）

### 用途

存储 LLM 重排后的候选成果匹配结果。一条需求对应多条匹配记录（Top N），管理员在此表上执行确认操作。

### 关联关系

- `demand_id` → `demand_items.id`（软关联，不建外键）
- `achievement_doc_id` → `achievement_search_docs.achievement_doc_id`（软关联，保存快照）

### 字段定义

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|--------|------|--------|--------|------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | 主键 |
| `demand_id` | BIGINT UNSIGNED | NO | - | 需求 ID |
| `achievement_doc_id` | VARCHAR(255) | NO | - | 成果 documentId（快照，不做外键） |
| `result_title` | VARCHAR(500) | YES | NULL | 成果标题快照 |
| `result_type` | VARCHAR(100) | YES | NULL | 成果类型快照 |
| `owner` | VARCHAR(200) | YES | NULL | 成果负责人快照 |
| `match_score` | DECIMAL(4,3) | YES | NULL | LLM 给出的匹配分（0.000-1.000） |
| `reason` | TEXT | YES | NULL | 匹配理由 |
| `source_snippet` | TEXT | YES | NULL | 证据片段 |
| `fit_tags_json` | TEXT | YES | NULL | 匹配标签 JSON 数组 |
| `confirm_status` | VARCHAR(20) | NO | 'pending' | `pending` / `confirmed` / `rejected` |
| `confirmed_by` | VARCHAR(100) | YES | NULL | 确认人（Keycloak username） |
| `confirmed_at` | datetime(6) | YES | NULL | 确认时间 |
| `created_at` | datetime(6) | YES | NULL | 创建时间 |
| `updated_at` | datetime(6) | YES | NULL | 更新时间 |

### 索引

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | PRIMARY | `id` |
| `idx_demand_id` | 普通 | `demand_id` |
| `idx_achievement_doc_id` | 普通 | `achievement_doc_id` |
| `idx_confirm_status` | 普通 | `confirm_status` |

---

## 表 5：`demand_follow_ups`（需求跟进记录表）

### 用途

人工确认匹配后，记录每个需求的跟进过程和状态流转。

### 字段定义

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|--------|------|--------|--------|------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | 主键 |
| `demand_id` | BIGINT UNSIGNED | NO | - | 需求 ID（软关联 demand_items.id） |
| `owner_id` | VARCHAR(100) | YES | NULL | 跟进人 ID（Keycloak userId） |
| `owner_name` | VARCHAR(100) | YES | NULL | 跟进人姓名 |
| `status` | VARCHAR(50) | YES | NULL | 跟进状态（自由文本，如"洽谈中"） |
| `next_action` | VARCHAR(500) | YES | NULL | 下一步动作描述 |
| `due_at` | datetime(6) | YES | NULL | 截止时间 |
| `note` | TEXT | YES | NULL | 备注 |
| `created_at` | datetime(6) | YES | NULL | 创建时间 |
| `updated_at` | datetime(6) | YES | NULL | 更新时间 |

### 索引

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | PRIMARY | `id` |
| `idx_demand_id` | 普通 | `demand_id` |

---

## 表 6：`system_tasks`（系统异步任务表）

### 用途

记录采集、重匹配、研究洞察重建等 `@Async` 任务的执行状态，替代复杂的进度轮询。一期只记录状态和结果，不做精细百分比。

### 字段定义

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|--------|------|--------|--------|------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | 主键 |
| `task_type` | VARCHAR(50) | NO | - | `crawl` / `rematch` / `research_rebuild` |
| `status` | VARCHAR(20) | NO | 'pending' | `pending` / `running` / `success` / `failed` |
| `target_id` | VARCHAR(100) | YES | NULL | 目标 ID（如 demand_id），可为空 |
| `message` | TEXT | YES | NULL | 成功说明或失败原因 |
| `started_at` | datetime(6) | YES | NULL | 任务开始时间 |
| `finished_at` | datetime(6) | YES | NULL | 任务完成时间 |
| `created_at` | datetime(6) | YES | NULL | 记录创建时间 |

### 索引

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | PRIMARY | `id` |
| `idx_task_type_status` | 普通 | `task_type, status` |
| `idx_created_at` | 普通 | `created_at` |

---

## 表 7：`research_topics`（研究主题表）

### 用途

存储轻量研究洞察的主题聚合结果，由 `@Async rebuildResearchInsights()` 任务定期生成，每次重建覆盖旧数据。

### 字段定义

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|--------|------|--------|--------|------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | 主键 |
| `name` | VARCHAR(200) | NO | - | 主题名称（如"智能制造"） |
| `stage` | VARCHAR(20) | YES | NULL | `emerging` / `strong` / `cross` / `gap` |
| `keywords_json` | TEXT | YES | NULL | 主题关键词 JSON 数组 |
| `description` | TEXT | YES | NULL | 主题说明 |
| `external_heat` | DECIMAL(5,2) | YES | NULL | 外部热度（0-100） |
| `internal_strength` | DECIMAL(5,2) | YES | NULL | 内部基础（0-100） |
| `growth_rate_12m` | DECIMAL(5,2) | YES | NULL | 近 12 月增速（%） |
| `recommendation` | TEXT | YES | NULL | LLM 生成的建议动作 |
| `explanation` | TEXT | YES | NULL | LLM 生成的解释依据 |
| `internal_results_json` | LONGTEXT | YES | NULL | 内部代表成果列表 JSON |
| `external_signals_json` | LONGTEXT | YES | NULL | 外部需求信号列表 JSON |
| `trend_json` | TEXT | YES | NULL | 近 12 月趋势数组 JSON（12 个数值） |
| `generated_at` | datetime(6) | YES | NULL | 生成时间 |

### 索引

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | PRIMARY | `id` |
| `idx_stage` | 普通 | `stage` |
| `idx_generated_at` | 普通 | `generated_at` |

---

## 汇总

| # | 表名 | 优先级 | 核心作用 |
|---|------|--------|---------|
| 1 | `achievement_search_docs` | **P0** | 成果检索快照，FULLTEXT 全文索引在此 |
| 2 | `demand_sources` | **P0** | 白名单数据源配置 |
| 3 | `demand_items` | **P0** | 需求池主表，去重入口 |
| 4 | `demand_matches` | **P0** | LLM 重排匹配结果，人工确认在此 |
| 5 | `demand_follow_ups` | **P0** | 确认后跟进记录 |
| 6 | `system_tasks` | P1 | 异步任务状态跟踪 |
| 7 | `research_topics` | P1 | 轻量研究洞察主题快照 |

> ⚠️ **FULLTEXT 部署注意**：`achievement_search_docs` 的中文全文检索使用 ngram parser，需确认 MySQL 配置中 `ngram_token_size=2`（默认值），或在 `my.cnf` 中设置 `ft_min_word_len=1`。如果环境不支持 ngram，可改为在 `search_text` 中写入空格分隔的关键词串，降级为普通 LIKE 检索。
