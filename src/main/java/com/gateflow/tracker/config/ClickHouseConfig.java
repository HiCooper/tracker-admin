package com.gateflow.tracker.config;

import com.clickhouse.jdbc.ClickHouseDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class ClickHouseConfig {

    @Bean
    @ConfigurationProperties(prefix = "clickhouse")
    public ClickHouseProperties clickHouseProperties() {
        return new ClickHouseProperties();
    }

    /**
     * ClickHouse DataSource — NOT exposed as a Spring bean to avoid Flyway
     * trying to migrate it. Use {@link ClickHouseProperties} directly.
     */
    public static DataSource createDataSource(ClickHouseProperties props) throws Exception {
        Properties p = new Properties();
        p.setProperty("user", props.getUsername());
        if (props.getPassword() != null && !props.getPassword().isEmpty()) {
            p.setProperty("password", props.getPassword());
        }
        p.setProperty("socket_timeout", "30000");
        p.setProperty("connection_timeout", "10000");
        // 与 tracker-service 写入侧一致,统一按 UTC 解释 DateTime,避免聚合按本地时区偏移。
        p.setProperty("use_server_time_zone", "false");
        p.setProperty("use_time_zone", "UTC");
        return new ClickHouseDataSource(props.getUrl(), p);
    }

    public static class ClickHouseProperties {
        private String url = "jdbc:clickhouse://localhost:8123";
        private String username = "default";
        private String password = "";

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
