package com.gateflow.tracker.security;

import com.gateflow.tracker.domain.entity.TrackerAuditLog;
import com.gateflow.tracker.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuditInterceptorTest {

    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final AuditInterceptor interceptor = new AuditInterceptor(auditLogService);

    private HttpServletRequest request(String method, String uri) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn(method);
        when(req.getRequestURI()).thenReturn(uri);
        return req;
    }

    @Test
    void recordsMutatingOperationWithContext() {
        HttpServletRequest req = request("DELETE", "/api/v1/engineering/plans/5");
        when(req.getAttribute("username")).thenReturn("alice");
        when(req.getAttribute("role")).thenReturn("admin");
        when(req.getRemoteAddr()).thenReturn("10.0.0.1");
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(resp.getStatus()).thenReturn(200);

        interceptor.afterCompletion(req, resp, new Object(), null);

        ArgumentCaptor<TrackerAuditLog> captor = ArgumentCaptor.forClass(TrackerAuditLog.class);
        verify(auditLogService).record(captor.capture());
        TrackerAuditLog e = captor.getValue();
        assertThat(e.getUsername()).isEqualTo("alice");
        assertThat(e.getRole()).isEqualTo("admin");
        assertThat(e.getMethod()).isEqualTo("DELETE");
        assertThat(e.getPath()).isEqualTo("/api/v1/engineering/plans/5");
        assertThat(e.getStatus()).isEqualTo(200);
        assertThat(e.getIp()).isEqualTo("10.0.0.1");
    }

    @Test
    void skipsReadOperations() {
        interceptor.afterCompletion(request("GET", "/api/v1/engineering/plans"),
                mock(HttpServletResponse.class), new Object(), null);
        verify(auditLogService, never()).record(any());
    }

    @Test
    void skipsExcludedAuthAndCollectPaths() {
        interceptor.afterCompletion(request("POST", "/api/v1/auth/login"),
                mock(HttpServletResponse.class), new Object(), null);
        interceptor.afterCompletion(request("POST", "/api/v1/collect"),
                mock(HttpServletResponse.class), new Object(), null);
        verify(auditLogService, never()).record(any());
    }

    @Test
    void anonymousWhenNoUsernameAttribute() {
        HttpServletRequest req = request("POST", "/api/v1/engineering/plans");
        when(req.getRemoteAddr()).thenReturn("1.2.3.4");
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(resp.getStatus()).thenReturn(403);

        interceptor.afterCompletion(req, resp, new Object(), null);

        ArgumentCaptor<TrackerAuditLog> captor = ArgumentCaptor.forClass(TrackerAuditLog.class);
        verify(auditLogService).record(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("anonymous");
        assertThat(captor.getValue().getStatus()).isEqualTo(403);
    }

    @Test
    void clientIpPrefersFirstXForwardedFor() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("203.0.113.7, 10.0.0.1");
        assertThat(AuditInterceptor.clientIp(req)).isEqualTo("203.0.113.7");
    }
}
