package com.example.demo2;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@EnableJdbcRepositories(basePackages = "com.example.demo2.domain.repository")
public class PersistenceConfig {

    @Bean
    public DataSource dataSource() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:12.1-alpine")
                .withDatabaseName("batch-db")
                .withUsername("cellrebel")
                .withPassword("cellrebel");

        container.start();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(container.getJdbcUrl());
        hikariConfig.setPassword(container.getPassword());
        hikariConfig.setUsername(container.getUsername());
        hikariConfig.setDriverClassName("org.postgresql.Driver");

        return new HikariDataSource(hikariConfig);
    }

}
