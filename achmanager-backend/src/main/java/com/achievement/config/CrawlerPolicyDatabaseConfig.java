package com.achievement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerPolicyDatabaseConfig implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            initializeCrawlerPolicyTables();
        } catch (Exception e) {
            log.error("初始化爬虫政策数据表失败", e);
        }
    }

    private void initializeCrawlerPolicyTables() {
        if (!tableExists("crawler_policies")) {
            log.info("爬虫政策数据表不存在，开始创建...");
            executeSqlScript("db/migration/crawler_policy_tables.sql");
            log.info("爬虫政策数据表创建完成");
        } else {
            log.info("爬虫政策数据表已存在");
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

    private void executeSqlScript(String scriptPath) {
        ClassPathResource resource = new ClassPathResource(scriptPath);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String sqlScript = reader.lines().collect(Collectors.joining("\n"));
            String[] sqlStatements = sqlScript.split(";");
            for (String sql : sqlStatements) {
                String trimmedSql = sql.trim();
                if (!trimmedSql.isEmpty() && !trimmedSql.startsWith("--")) {
                    try {
                        jdbcTemplate.execute(trimmedSql);
                    } catch (Exception e) {
                        log.warn("执行 SQL 失败: {}", trimmedSql.substring(0, Math.min(100, trimmedSql.length())));
                        log.warn("错误: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("读取 SQL 脚本 {} 失败: {}", scriptPath, e.getMessage());
            throw new RuntimeException("初始化爬虫政策数据表失败", e);
        }
    }
}
