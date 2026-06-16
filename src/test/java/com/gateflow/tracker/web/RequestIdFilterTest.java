package com.gateflow.tracker.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    private String runWith(String incomingHeader) throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(req.getHeader(RequestIdFilter.HEADER)).thenReturn(incomingHeader);

        AtomicReference<String> mdcDuringChain = new AtomicReference<>();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            mdcDuringChain.set(MDC.get(RequestIdFilter.MDC_KEY));
            return null;
        }).when(chain).doFilter(req, resp);

        filter.doFilter(req, resp, chain);

        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
        verify(chain).doFilter(req, resp);
        return mdcDuringChain.get();
    }

    @Test
    void generatesIdWhenHeaderAbsent() throws Exception {
        assertThat(runWith(null)).isNotBlank();
    }

    @Test
    void propagatesValidIncomingId() throws Exception {
        assertThat(runWith("abc-123_XYZ")).isEqualTo("abc-123_XYZ");
    }

    @Test
    void replacesMalformedIncomingId() throws Exception {
        assertThat(runWith("bad id!")).isNotBlank().doesNotContain(" ");
    }

    @Test
    void setsResponseHeader() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(req.getHeader(RequestIdFilter.HEADER)).thenReturn("req-1");
        filter.doFilter(req, resp, mock(FilterChain.class));
        verify(resp).setHeader(eq(RequestIdFilter.HEADER), eq("req-1"));
    }

    @Test
    void sanitizeRejectsOverlongAndInvalid() {
        assertThat(RequestIdFilter.sanitize("ok-1")).isEqualTo("ok-1");
        assertThat(RequestIdFilter.sanitize("has space")).isNull();
        assertThat(RequestIdFilter.sanitize("a".repeat(65))).isNull();
        assertThat(RequestIdFilter.sanitize("")).isNull();
    }
}
