package com.achievement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.db-init.enabled", havingValue = "true", matchIfMissing = true)
public class RagDatabaseConfig implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        try {
            initializeRagTables();
        } catch (Exception e) {
            log.error("初始化 RAG 相关表失败", e);
        }
    }

    private void initializeRagTables() {
        if (!tableExists("demand_items")) {
            log.info("RAG 相关表不存在，开始创建...");
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("db/migration/rag_lightweight_es_mysql_tables.sql"));
            populator.setSeparator(";");
            populator.setCommentPrefix("--");
            populator.execute(dataSource);
            log.info("RAG 相关表创建完成");
        } else {
            log.info("RAG 相关表已存在");
        }
    }

    private boolean tableExists(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("检查表 {} 是否存在时出错: {}", tableName, e.getMessage());
            return false;
        }
    }
}
