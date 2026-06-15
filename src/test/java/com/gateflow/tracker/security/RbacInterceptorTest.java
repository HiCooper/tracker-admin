package com.gateflow.tracker.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RbacInterceptorTest {

    private final RbacInterceptor interceptor = new RbacInterceptor(new ObjectMapper());

    // ---- test fixtures: controller-like beans with annotated handlers ----
    static class Sample {
        @RequireRole("admin")
        public void adminOnly() { }

        public void unrestricted() { }
    }

    private HandlerMethod handler(String method) throws NoSuchMethodException {
        Method m = Sample.class.getMethod(method);
        return new HandlerMethod(new Sample(), m);
    }

    private HttpServletResponse mockResponse(StringWriter sink) throws Exception {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(resp.getWriter()).thenReturn(new PrintWriter(sink));
        return resp;
    }

    @Test
    void allowsWhenRoleMatches() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("role")).thenReturn("admin");

        boolean ok = interceptor.preHandle(req, mock(HttpServletResponse.class), handler("adminOnly"));
        assertThat(ok).isTrue();
    }

    @Test
    void forbidsWhenRoleMismatch() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("role")).thenReturn("viewer");
        StringWriter sink = new StringWriter();
        HttpServletResponse resp = mockResponse(sink);

        boolean ok = interceptor.preHandle(req, resp, handler("adminOnly"));

        assertThat(ok).isFalse();
        verify(resp).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertThat(sink.toString()).contains("4005");
    }

    @Test
    void unauthorizedWhenNoRoleAttribute() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("role")).thenReturn(null);
        StringWriter sink = new StringWriter();
        HttpServletResponse resp = mockResponse(sink);

        boolean ok = interceptor.preHandle(req, resp, handler("adminOnly"));

        assertThat(ok).isFalse();
        verify(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void allowsUnannotatedHandler() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        boolean ok = interceptor.preHandle(req, mock(HttpServletResponse.class), handler("unrestricted"));
        assertThat(ok).isTrue();
    }

    @Test
    void allowsNonHandlerMethod() throws Exception {
        boolean ok = interceptor.preHandle(
                mock(HttpServletRequest.class), mock(HttpServletResponse.class), new Object());
        assertThat(ok).isTrue();
    }
}
