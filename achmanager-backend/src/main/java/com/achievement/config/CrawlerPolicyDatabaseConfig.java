package com.achievement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerPolicyDatabaseConfig implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

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
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("db/migration/crawler_policy_tables.sql"));
            populator.setSeparator(";");
            populator.setCommentPrefix("--");
            populator.execute(dataSource);
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
}
