-- 轻量化 ES + MySQL RAG 建表脚本
-- 日期: 2026-05-25
-- 说明:
-- 1. MySQL 只保存业务主数据、同步快照和任务状态。
-- 2. Elasticsearch 负责成果/需求文本检索，不在本脚本中创建。
-- 3. 本脚本不修改 Strapi 原有表，不建立跨域外键，统一使用软引用。
-- 4. 一期主线为需求洞察；研究洞察表先建好，实施顺序放后。

SET NAMES utf8mb4;

-- ============================================================
-- 1. 成果检索快照表
-- ============================================================
CREATE TABLE IF NOT EXISTS `achievement_search_docs` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `achievement_doc_id` varchar(255) NOT NULL COMMENT '成果 documentId，软关联 achievement_mains.document_id',
  `title` varchar(500) DEFAULT NULL COMMENT '成果标题',
  `type_name` varchar(100) DEFAULT NULL COMMENT '成果类型名称',
  `type_code` varchar(50) DEFAULT NULL COMMENT '成果类型编码',
  `summary` text DEFAULT NULL COMMENT '成果摘要，建议同步时截断至 2000 字左右',
  `keywords_text` text DEFAULT NULL COMMENT '关键词拼接文本，来自 achievement_mains.keywords JSON',
  `authors_text` varchar(500) DEFAULT NULL COMMENT '作者/负责人拼接文本，来自 achievement_mains.authors JSON',
  `project_name` varchar(500) DEFAULT NULL COMMENT '项目名称',
  `year` varchar(20) DEFAULT NULL COMMENT '年份，与 achievement_mains.year 类型口径保持一致',
  `visibility_range` varchar(50) DEFAULT NULL COMMENT '成果可见范围',
  `attachment_names` text DEFAULT NULL COMMENT '附件名称拼接文本',
  `attachment_text_preview` longtext DEFAULT NULL COMMENT '附件轻量正文预览：每个附件提取前 5000-10000 字后拼接',
  `attachment_extract_status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '附件提取状态：pending/success/partial/failed/skipped',
  `attachment_extract_error` varchar(500) DEFAULT NULL COMMENT '附件提取失败原因或跳过说明',
  `search_text` longtext DEFAULT NULL COMMENT 'ES 检索主体：标题+摘要+关键词+类型+项目名+动态字段+附件名称+附件轻量正文',
  `search_hash` varchar(64) DEFAULT NULL COMMENT 'search_text 的 MD5，用于增量同步判断',
  `es_doc_id` varchar(255) DEFAULT NULL COMMENT 'ES 文档 ID',
  `es_indexed_at` datetime(6) DEFAULT NULL COMMENT '最近一次写入 ES 时间',
  `indexed_at` datetime(6) DEFAULT NULL COMMENT '最近一次构建检索快照时间',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `is_delete` int DEFAULT 0 COMMENT '逻辑删除：0=正常，1=删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_achievement_doc_id` (`achievement_doc_id`),
  KEY `idx_type_code` (`type_code`),
  KEY `idx_year` (`year`),
  KEY `idx_visibility_range` (`visibility_range`),
  KEY `idx_attachment_extract_status` (`attachment_extract_status`),
  KEY `idx_es_indexed_at` (`es_indexed_at`),
  KEY `idx_indexed_at` (`indexed_at`),
  KEY `idx_is_delete` (`is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='成果检索快照表：MySQL 业务快照 + ES 同步来源';

-- ============================================================
-- 2. 需求数据源配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS `demand_sources` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(200) NOT NULL COMMENT '数据源名称',
  `type` varchar(20) NOT NULL COMMENT '类型：rss/html/api/file',
  `industry` varchar(100) DEFAULT NULL COMMENT '所属行业',
  `region` varchar(100) DEFAULT NULL COMMENT '所属地域',
  `base_url` varchar(1000) NOT NULL COMMENT '采集地址',
  `description` varchar(500) DEFAULT NULL COMMENT '数据源说明',
  `auth_type` varchar(20) NOT NULL DEFAULT 'none' COMMENT '认证方式：none/api_key/cookie/basic',
  `credentials_json` text DEFAULT NULL COMMENT '认证信息 JSON，展示时必须脱敏',
  `frequency_hours` int NOT NULL DEFAULT 24 COMMENT '采集频率，单位小时',
  `priority` varchar(20) NOT NULL DEFAULT 'medium' COMMENT '优先级：low/medium/high',
  `tags_json` text DEFAULT NULL COMMENT '数据源标签 JSON 数组',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1=启用，0=停用',
  `status` varchar(20) NOT NULL DEFAULT 'idle' COMMENT '运行状态：healthy/warning/error/idle',
  `last_run_at` datetime(6) DEFAULT NULL COMMENT '最近运行时间',
  `last_success_at` datetime(6) DEFAULT NULL COMMENT '最近成功时间',
  `failure_reason` varchar(500) DEFAULT NULL COMMENT '最近失败原因',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `is_delete` int DEFAULT 0 COMMENT '逻辑删除：0=正常，1=删除',
  PRIMARY KEY (`id`),
  KEY `idx_enabled_status` (`enabled`, `status`),
  KEY `idx_type` (`type`),
  KEY `idx_priority` (`priority`),
  KEY `idx_is_delete` (`is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='需求白名单数据源配置表';

-- ============================================================
-- 3. 需求池主表
-- ============================================================
CREATE TABLE IF NOT EXISTS `demand_items` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `demand_code` varchar(100) DEFAULT NULL COMMENT '需求业务编号，可由后端生成',
  `title` varchar(500) NOT NULL COMMENT '需求标题',
  `raw_content` longtext DEFAULT NULL COMMENT '原始正文',
  `summary` text DEFAULT NULL COMMENT '来源站点提供或规则抽取摘要',
  `llm_summary` text DEFAULT NULL COMMENT 'LLM 生成摘要',
  `keywords_json` text DEFAULT NULL COMMENT '关键词 JSON 数组',
  `tags_json` text DEFAULT NULL COMMENT '标签 JSON 数组',
  `industry` varchar(100) DEFAULT NULL COMMENT '行业',
  `region` varchar(100) DEFAULT NULL COMMENT '地域',
  `source_category` varchar(100) DEFAULT NULL COMMENT '来源分类，如政府/园区/企业/招投标',
  `source_id` bigint unsigned DEFAULT NULL COMMENT '数据源 ID，软关联 demand_sources.id',
  `source_site` varchar(200) DEFAULT NULL COMMENT '来源站点名称',
  `source_url` varchar(1000) DEFAULT NULL COMMENT '原始链接',
  `captured_at` datetime(6) DEFAULT NULL COMMENT '抓取时间',
  `priority` varchar(20) NOT NULL DEFAULT 'medium' COMMENT '优先级：low/medium/high',
  `confidence` decimal(4,3) DEFAULT NULL COMMENT '结构化置信度：0.000-1.000',
  `best_match_score` decimal(4,3) DEFAULT NULL COMMENT '当前最佳匹配分：0.000-1.000',
  `status` varchar(30) NOT NULL DEFAULT 'new' COMMENT '状态：new/reviewing/matched/in_follow_up/invalid/archived',
  `owner_id` varchar(100) DEFAULT NULL COMMENT '负责人 ID',
  `owner_name` varchar(100) DEFAULT NULL COMMENT '负责人名称',
  `due_at` datetime(6) DEFAULT NULL COMMENT '建议处理截止时间',
  `pending_confirmations_json` text DEFAULT NULL COMMENT '待人工确认事项 JSON 数组',
  `risk_notes_json` text DEFAULT NULL COMMENT '风险提示 JSON 数组',
  `content_hash` varchar(64) DEFAULT NULL COMMENT '去重 hash',
  `es_doc_id` varchar(255) DEFAULT NULL COMMENT 'ES 文档 ID',
  `es_indexed_at` datetime(6) DEFAULT NULL COMMENT '最近一次写入 ES 时间',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `is_delete` int DEFAULT 0 COMMENT '逻辑删除：0=正常，1=删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_content_hash` (`content_hash`),
  UNIQUE KEY `uk_demand_code` (`demand_code`),
  KEY `idx_status` (`status`),
  KEY `idx_source_id` (`source_id`),
  KEY `idx_industry` (`industry`),
  KEY `idx_region` (`region`),
  KEY `idx_priority` (`priority`),
  KEY `idx_captured_at` (`captured_at`),
  KEY `idx_es_indexed_at` (`es_indexed_at`),
  KEY `idx_is_delete` (`is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='需求池主表';

-- ============================================================
-- 4. 需求匹配结果表
-- ============================================================
CREATE TABLE IF NOT EXISTS `demand_matches` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `demand_id` bigint unsigned NOT NULL COMMENT '需求 ID，软关联 demand_items.id',
  `achievement_doc_id` varchar(255) NOT NULL COMMENT '成果 documentId 快照',
  `result_title` varchar(500) DEFAULT NULL COMMENT '成果标题快照',
  `result_type` varchar(100) DEFAULT NULL COMMENT '成果类型快照',
  `owner` varchar(200) DEFAULT NULL COMMENT '成果负责人快照',
  `department` varchar(200) DEFAULT NULL COMMENT '成果所属部门快照',
  `es_score` decimal(10,4) DEFAULT NULL COMMENT 'ES 原始召回分',
  `rule_score` decimal(4,3) DEFAULT NULL COMMENT '规则加权分：0.000-1.000',
  `llm_score` decimal(4,3) DEFAULT NULL COMMENT 'LLM 重排分：0.000-1.000',
  `match_score` decimal(4,3) DEFAULT NULL COMMENT '最终匹配分：0.000-1.000',
  `reason` text DEFAULT NULL COMMENT '匹配理由',
  `source_snippet` text DEFAULT NULL COMMENT '证据片段，来自成果摘要/动态字段/附件轻量正文',
  `fit_tags_json` text DEFAULT NULL COMMENT '匹配标签 JSON 数组',
  `confirm_status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '人工确认状态：pending/confirmed/rejected',
  `confirmed_by` varchar(100) DEFAULT NULL COMMENT '确认人',
  `confirmed_at` datetime(6) DEFAULT NULL COMMENT '确认时间',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `is_delete` int DEFAULT 0 COMMENT '逻辑删除：0=正常，1=删除',
  PRIMARY KEY (`id`),
  KEY `idx_demand_id` (`demand_id`),
  KEY `idx_achievement_doc_id` (`achievement_doc_id`),
  KEY `idx_confirm_status` (`confirm_status`),
  KEY `idx_match_score` (`match_score`),
  KEY `idx_is_delete` (`is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='需求与成果匹配结果表';

-- ============================================================
-- 5. 需求跟进记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `demand_follow_ups` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `demand_id` bigint unsigned NOT NULL COMMENT '需求 ID，软关联 demand_items.id',
  `owner_id` varchar(100) DEFAULT NULL COMMENT '跟进人 ID',
  `owner_name` varchar(100) DEFAULT NULL COMMENT '跟进人姓名',
  `status` varchar(50) DEFAULT NULL COMMENT '跟进状态，如待联系/洽谈中/已对接/已结束',
  `next_action` varchar(500) DEFAULT NULL COMMENT '下一步动作',
  `due_at` datetime(6) DEFAULT NULL COMMENT '截止时间',
  `note` text DEFAULT NULL COMMENT '备注',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `is_delete` int DEFAULT 0 COMMENT '逻辑删除：0=正常，1=删除',
  PRIMARY KEY (`id`),
  KEY `idx_demand_id` (`demand_id`),
  KEY `idx_status` (`status`),
  KEY `idx_due_at` (`due_at`),
  KEY `idx_is_delete` (`is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='需求跟进记录表';

-- ============================================================
-- 6. 系统异步任务表
-- ============================================================
CREATE TABLE IF NOT EXISTS `system_tasks` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_type` varchar(50) NOT NULL COMMENT '任务类型：crawl/rematch/index_achievement/index_demand/research_rebuild',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '任务状态：pending/running/success/failed',
  `target_type` varchar(50) DEFAULT NULL COMMENT '目标类型：source/demand/achievement/research',
  `target_id` varchar(100) DEFAULT NULL COMMENT '目标 ID',
  `message` text DEFAULT NULL COMMENT '成功说明或失败原因',
  `started_at` datetime(6) DEFAULT NULL COMMENT '任务开始时间',
  `finished_at` datetime(6) DEFAULT NULL COMMENT '任务完成时间',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_type_status` (`task_type`, `status`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='系统异步任务状态表';

-- ============================================================
-- 7. 研究主题轻量洞察表
-- ============================================================
CREATE TABLE IF NOT EXISTS `research_topics` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `topic_code` varchar(100) DEFAULT NULL COMMENT '主题编号',
  `name` varchar(200) NOT NULL COMMENT '主题名称',
  `domain` varchar(100) DEFAULT NULL COMMENT '学科域',
  `field` varchar(100) DEFAULT NULL COMMENT '学科',
  `stage` varchar(20) DEFAULT NULL COMMENT '主题阶段：emerging/strong/cross/gap',
  `keywords_json` text DEFAULT NULL COMMENT '主题关键词 JSON 数组',
  `description` text DEFAULT NULL COMMENT '主题说明',
  `external_heat` decimal(5,2) DEFAULT NULL COMMENT '外部热度：0-100',
  `internal_strength` decimal(5,2) DEFAULT NULL COMMENT '内部基础：0-100',
  `growth_rate_12m` decimal(6,2) DEFAULT NULL COMMENT '近 12 月增速百分比',
  `opportunity_type` varchar(100) DEFAULT NULL COMMENT '机会类型',
  `recommendation` text DEFAULT NULL COMMENT '建议动作',
  `explanation` text DEFAULT NULL COMMENT '解释依据',
  `departments_json` text DEFAULT NULL COMMENT '参与部门 JSON 数组',
  `internal_results_json` longtext DEFAULT NULL COMMENT '内部代表成果列表 JSON',
  `external_signals_json` longtext DEFAULT NULL COMMENT '外部需求信号列表 JSON',
  `trend_json` text DEFAULT NULL COMMENT '近 12 月趋势数组 JSON',
  `generated_at` datetime(6) DEFAULT NULL COMMENT '生成时间',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `is_delete` int DEFAULT 0 COMMENT '逻辑删除：0=正常，1=删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_topic_code` (`topic_code`),
  KEY `idx_stage` (`stage`),
  KEY `idx_generated_at` (`generated_at`),
  KEY `idx_is_delete` (`is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='研究主题轻量洞察表';
