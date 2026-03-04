BEGIN;

-- collection_attachment / 征集附件 / achievement_type_id = 49
-- 字段清单：
-- 1. 开始时间
-- 2. 征集主题名称
-- 3. 征集描述
-- 4. 测试时间
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (1, 103, 49, 1);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (2, 105, 49, 2);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (3, 107, 49, 3);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (4, 109, 49, 4);

-- proposal_file / 申报书 / achievement_type_id = 51
-- 字段清单：
-- 1. 研究周期（月）
-- 2. 项目所属领域
-- 3. 项目名称
-- 4. 项目描述
-- 5. 项目预期成果
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (5, 111, 51, 1);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (6, 113, 51, 2);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (7, 115, 51, 3);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (8, 117, 51, 4);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (9, 119, 51, 5);

-- contract_template / 合同模版 / achievement_type_id = 53
-- 字段清单：
-- 1. 合同备注
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (10, 121, 53, 1);

-- deliverable_report / 成果物报告 / achievement_type_id = 55
-- 字段清单：
-- 1. 报告填写要求
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (11, 123, 55, 1);

-- signed_contract / 已签署合同 / achievement_type_id = 57
-- 字段清单：
-- 1. 合同备注
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (12, 125, 57, 1);

-- paper / 论文 / achievement_type_id = 59
-- 字段清单：
-- 1. 第一作者
-- 2. 发表刊物/会议
-- 3. 发表日期
-- 4. 期刊/会议级别
-- 5. 通讯作者
-- 6. 影响因子
-- 7. 是否TOP
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (13, 127, 59, 1);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (14, 129, 59, 2);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (15, 131, 59, 3);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (16, 133, 59, 4);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (17, 135, 59, 5);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (18, 137, 59, 6);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (19, 139, 59, 7);

-- software / 软著 / achievement_type_id = 61
-- 字段清单：
-- 1. 登记号
-- 2. 版本号
-- 3. 登记批准日期
-- 4. 著作权人
-- 5. 开发完成日期
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (20, 185, 61, 1);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (21, 186, 61, 2);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (22, 187, 61, 3);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (23, 188, 61, 4);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (24, 189, 61, 5);

-- patent / 专利 / achievement_type_id = 63
-- 字段清单：
-- 1. 申请号
-- 2. 专利类型
-- 3. 申请日期
-- 4. 申请人/专利权人
-- 5. 发明人列表
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (25, 160, 63, 1);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (26, 161, 63, 2);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (27, 162, 63, 3);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (28, 163, 63, 4);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (29, 164, 63, 5);

-- dataset / 数据集 / achievement_type_id = 65
-- 字段清单：
-- 1. 数据集标识
-- 2. 存储平台/仓库
-- 3. 发布日期
-- 4. 数据格式
-- 5. 样本量/记录数
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (30, 180, 65, 1);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (31, 181, 65, 2);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (32, 182, 65, 3);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (33, 183, 65, 4);
INSERT INTO `achievement_field_defs_achievement_type_id_lnk` VALUES (34, 184, 65, 5);

COMMIT;
