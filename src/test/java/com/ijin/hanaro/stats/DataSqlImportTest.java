package com.ijin.hanaro.stats;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataSqlImportTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("마이그레이션/스키마가 준비되어 핵심 테이블이 존재한다")
    void migrationOrSchemaReady() {
        // Flyway가 없어도 동작하도록, 현재 DB 스키마에서 핵심 테이블 존재 확인
        Integer tables = jdbc.queryForObject(
                "select count(*) from information_schema.tables " +
                        "where table_schema = database() and table_name in ('users','products','orders','order_items')",
                Integer.class
        );
        assertThat(tables).as("users/products/orders/order_items 테이블 존재 개수").isGreaterThanOrEqualTo(4);
    }

    @Test
    @Sql(scripts = "classpath:data/data.sql")
    @DisplayName("data.sql 적재 시 핵심 테이블에 레코드가 삽입된다")
    void loadDataSqlAndVerify() {
        Integer userCnt    = jdbc.queryForObject("select count(*) from users", Integer.class);
        Integer productCnt = jdbc.queryForObject("select count(*) from products", Integer.class);
        Integer orderCnt   = jdbc.queryForObject("select count(*) from orders", Integer.class);
        Integer itemCnt    = jdbc.queryForObject("select count(*) from order_items", Integer.class);

        assertThat(userCnt).isNotNull().isGreaterThan(0);
        assertThat(productCnt).isNotNull().isGreaterThan(0);
        assertThat(orderCnt).isNotNull().isGreaterThan(0);
        assertThat(itemCnt).isNotNull().isGreaterThan(0);
    }
}