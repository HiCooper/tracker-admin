package com.gateflow.tracker.config;

import com.clickhouse.jdbc.ClickHouseDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import lombok.Getter;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

@Slf4j
@Getter
@Configuration
public class ClickHouseConfig {

    @Value("${clickhouse.url}")
    private String url;

    @Value("${clickhouse.username:default}")
    private String username;

    @Value("${clickhouse.password:}")
    private String password;

    private DataSource chDataSource;

    @Bean(name = "clickHouseJdbcTemplate")
    public NamedParameterJdbcTemplate clickHouseJdbcTemplate() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", username);
        if (password != null && !password.isEmpty()) {
            props.setProperty("password", password);
        }
        props.setProperty("connect_timeout", "10000");
        props.setProperty("socket_timeout", "30000");
        log.info("Initializing ClickHouse JdbcTemplate: {}", url);
        chDataSource = new ClickHouseDataSource(url, props);
        return new NamedParameterJdbcTemplate(chDataSource);
    }
}
