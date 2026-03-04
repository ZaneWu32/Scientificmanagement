/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 90100 (9.1.0)
 Source Host           : localhost:3306
 Source Schema         : strapi

 Target Server Type    : MySQL
 Target Server Version : 90100 (9.1.0)
 File Encoding         : 65001

 Date: 04/03/2026 12:45:38
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for achievement_access_grants
-- ----------------------------
DROP TABLE IF EXISTS `achievement_access_grants`;
CREATE TABLE `achievement_access_grants` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Strapi文档ID',
  `achievement_id` int unsigned NOT NULL COMMENT '成果ID',
  `achievement_doc_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '成果documentId',
  `grantee_id` int unsigned NOT NULL COMMENT '被授权人ID',
  `grantee_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '被授权人姓名',
  `source_request_id` int unsigned DEFAULT NULL COMMENT '来源申请ID',
  `source_request_doc_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '来源申请documentId',
  `granted_by_id` int unsigned NOT NULL COMMENT '授权人ID',
  `granted_by_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '授权人姓名',
  `grant_scope` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'full_content' COMMENT '授权范围',
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active' COMMENT '状态：active/revoked/expired',
  `granted_at` datetime(6) NOT NULL COMMENT '授权时间',
  `expires_at` datetime(6) DEFAULT NULL COMMENT '过期时间',
  `revoked_at` datetime(6) DEFAULT NULL COMMENT '撤销时间',
  `revoke_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '撤销原因',
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `created_by_id` int unsigned DEFAULT NULL,
  `updated_by_id` int unsigned DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_delete` int DEFAULT '0' COMMENT '软删除标记',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_achievement_grantee_active` (`achievement_doc_id`,`grantee_id`,`status`,`is_delete`) USING BTREE,
  KEY `idx_achievement_id` (`achievement_id`) USING BTREE,
  KEY `idx_achievement_doc_id` (`achievement_doc_id`) USING BTREE,
  KEY `idx_grantee_id` (`grantee_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_granted_at` (`granted_at`) USING BTREE,
  KEY `achievement_access_grants_documents_idx` (`document_id`,`locale`,`published_at`) USING BTREE,
  KEY `achievement_access_grants_created_by_id_fk` (`created_by_id`) USING BTREE,
  KEY `achievement_access_grants_updated_by_id_fk` (`updated_by_id`) USING BTREE,
  CONSTRAINT `achievement_access_grants_achievement_fk` FOREIGN KEY (`achievement_id`) REFERENCES `achievement_mains` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `achievement_access_grants_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `achievement_access_grants_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='成果全文访问授权表';




-- ----------------------------
-- Table structure for achievement_access_requests
-- ----------------------------
DROP TABLE IF EXISTS `achievement_access_requests`;
CREATE TABLE `achievement_access_requests` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Strapi文档ID',
  `achievement_id` int unsigned NOT NULL COMMENT '成果ID',
  `achievement_doc_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '成果documentId',
  `achievement_title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '成果标题',
  `requester_id` int unsigned NOT NULL COMMENT '申请人ID',
  `requester_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '申请人姓名',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '申请理由',
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'pending' COMMENT '状态：pending/approved/rejected',
  `reviewer_id` int unsigned DEFAULT NULL COMMENT '审批人ID',
  `reviewer_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批人姓名',
  `review_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '审批意见',
  `reviewed_at` datetime(6) DEFAULT NULL COMMENT '审批时间',
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `created_by_id` int unsigned DEFAULT NULL,
  `updated_by_id` int unsigned DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_delete` int DEFAULT '0' COMMENT '软删除标记',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_achievement_id` (`achievement_id`) USING BTREE,
  KEY `idx_achievement_doc_id` (`achievement_doc_id`) USING BTREE,
  KEY `idx_requester_id` (`requester_id`) USING BTREE,
  KEY `idx_reviewer_id` (`reviewer_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_created_at` (`created_at`) USING BTREE,
  KEY `achievement_access_requests_documents_idx` (`document_id`,`locale`,`published_at`) USING BTREE,
  KEY `achievement_access_requests_created_by_id_fk` (`created_by_id`) USING BTREE,
  KEY `achievement_access_requests_updated_by_id_fk` (`updated_by_id`) USING BTREE,
  CONSTRAINT `achievement_access_requests_achievement_fk` FOREIGN KEY (`achievement_id`) REFERENCES `achievement_mains` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `achievement_access_requests_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `achievement_access_requests_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='成果访问权限申请表';




-- ----------------------------
-- Table structure for achievement_reviewer_assignments
-- ----------------------------
DROP TABLE IF EXISTS `achievement_reviewer_assignments`;
CREATE TABLE `achievement_reviewer_assignments` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Strapi文档ID',
  `achievement_id` int unsigned NOT NULL COMMENT '成果ID',
  `achievement_doc_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '成果documentId',
  `reviewer_id` int unsigned NOT NULL COMMENT '审核人ID',
  `reviewer_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '审核人姓名',
  `assigned_by_id` int unsigned DEFAULT NULL COMMENT '分配人ID',
  `assigned_by_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分配人姓名',
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'pending' COMMENT '状态：pending/completed/cancelled',
  `assigned_at` datetime(6) DEFAULT NULL COMMENT '分配时间',
  `completed_at` datetime(6) DEFAULT NULL COMMENT '完成时间',
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `created_by_id` int unsigned DEFAULT NULL,
  `updated_by_id` int unsigned DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_delete` int DEFAULT '0' COMMENT '软删除标记',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_achievement_id` (`achievement_id`) USING BTREE,
  KEY `idx_achievement_doc_id` (`achievement_doc_id`) USING BTREE,
  KEY `idx_reviewer_id` (`reviewer_id`) USING BTREE,
  KEY `idx_assigned_by_id` (`assigned_by_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_assigned_at` (`assigned_at`) USING BTREE,
  KEY `achievement_reviewer_assignments_documents_idx` (`document_id`,`locale`,`published_at`) USING BTREE,
  KEY `achievement_reviewer_assignments_created_by_id_fk` (`created_by_id`) USING BTREE,
  KEY `achievement_reviewer_assignments_updated_by_id_fk` (`updated_by_id`) USING BTREE,
  CONSTRAINT `achievement_reviewer_assignments_achievement_fk` FOREIGN KEY (`achievement_id`) REFERENCES `achievement_mains` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `achievement_reviewer_assignments_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `achievement_reviewer_assignments_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='成果审核人分配表';




-- ----------------------------
-- Table structure for achievement_reviews
-- ----------------------------
DROP TABLE IF EXISTS `achievement_reviews`;
CREATE TABLE `achievement_reviews` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Strapi文档ID',
  `achievement_id` int unsigned NOT NULL COMMENT '成果ID',
  `achievement_doc_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '成果documentId',
  `reviewer_id` int unsigned NOT NULL COMMENT '审核人ID',
  `reviewer_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '审核人姓名',
  `action` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '审核动作：approve/reject/request_changes',
  `comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '审核意见',
  `previous_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核前状态',
  `new_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核后状态',
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `created_by_id` int unsigned DEFAULT NULL,
  `updated_by_id` int unsigned DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_delete` int DEFAULT '0' COMMENT '软删除标记',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_achievement_id` (`achievement_id`) USING BTREE,
  KEY `idx_achievement_doc_id` (`achievement_doc_id`) USING BTREE,
  KEY `idx_reviewer_id` (`reviewer_id`) USING BTREE,
  KEY `idx_action` (`action`) USING BTREE,
  KEY `idx_created_at` (`created_at`) USING BTREE,
  KEY `achievement_reviews_documents_idx` (`document_id`,`locale`,`published_at`) USING BTREE,
  KEY `achievement_reviews_created_by_id_fk` (`created_by_id`) USING BTREE,
  KEY `achievement_reviews_updated_by_id_fk` (`updated_by_id`) USING BTREE,
  CONSTRAINT `achievement_reviews_achievement_fk` FOREIGN KEY (`achievement_id`) REFERENCES `achievement_mains` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `achievement_reviews_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `achievement_reviews_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='成果审核记录表';




-- ----------------------------
-- Table structure for business_users
-- ----------------------------
DROP TABLE IF EXISTS `business_users`;
CREATE TABLE `business_users` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `keycloak_user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Keycloak 用户 UUID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_keycloak_user_id` (`keycloak_user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='业务用户映射表';

SET FOREIGN_KEY_CHECKS = 1;
