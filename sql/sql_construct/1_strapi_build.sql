/*
 Navicat Premium Data Transfer

 Source Server         : mysql
 Source Server Type    : MySQL
 Source Server Version : 80040 (8.0.40)
 Source Host           : localhost:3306
 Source Schema         : test

 Target Server Type    : MySQL
 Target Server Version : 80040 (8.0.40)
 File Encoding         : 65001

 Date: 04/03/2026 12:58:41
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admin_permissions
-- ----------------------------
DROP TABLE IF EXISTS `admin_permissions`;
CREATE TABLE `admin_permissions`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `action` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `action_parameters` json NULL,
  `subject` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `properties` json NULL,
  `conditions` json NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `published_at` datetime(6) NULL DEFAULT NULL,
  `created_by_id` int UNSIGNED NULL DEFAULT NULL,
  `updated_by_id` int UNSIGNED NULL DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `admin_permissions_documents_idx`(`document_id` ASC, `locale` ASC, `published_at` ASC) USING BTREE,
  INDEX `admin_permissions_created_by_id_fk`(`created_by_id` ASC) USING BTREE,
  INDEX `admin_permissions_updated_by_id_fk`(`updated_by_id` ASC) USING BTREE,
  CONSTRAINT `admin_permissions_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `admin_permissions_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 71 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for admin_permissions_role_lnk
-- ----------------------------
DROP TABLE IF EXISTS `admin_permissions_role_lnk`;
CREATE TABLE `admin_permissions_role_lnk`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `permission_id` int UNSIGNED NULL DEFAULT NULL,
  `role_id` int UNSIGNED NULL DEFAULT NULL,
  `permission_ord` double UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `admin_permissions_role_lnk_uq`(`permission_id` ASC, `role_id` ASC) USING BTREE,
  INDEX `admin_permissions_role_lnk_fk`(`permission_id` ASC) USING BTREE,
  INDEX `admin_permissions_role_lnk_ifk`(`role_id` ASC) USING BTREE,
  INDEX `admin_permissions_role_lnk_oifk`(`permission_ord` ASC) USING BTREE,
  CONSTRAINT `admin_permissions_role_lnk_fk` FOREIGN KEY (`permission_id`) REFERENCES `admin_permissions` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `admin_permissions_role_lnk_ifk` FOREIGN KEY (`role_id`) REFERENCES `admin_roles` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 71 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for admin_roles
-- ----------------------------
DROP TABLE IF EXISTS `admin_roles`;
CREATE TABLE `admin_roles`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `published_at` datetime(6) NULL DEFAULT NULL,
  `created_by_id` int UNSIGNED NULL DEFAULT NULL,
  `updated_by_id` int UNSIGNED NULL DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `admin_roles_documents_idx`(`document_id` ASC, `locale` ASC, `published_at` ASC) USING BTREE,
  INDEX `admin_roles_created_by_id_fk`(`created_by_id` ASC) USING BTREE,
  INDEX `admin_roles_updated_by_id_fk`(`updated_by_id` ASC) USING BTREE,
  CONSTRAINT `admin_roles_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `admin_roles_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for admin_users
-- ----------------------------
DROP TABLE IF EXISTS `admin_users`;
CREATE TABLE `admin_users`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `firstname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `lastname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `reset_password_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `registration_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `is_active` tinyint(1) NULL DEFAULT NULL,
  `blocked` tinyint(1) NULL DEFAULT NULL,
  `prefered_language` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `published_at` datetime(6) NULL DEFAULT NULL,
  `created_by_id` int UNSIGNED NULL DEFAULT NULL,
  `updated_by_id` int UNSIGNED NULL DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `admin_users_documents_idx`(`document_id` ASC, `locale` ASC, `published_at` ASC) USING BTREE,
  INDEX `admin_users_created_by_id_fk`(`created_by_id` ASC) USING BTREE,
  INDEX `admin_users_updated_by_id_fk`(`updated_by_id` ASC) USING BTREE,
  CONSTRAINT `admin_users_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `admin_users_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for admin_users_roles_lnk
-- ----------------------------
DROP TABLE IF EXISTS `admin_users_roles_lnk`;
CREATE TABLE `admin_users_roles_lnk`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` int UNSIGNED NULL DEFAULT NULL,
  `role_id` int UNSIGNED NULL DEFAULT NULL,
  `role_ord` double UNSIGNED NULL DEFAULT NULL,
  `user_ord` double UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `admin_users_roles_lnk_uq`(`user_id` ASC, `role_id` ASC) USING BTREE,
  INDEX `admin_users_roles_lnk_fk`(`user_id` ASC) USING BTREE,
  INDEX `admin_users_roles_lnk_ifk`(`role_id` ASC) USING BTREE,
  INDEX `admin_users_roles_lnk_ofk`(`role_ord` ASC) USING BTREE,
  INDEX `admin_users_roles_lnk_oifk`(`user_ord` ASC) USING BTREE,
  CONSTRAINT `admin_users_roles_lnk_fk` FOREIGN KEY (`user_id`) REFERENCES `admin_users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `admin_users_roles_lnk_ifk` FOREIGN KEY (`role_id`) REFERENCES `admin_roles` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for files
-- ----------------------------
DROP TABLE IF EXISTS `files`;
CREATE TABLE `files`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `alternative_text` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `caption` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `focal_point` json NULL,
  `width` int NULL DEFAULT NULL,
  `height` int NULL DEFAULT NULL,
  `formats` json NULL,
  `hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `ext` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `mime` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `size` decimal(10, 2) NULL DEFAULT NULL,
  `url` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `preview_url` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `provider` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `provider_metadata` json NULL,
  `folder_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `published_at` datetime(6) NULL DEFAULT NULL,
  `created_by_id` int UNSIGNED NULL DEFAULT NULL,
  `updated_by_id` int UNSIGNED NULL DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `upload_files_folder_path_index`(`folder_path` ASC) USING BTREE,
  INDEX `upload_files_created_at_index`(`created_at` ASC) USING BTREE,
  INDEX `upload_files_updated_at_index`(`updated_at` ASC) USING BTREE,
  INDEX `upload_files_name_index`(`name` ASC) USING BTREE,
  INDEX `upload_files_size_index`(`size` ASC) USING BTREE,
  INDEX `upload_files_ext_index`(`ext` ASC) USING BTREE,
  INDEX `files_documents_idx`(`document_id` ASC, `locale` ASC, `published_at` ASC) USING BTREE,
  INDEX `files_created_by_id_fk`(`created_by_id` ASC) USING BTREE,
  INDEX `files_updated_by_id_fk`(`updated_by_id` ASC) USING BTREE,
  CONSTRAINT `files_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `files_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for files_folder_lnk
-- ----------------------------
DROP TABLE IF EXISTS `files_folder_lnk`;
CREATE TABLE `files_folder_lnk`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `file_id` int UNSIGNED NULL DEFAULT NULL,
  `folder_id` int UNSIGNED NULL DEFAULT NULL,
  `file_ord` double UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `files_folder_lnk_uq`(`file_id` ASC, `folder_id` ASC) USING BTREE,
  INDEX `files_folder_lnk_fk`(`file_id` ASC) USING BTREE,
  INDEX `files_folder_lnk_ifk`(`folder_id` ASC) USING BTREE,
  INDEX `files_folder_lnk_oifk`(`file_ord` ASC) USING BTREE,
  CONSTRAINT `files_folder_lnk_fk` FOREIGN KEY (`file_id`) REFERENCES `files` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `files_folder_lnk_ifk` FOREIGN KEY (`folder_id`) REFERENCES `upload_folders` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for files_related_mph
-- ----------------------------
DROP TABLE IF EXISTS `files_related_mph`;
CREATE TABLE `files_related_mph`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `file_id` int UNSIGNED NULL DEFAULT NULL,
  `related_id` int UNSIGNED NULL DEFAULT NULL,
  `related_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `field` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `order` double UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `files_related_mph_fk`(`file_id` ASC) USING BTREE,
  INDEX `files_related_mph_oidx`(`order` ASC) USING BTREE,
  INDEX `files_related_mph_idix`(`related_id` ASC) USING BTREE,
  CONSTRAINT `files_related_mph_fk` FOREIGN KEY (`file_id`) REFERENCES `files` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for i18n_locale
-- ----------------------------
DROP TABLE IF EXISTS `i18n_locale`;
CREATE TABLE `i18n_locale`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `published_at` datetime(6) NULL DEFAULT NULL,
  `created_by_id` int UNSIGNED NULL DEFAULT NULL,
  `updated_by_id` int UNSIGNED NULL DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `i18n_locale_documents_idx`(`document_id` ASC, `locale` ASC, `published_at` ASC) USING BTREE,
  INDEX `i18n_locale_created_by_id_fk`(`created_by_id` ASC) USING BTREE,
  INDEX `i18n_locale_updated_by_id_fk`(`updated_by_id` ASC) USING BTREE,
  CONSTRAINT `i18n_locale_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `i18n_locale_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_ai_localization_jobs
-- ----------------------------
DROP TABLE IF EXISTS `strapi_ai_localization_jobs`;
CREATE TABLE `strapi_ai_localization_jobs`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `content_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `related_document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `source_locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `target_locales` json NOT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_ai_metadata_jobs
-- ----------------------------
DROP TABLE IF EXISTS `strapi_ai_metadata_jobs`;
CREATE TABLE `strapi_ai_metadata_jobs`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `completed_at` datetime(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_api_token_permissions
-- ----------------------------
DROP TABLE IF EXISTS `strapi_api_token_permissions`;
CREATE TABLE `strapi_api_token_permissions`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `action` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `published_at` datetime(6) NULL DEFAULT NULL,
  `created_by_id` int UNSIGNED NULL DEFAULT NULL,
  `updated_by_id` int UNSIGNED NULL DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `strapi_api_token_permissions_documents_idx`(`document_id` ASC, `locale` ASC, `published_at` ASC) USING BTREE,
  INDEX `strapi_api_token_permissions_created_by_id_fk`(`created_by_id` ASC) USING BTREE,
  INDEX `strapi_api_token_permissions_updated_by_id_fk`(`updated_by_id` ASC) USING BTREE,
  CONSTRAINT `strapi_api_token_permissions_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `strapi_api_token_permissions_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_api_token_permissions_token_lnk
-- ----------------------------
DROP TABLE IF EXISTS `strapi_api_token_permissions_token_lnk`;
CREATE TABLE `strapi_api_token_permissions_token_lnk`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `api_token_permission_id` int UNSIGNED NULL DEFAULT NULL,
  `api_token_id` int UNSIGNED NULL DEFAULT NULL,
  `api_token_permission_ord` double UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `strapi_api_token_permissions_token_lnk_uq`(`api_token_permission_id` ASC, `api_token_id` ASC) USING BTREE,
  INDEX `strapi_api_token_permissions_token_lnk_fk`(`api_token_permission_id` ASC) USING BTREE,
  INDEX `strapi_api_token_permissions_token_lnk_ifk`(`api_token_id` ASC) USING BTREE,
  INDEX `strapi_api_token_permissions_token_lnk_oifk`(`api_token_permission_ord` ASC) USING BTREE,
  CONSTRAINT `strapi_api_token_permissions_token_lnk_fk` FOREIGN KEY (`api_token_permission_id`) REFERENCES `strapi_api_token_permissions` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `strapi_api_token_permissions_token_lnk_ifk` FOREIGN KEY (`api_token_id`) REFERENCES `strapi_api_tokens` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_api_tokens
-- ----------------------------
DROP TABLE IF EXISTS `strapi_api_tokens`;
CREATE TABLE `strapi_api_tokens`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `access_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `encrypted_key` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `last_used_at` datetime(6) NULL DEFAULT NULL,
  `expires_at` datetime(6) NULL DEFAULT NULL,
  `lifespan` bigint NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `published_at` datetime(6) NULL DEFAULT NULL,
  `created_by_id` int UNSIGNED NULL DEFAULT NULL,
  `updated_by_id` int UNSIGNED NULL DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `strapi_api_tokens_documents_idx`(`document_id` ASC, `locale` ASC, `published_at` ASC) USING BTREE,
  INDEX `strapi_api_tokens_created_by_id_fk`(`created_by_id` ASC) USING BTREE,
  INDEX `strapi_api_tokens_updated_by_id_fk`(`updated_by_id` ASC) USING BTREE,
  CONSTRAINT `strapi_api_tokens_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `strapi_api_tokens_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_core_store_settings
-- ----------------------------
DROP TABLE IF EXISTS `strapi_core_store_settings`;
CREATE TABLE `strapi_core_store_settings`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `value` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `environment` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `tag` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 29 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_database_schema
-- ----------------------------
DROP TABLE IF EXISTS `strapi_database_schema`;
CREATE TABLE `strapi_database_schema`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `schema` json NULL,
  `time` datetime NULL DEFAULT NULL,
  `hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_history_versions
-- ----------------------------
DROP TABLE IF EXISTS `strapi_history_versions`;
CREATE TABLE `strapi_history_versions`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `content_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `related_document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `data` json NULL,
  `schema` json NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `created_by_id` int UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `strapi_history_versions_created_by_id_fk`(`created_by_id` ASC) USING BTREE,
  CONSTRAINT `strapi_history_versions_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_migrations
-- ----------------------------
DROP TABLE IF EXISTS `strapi_migrations`;
CREATE TABLE `strapi_migrations`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_migrations_internal
-- ----------------------------
DROP TABLE IF EXISTS `strapi_migrations_internal`;
CREATE TABLE `strapi_migrations_internal`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_release_actions
-- ----------------------------
DROP TABLE IF EXISTS `strapi_release_actions`;
CREATE TABLE `strapi_release_actions`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `content_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `entry_document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `is_entry_valid` tinyint(1) NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `published_at` datetime(6) NULL DEFAULT NULL,
  `created_by_id` int UNSIGNED NULL DEFAULT NULL,
  `updated_by_id` int UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `strapi_release_actions_documents_idx`(`document_id` ASC, `locale` ASC, `published_at` ASC) USING BTREE,
  INDEX `strapi_release_actions_created_by_id_fk`(`created_by_id` ASC) USING BTREE,
  INDEX `strapi_release_actions_updated_by_id_fk`(`updated_by_id` ASC) USING BTREE,
  CONSTRAINT `strapi_release_actions_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `strapi_release_actions_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_release_actions_release_lnk
-- ----------------------------
DROP TABLE IF EXISTS `strapi_release_actions_release_lnk`;
CREATE TABLE `strapi_release_actions_release_lnk`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `release_action_id` int UNSIGNED NULL DEFAULT NULL,
  `release_id` int UNSIGNED NULL DEFAULT NULL,
  `release_action_ord` double UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `strapi_release_actions_release_lnk_uq`(`release_action_id` ASC, `release_id` ASC) USING BTREE,
  INDEX `strapi_release_actions_release_lnk_fk`(`release_action_id` ASC) USING BTREE,
  INDEX `strapi_release_actions_release_lnk_ifk`(`release_id` ASC) USING BTREE,
  INDEX `strapi_release_actions_release_lnk_oifk`(`release_action_ord` ASC) USING BTREE,
  CONSTRAINT `strapi_release_actions_release_lnk_fk` FOREIGN KEY (`release_action_id`) REFERENCES `strapi_release_actions` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `strapi_release_actions_release_lnk_ifk` FOREIGN KEY (`release_id`) REFERENCES `strapi_releases` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_releases
-- ----------------------------
DROP TABLE IF EXISTS `strapi_releases`;
CREATE TABLE `strapi_releases`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `released_at` datetime(6) NULL DEFAULT NULL,
  `scheduled_at` datetime(6) NULL DEFAULT NULL,
  `timezone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `published_at` datetime(6) NULL DEFAULT NULL,
  `created_by_id` int UNSIGNED NULL DEFAULT NULL,
  `updated_by_id` int UNSIGNED NULL DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `strapi_releases_documents_idx`(`document_id` ASC, `locale` ASC, `published_at` ASC) USING BTREE,
  INDEX `strapi_releases_created_by_id_fk`(`created_by_id` ASC) USING BTREE,
  INDEX `strapi_releases_updated_by_id_fk`(`updated_by_id` ASC) USING BTREE,
  CONSTRAINT `strapi_releases_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `strapi_releases_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_sessions
-- ----------------------------
DROP TABLE IF EXISTS `strapi_sessions`;
CREATE TABLE `strapi_sessions`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `session_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `child_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `device_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `origin` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `expires_at` datetime(6) NULL DEFAULT NULL,
  `absolute_expires_at` datetime(6) NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `published_at` datetime(6) NULL DEFAULT NULL,
  `created_by_id` int UNSIGNED NULL DEFAULT NULL,
  `updated_by_id` int UNSIGNED NULL DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `strapi_sessions_documents_idx`(`document_id` ASC, `locale` ASC, `published_at` ASC) USING BTREE,
  INDEX `strapi_sessions_created_by_id_fk`(`created_by_id` ASC) USING BTREE,
  INDEX `strapi_sessions_updated_by_id_fk`(`updated_by_id` ASC) USING BTREE,
  CONSTRAINT `strapi_sessions_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `strapi_sessions_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_transfer_token_permissions
-- ----------------------------
DROP TABLE IF EXISTS `strapi_transfer_token_permissions`;
CREATE TABLE `strapi_transfer_token_permissions`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `action` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `published_at` datetime(6) NULL DEFAULT NULL,
  `created_by_id` int UNSIGNED NULL DEFAULT NULL,
  `updated_by_id` int UNSIGNED NULL DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `strapi_transfer_token_permissions_documents_idx`(`document_id` ASC, `locale` ASC, `published_at` ASC) USING BTREE,
  INDEX `strapi_transfer_token_permissions_created_by_id_fk`(`created_by_id` ASC) USING BTREE,
  INDEX `strapi_transfer_token_permissions_updated_by_id_fk`(`updated_by_id` ASC) USING BTREE,
  CONSTRAINT `strapi_transfer_token_permissions_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `strapi_transfer_token_permissions_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_transfer_token_permissions_token_lnk
-- ----------------------------
DROP TABLE IF EXISTS `strapi_transfer_token_permissions_token_lnk`;
CREATE TABLE `strapi_transfer_token_permissions_token_lnk`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `transfer_token_permission_id` int UNSIGNED NULL DEFAULT NULL,
  `transfer_token_id` int UNSIGNED NULL DEFAULT NULL,
  `transfer_token_permission_ord` double UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `strapi_transfer_token_permissions_token_lnk_uq`(`transfer_token_permission_id` ASC, `transfer_token_id` ASC) USING BTREE,
  INDEX `strapi_transfer_token_permissions_token_lnk_fk`(`transfer_token_permission_id` ASC) USING BTREE,
  INDEX `strapi_transfer_token_permissions_token_lnk_ifk`(`transfer_token_id` ASC) USING BTREE,
  INDEX `strapi_transfer_token_permissions_token_lnk_oifk`(`transfer_token_permission_ord` ASC) USING BTREE,
  CONSTRAINT `strapi_transfer_token_permissions_token_lnk_fk` FOREIGN KEY (`transfer_token_permission_id`) REFERENCES `strapi_transfer_token_permissions` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `strapi_transfer_token_permissions_token_lnk_ifk` FOREIGN KEY (`transfer_token_id`) REFERENCES `strapi_transfer_tokens` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_transfer_tokens
-- ----------------------------
DROP TABLE IF EXISTS `strapi_transfer_tokens`;
CREATE TABLE `strapi_transfer_tokens`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `access_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `last_used_at` datetime(6) NULL DEFAULT NULL,
  `expires_at` datetime(6) NULL DEFAULT NULL,
  `lifespan` bigint NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `published_at` datetime(6) NULL DEFAULT NULL,
  `created_by_id` int UNSIGNED NULL DEFAULT NULL,
  `updated_by_id` int UNSIGNED NULL DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `strapi_transfer_tokens_documents_idx`(`document_id` ASC, `locale` ASC, `published_at` ASC) USING BTREE,
  INDEX `strapi_transfer_tokens_created_by_id_fk`(`created_by_id` ASC) USING BTREE,
  INDEX `strapi_transfer_tokens_updated_by_id_fk`(`updated_by_id` ASC) USING BTREE,
  CONSTRAINT `strapi_transfer_tokens_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `strapi_transfer_tokens_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_webhooks
-- ----------------------------
DROP TABLE IF EXISTS `strapi_webhooks`;
CREATE TABLE `strapi_webhooks`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `url` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `headers` json NULL,
  `events` json NULL,
  `enabled` tinyint(1) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_workflows
-- ----------------------------
DROP TABLE IF EXISTS `strapi_workflows`;
CREATE TABLE `strapi_workflows`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `content_types` json NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `published_at` datetime(6) NULL DEFAULT NULL,
  `created_by_id` int UNSIGNED NULL DEFAULT NULL,
  `updated_by_id` int UNSIGNED NULL DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `strapi_workflows_documents_idx`(`document_id` ASC, `locale` ASC, `published_at` ASC) USING BTREE,
  INDEX `strapi_workflows_created_by_id_fk`(`created_by_id` ASC) USING BTREE,
  INDEX `strapi_workflows_updated_by_id_fk`(`updated_by_id` ASC) USING BTREE,
  CONSTRAINT `strapi_workflows_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `strapi_workflows_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_workflows_stage_required_to_publish_lnk
-- ----------------------------
DROP TABLE IF EXISTS `strapi_workflows_stage_required_to_publish_lnk`;
CREATE TABLE `strapi_workflows_stage_required_to_publish_lnk`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `workflow_id` int UNSIGNED NULL DEFAULT NULL,
  `workflow_stage_id` int UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `strapi_workflows_stage_required_to_publish_lnk_uq`(`workflow_id` ASC, `workflow_stage_id` ASC) USING BTREE,
  INDEX `strapi_workflows_stage_required_to_publish_lnk_fk`(`workflow_id` ASC) USING BTREE,
  INDEX `strapi_workflows_stage_required_to_publish_lnk_ifk`(`workflow_stage_id` ASC) USING BTREE,
  CONSTRAINT `strapi_workflows_stage_required_to_publish_lnk_fk` FOREIGN KEY (`workflow_id`) REFERENCES `strapi_workflows` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `strapi_workflows_stage_required_to_publish_lnk_ifk` FOREIGN KEY (`workflow_stage_id`) REFERENCES `strapi_workflows_stages` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_workflows_stages
-- ----------------------------
DROP TABLE IF EXISTS `strapi_workflows_stages`;
CREATE TABLE `strapi_workflows_stages`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `color` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `published_at` datetime(6) NULL DEFAULT NULL,
  `created_by_id` int UNSIGNED NULL DEFAULT NULL,
  `updated_by_id` int UNSIGNED NULL DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `strapi_workflows_stages_documents_idx`(`document_id` ASC, `locale` ASC, `published_at` ASC) USING BTREE,
  INDEX `strapi_workflows_stages_created_by_id_fk`(`created_by_id` ASC) USING BTREE,
  INDEX `strapi_workflows_stages_updated_by_id_fk`(`updated_by_id` ASC) USING BTREE,
  CONSTRAINT `strapi_workflows_stages_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `strapi_workflows_stages_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_workflows_stages_permissions_lnk
-- ----------------------------
DROP TABLE IF EXISTS `strapi_workflows_stages_permissions_lnk`;
CREATE TABLE `strapi_workflows_stages_permissions_lnk`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `workflow_stage_id` int UNSIGNED NULL DEFAULT NULL,
  `permission_id` int UNSIGNED NULL DEFAULT NULL,
  `permission_ord` double UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `strapi_workflows_stages_permissions_lnk_uq`(`workflow_stage_id` ASC, `permission_id` ASC) USING BTREE,
  INDEX `strapi_workflows_stages_permissions_lnk_fk`(`workflow_stage_id` ASC) USING BTREE,
  INDEX `strapi_workflows_stages_permissions_lnk_ifk`(`permission_id` ASC) USING BTREE,
  INDEX `strapi_workflows_stages_permissions_lnk_ofk`(`permission_ord` ASC) USING BTREE,
  CONSTRAINT `strapi_workflows_stages_permissions_lnk_fk` FOREIGN KEY (`workflow_stage_id`) REFERENCES `strapi_workflows_stages` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `strapi_workflows_stages_permissions_lnk_ifk` FOREIGN KEY (`permission_id`) REFERENCES `admin_permissions` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strapi_workflows_stages_workflow_lnk
-- ----------------------------
DROP TABLE IF EXISTS `strapi_workflows_stages_workflow_lnk`;
CREATE TABLE `strapi_workflows_stages_workflow_lnk`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `workflow_stage_id` int UNSIGNED NULL DEFAULT NULL,
  `workflow_id` int UNSIGNED NULL DEFAULT NULL,
  `workflow_stage_ord` double UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `strapi_workflows_stages_workflow_lnk_uq`(`workflow_stage_id` ASC, `workflow_id` ASC) USING BTREE,
  INDEX `strapi_workflows_stages_workflow_lnk_fk`(`workflow_stage_id` ASC) USING BTREE,
  INDEX `strapi_workflows_stages_workflow_lnk_ifk`(`workflow_id` ASC) USING BTREE,
  INDEX `strapi_workflows_stages_workflow_lnk_oifk`(`workflow_stage_ord` ASC) USING BTREE,
  CONSTRAINT `strapi_workflows_stages_workflow_lnk_fk` FOREIGN KEY (`workflow_stage_id`) REFERENCES `strapi_workflows_stages` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `strapi_workflows_stages_workflow_lnk_ifk` FOREIGN KEY (`workflow_id`) REFERENCES `strapi_workflows` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for up_permissions
-- ----------------------------
DROP TABLE IF EXISTS `up_permissions`;
CREATE TABLE `up_permissions`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `action` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `published_at` datetime(6) NULL DEFAULT NULL,
  `created_by_id` int UNSIGNED NULL DEFAULT NULL,
  `updated_by_id` int UNSIGNED NULL DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `up_permissions_documents_idx`(`document_id` ASC, `locale` ASC, `published_at` ASC) USING BTREE,
  INDEX `up_permissions_created_by_id_fk`(`created_by_id` ASC) USING BTREE,
  INDEX `up_permissions_updated_by_id_fk`(`updated_by_id` ASC) USING BTREE,
  CONSTRAINT `up_permissions_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `up_permissions_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for up_permissions_role_lnk
-- ----------------------------
DROP TABLE IF EXISTS `up_permissions_role_lnk`;
CREATE TABLE `up_permissions_role_lnk`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `permission_id` int UNSIGNED NULL DEFAULT NULL,
  `role_id` int UNSIGNED NULL DEFAULT NULL,
  `permission_ord` double UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `up_permissions_role_lnk_uq`(`permission_id` ASC, `role_id` ASC) USING BTREE,
  INDEX `up_permissions_role_lnk_fk`(`permission_id` ASC) USING BTREE,
  INDEX `up_permissions_role_lnk_ifk`(`role_id` ASC) USING BTREE,
  INDEX `up_permissions_role_lnk_oifk`(`permission_ord` ASC) USING BTREE,
  CONSTRAINT `up_permissions_role_lnk_fk` FOREIGN KEY (`permission_id`) REFERENCES `up_permissions` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `up_permissions_role_lnk_ifk` FOREIGN KEY (`role_id`) REFERENCES `up_roles` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for up_roles
-- ----------------------------
DROP TABLE IF EXISTS `up_roles`;
CREATE TABLE `up_roles`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `published_at` datetime(6) NULL DEFAULT NULL,
  `created_by_id` int UNSIGNED NULL DEFAULT NULL,
  `updated_by_id` int UNSIGNED NULL DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `up_roles_documents_idx`(`document_id` ASC, `locale` ASC, `published_at` ASC) USING BTREE,
  INDEX `up_roles_created_by_id_fk`(`created_by_id` ASC) USING BTREE,
  INDEX `up_roles_updated_by_id_fk`(`updated_by_id` ASC) USING BTREE,
  CONSTRAINT `up_roles_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `up_roles_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for up_users
-- ----------------------------
DROP TABLE IF EXISTS `up_users`;
CREATE TABLE `up_users`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `provider` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `reset_password_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `confirmation_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `confirmed` tinyint(1) NULL DEFAULT NULL,
  `blocked` tinyint(1) NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `published_at` datetime(6) NULL DEFAULT NULL,
  `created_by_id` int UNSIGNED NULL DEFAULT NULL,
  `updated_by_id` int UNSIGNED NULL DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `up_users_documents_idx`(`document_id` ASC, `locale` ASC, `published_at` ASC) USING BTREE,
  INDEX `up_users_created_by_id_fk`(`created_by_id` ASC) USING BTREE,
  INDEX `up_users_updated_by_id_fk`(`updated_by_id` ASC) USING BTREE,
  CONSTRAINT `up_users_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `up_users_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for up_users_role_lnk
-- ----------------------------
DROP TABLE IF EXISTS `up_users_role_lnk`;
CREATE TABLE `up_users_role_lnk`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` int UNSIGNED NULL DEFAULT NULL,
  `role_id` int UNSIGNED NULL DEFAULT NULL,
  `user_ord` double UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `up_users_role_lnk_uq`(`user_id` ASC, `role_id` ASC) USING BTREE,
  INDEX `up_users_role_lnk_fk`(`user_id` ASC) USING BTREE,
  INDEX `up_users_role_lnk_ifk`(`role_id` ASC) USING BTREE,
  INDEX `up_users_role_lnk_oifk`(`user_ord` ASC) USING BTREE,
  CONSTRAINT `up_users_role_lnk_fk` FOREIGN KEY (`user_id`) REFERENCES `up_users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `up_users_role_lnk_ifk` FOREIGN KEY (`role_id`) REFERENCES `up_roles` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for upload_folders
-- ----------------------------
DROP TABLE IF EXISTS `upload_folders`;
CREATE TABLE `upload_folders`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `document_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `path_id` int NULL DEFAULT NULL,
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `published_at` datetime(6) NULL DEFAULT NULL,
  `created_by_id` int UNSIGNED NULL DEFAULT NULL,
  `updated_by_id` int UNSIGNED NULL DEFAULT NULL,
  `locale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `upload_folders_path_id_index`(`path_id` ASC) USING BTREE,
  UNIQUE INDEX `upload_folders_path_index`(`path` ASC) USING BTREE,
  INDEX `upload_folders_documents_idx`(`document_id` ASC, `locale` ASC, `published_at` ASC) USING BTREE,
  INDEX `upload_folders_created_by_id_fk`(`created_by_id` ASC) USING BTREE,
  INDEX `upload_folders_updated_by_id_fk`(`updated_by_id` ASC) USING BTREE,
  CONSTRAINT `upload_folders_created_by_id_fk` FOREIGN KEY (`created_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `upload_folders_updated_by_id_fk` FOREIGN KEY (`updated_by_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for upload_folders_parent_lnk
-- ----------------------------
DROP TABLE IF EXISTS `upload_folders_parent_lnk`;
CREATE TABLE `upload_folders_parent_lnk`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `folder_id` int UNSIGNED NULL DEFAULT NULL,
  `inv_folder_id` int UNSIGNED NULL DEFAULT NULL,
  `folder_ord` double UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `upload_folders_parent_lnk_uq`(`folder_id` ASC, `inv_folder_id` ASC) USING BTREE,
  INDEX `upload_folders_parent_lnk_fk`(`folder_id` ASC) USING BTREE,
  INDEX `upload_folders_parent_lnk_ifk`(`inv_folder_id` ASC) USING BTREE,
  INDEX `upload_folders_parent_lnk_oifk`(`folder_ord` ASC) USING BTREE,
  CONSTRAINT `upload_folders_parent_lnk_fk` FOREIGN KEY (`folder_id`) REFERENCES `upload_folders` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `upload_folders_parent_lnk_ifk` FOREIGN KEY (`inv_folder_id`) REFERENCES `upload_folders` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
