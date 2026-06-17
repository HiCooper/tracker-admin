package com.gateflow.tracker.service;

import com.gateflow.tracker.config.ClickHouseConfig.ClickHouseProperties;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdminPrivacyServiceTest {

    private AdminPrivacyService service() {
        ClickHouseProperties props = new ClickHouseProperties();
        props.setUrl("jdbc:clickhouse://127.0.0.1:1/nodb"); // 不可达,避免误连
        return new AdminPrivacyService(props);
    }

    @Test
    void blankUserIdReturnsFalse() {
        assertThat(service().deleteUserData("  ")).isFalse();
        assertThat(service().deleteUserData(null)).isFalse();
    }

    @Test
    void unreachableClickHouseReturnsFalse() {
        // 连接失败应被吞掉并返回 false,不抛出
        assertThat(service().deleteUserData("u1")).isFalse();
    }

    @Test
    void runDeleteBindsUserIdAndExecutesAlterDelete() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(org.mockito.ArgumentMatchers.anyString())).thenReturn(stmt);

        AdminPrivacyService.runDelete(ds, "gateflow_tracker.events", "u1");

        verify(conn).prepareStatement("ALTER TABLE gateflow_tracker.events DELETE WHERE user_id = ?");
        verify(stmt).setString(1, "u1");
        verify(stmt).executeUpdate();
    }
}
