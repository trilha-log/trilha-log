package io.github.trilhalog.web;

import io.github.trilhalog.testsupport.CapturaLogAppender;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestCorrelationFilterTest {

    private static final String IP_TESTE = "192.168.1.100";

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter(true);
    private final CapturaLogAppender appender = new CapturaLogAppender();

    @BeforeEach
    void setUp() {
        appender.anexar();
    }

    @AfterEach
    void tearDown() {
        appender.desanexar();
    }

    @Test
    void geraTraceIdNoMdcDuranteACadeiaERemoveDepoisAoFinalizar() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        StringBuilder traceIdDuranteChain = new StringBuilder();
        FilterChain chain = (req, res) -> traceIdDuranteChain.append(MDC.get(RequestCorrelationFilter.TRACE_ID_MDC_KEY));

        filter.doFilter(request, response, chain);

        assertThat(traceIdDuranteChain.toString()).hasSize(8);
        assertThat(MDC.get(RequestCorrelationFilter.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    void removeTraceIdDoMdcMesmoQuandoChainLancaExcecao() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/erro");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            throw new IllegalStateException("falhou");
        };

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(IllegalStateException.class);

        assertThat(MDC.get(RequestCorrelationFilter.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    void logaIpQuandoLogIpAtivado() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ping");
        request.setRemoteAddr(IP_TESTE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(appender.getEventos())
                .anyMatch(e -> e.getMessage().getFormattedMessage().contains(IP_TESTE));
    }

    @Test
    void naoLogaIpQuandoLogIpDesativado() throws Exception {
        RequestCorrelationFilter filterSemIp = new RequestCorrelationFilter(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ping");
        request.setRemoteAddr(IP_TESTE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        filterSemIp.doFilter(request, response, (req, res) -> {});

        assertThat(appender.getEventos())
                .noneMatch(e -> e.getMessage().getFormattedMessage().contains(IP_TESTE));
    }
}
