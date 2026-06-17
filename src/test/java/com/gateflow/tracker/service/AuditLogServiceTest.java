package com.gateflow.tracker.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gateflow.tracker.domain.dto.PageResponse;
import com.gateflow.tracker.domain.entity.TrackerAuditLog;
import com.gateflow.tracker.repository.TrackerAuditLogMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditLogServiceTest {

    private final TrackerAuditLogMapper mapper = mock(TrackerAuditLogMapper.class);
    private final AuditLogService service = new AuditLogService(mapper);

    @Test
    void recordInsertsEntry() {
        TrackerAuditLog entry = TrackerAuditLog.builder().username("a").method("POST").path("/x").build();
        service.record(entry);
        verify(mapper).insert(entry);
    }

    @Test
    void recordSwallowsPersistenceErrors() {
        when(mapper.insert(any(TrackerAuditLog.class))).thenThrow(new RuntimeException("db down"));
        // 审计失败绝不能冒泡影响主请求
        assertThatNoException().isThrownBy(() ->
                service.record(TrackerAuditLog.builder().username("a").method("POST").path("/x").build()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void queryReturnsPagedResult() {
        IPage<TrackerAuditLog> page = new Page<>(1, 50);
        page.setRecords(List.of(TrackerAuditLog.builder().username("alice").build()));
        page.setTotal(1);
        when(mapper.selectPage(any(), any())).thenReturn(page);

        PageResponse<TrackerAuditLog> resp = service.query(1, 50, "alice");

        assertThat(resp.getData().getTotal()).isEqualTo(1);
        assertThat(resp.getData().getList()).hasSize(1);
        verify(mapper).selectPage(any(), any());
    }
}
