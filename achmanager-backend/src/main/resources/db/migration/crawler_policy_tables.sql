-- 爬虫政策数据表结构
-- 创建时间: 2026-05-29

-- 1. 爬虫政策数据表
CREATE TABLE IF NOT EXISTS crawler_policies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    crawler_id VARCHAR(50) NOT NULL COMMENT '来源爬虫ID',
    source_url VARCHAR(1000) NOT NULL COMMENT '原始URL',
    title VARCHAR(500) NOT NULL COMMENT '政策标题',
    publish_date VARCHAR(50) COMMENT '原始日期字符串',
    publish_date_parsed DATE COMMENT '解析后的日期',
    content MEDIUMTEXT COMMENT '全文内容',
    hrefs TEXT COMMENT '相关链接(JSON数组)',
    content_hash VARCHAR(64) NOT NULL COMMENT '内容去重哈希(MD5)',
    keywords_extracted TEXT COMMENT '提取的关键词(JSON数组)',

    -- 系统字段
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志',

    UNIQUE KEY uk_content_hash (content_hash),
    KEY idx_crawler_id (crawler_id),
    KEY idx_publish_date (publish_date_parsed),
    KEY idx_create_time (create_time)
) COMMENT='爬虫政策数据表';

-- 2. 政策-成果物匹配关系表
CREATE TABLE IF NOT EXISTS crawler_policy_achievement_match (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    policy_id BIGINT NOT NULL COMMENT '关联政策ID',
    achievement_document_id VARCHAR(100) NOT NULL COMMENT '关联成果物document_id',
    match_score DECIMAL(5,4) NOT NULL COMMENT '匹配分数(0.0000~1.0000)',
    match_method VARCHAR(20) NOT NULL COMMENT '匹配方式(keyword/embedding/llm)',
    match_reason TEXT COMMENT '匹配理由',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否有效',

    -- 系统字段
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_policy_achievement_method (policy_id, achievement_document_id, match_method),
    KEY idx_policy_id (policy_id),
    KEY idx_achievement_document_id (achievement_document_id),
    KEY idx_match_score (match_score),
    KEY idx_is_active (is_active)
) COMMENT='政策-成果物匹配关系表';
