package io.github.trilhalog.web;

import io.github.trilhalog.testsupport.CapturaLogAppender;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestCorrelationFilterTest {

    private static final String IP_TESTE = "192.168.1.100";

    @Mock
    private Tracer tracer;
    @Mock
    private Span span;
    @Mock
    private TraceContext traceContext;

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter(true, List.of(), null);
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
        RequestCorrelationFilter filterSemIp = new RequestCorrelationFilter(false, List.of(), null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ping");
        request.setRemoteAddr(IP_TESTE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        filterSemIp.doFilter(request, response, (req, res) -> {});

        assertThat(appender.getEventos())
                .noneMatch(e -> e.getMessage().getFormattedMessage().contains(IP_TESTE));
    }

    @Test
    void usaXRequestIdComoTraceIdQuandoPresente() throws Exception {
        RequestCorrelationFilter filterComCabecalho = new RequestCorrelationFilter(true, List.of("X-Request-ID"), null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ping");
        request.addHeader("X-Request-ID", "meu-trace-externo");
        MockHttpServletResponse response = new MockHttpServletResponse();

        StringBuilder traceIdCapturado = new StringBuilder();
        filterComCabecalho.doFilter(request, response,
                (req, res) -> traceIdCapturado.append(MDC.get(RequestCorrelationFilter.TRACE_ID_MDC_KEY)));

        assertThat(traceIdCapturado.toString()).isEqualTo("meu-trace-externo");
    }

    @Test
    void extraiTraceIdDoFormatoW3cTraceparent() throws Exception {
        RequestCorrelationFilter filterComCabecalho = new RequestCorrelationFilter(true, List.of("traceparent"), null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ping");
        request.addHeader("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
        MockHttpServletResponse response = new MockHttpServletResponse();

        StringBuilder traceIdCapturado = new StringBuilder();
        filterComCabecalho.doFilter(request, response,
                (req, res) -> traceIdCapturado.append(MDC.get(RequestCorrelationFilter.TRACE_ID_MDC_KEY)));

        assertThat(traceIdCapturado.toString()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }

    @Test
    void geraNovoTraceIdQuandoNenhumCabecalhoEstaPresente() throws Exception {
        RequestCorrelationFilter filterComCabecalho = new RequestCorrelationFilter(true, List.of("X-Request-ID"), null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();

        StringBuilder traceIdCapturado = new StringBuilder();
        filterComCabecalho.doFilter(request, response,
                (req, res) -> traceIdCapturado.append(MDC.get(RequestCorrelationFilter.TRACE_ID_MDC_KEY)));

        assertThat(traceIdCapturado.toString())
                .isNotBlank()
                .hasSize(8);
    }

    @Test
    void micrometerTraceIdEhUsadoQuandoSpanAtivo() throws Exception {
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("4bf92f3577b34da6a3ce929d0e0e4736");

        RequestCorrelationFilter filterComTracer = new RequestCorrelationFilter(false, List.of(), tracer);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();

        StringBuilder traceCapturado = new StringBuilder();
        filterComTracer.doFilter(request, response,
                (req, res) -> traceCapturado.append(MDC.get(RequestCorrelationFilter.TRACE_ID_MDC_KEY)));

        assertThat(traceCapturado.toString()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }

    @Test
    void spanIdEPopuladoNoMdcQuandoMicrometerAtivo() throws Exception {
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("4bf92f3577b34da6a3ce929d0e0e4736");
        when(traceContext.spanId()).thenReturn("00f067aa0ba902b7");

        RequestCorrelationFilter filterComTracer = new RequestCorrelationFilter(false, List.of(), tracer);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();

        StringBuilder spanCapturado = new StringBuilder();
        filterComTracer.doFilter(request, response,
                (req, res) -> spanCapturado.append(MDC.get(RequestCorrelationFilter.SPAN_ID_MDC_KEY)));

        assertThat(spanCapturado.toString()).isEqualTo("00f067aa0ba902b7");
        assertThat(MDC.get(RequestCorrelationFilter.SPAN_ID_MDC_KEY)).isNull();
    }

    @Test
    void fallbackParaUuidQuandoSpanEhNulo() throws Exception {
        when(tracer.currentSpan()).thenReturn(null);

        RequestCorrelationFilter filterComTracer = new RequestCorrelationFilter(false, List.of(), tracer);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();

        StringBuilder traceCapturado = new StringBuilder();
        filterComTracer.doFilter(request, response,
                (req, res) -> traceCapturado.append(MDC.get(RequestCorrelationFilter.TRACE_ID_MDC_KEY)));

        assertThat(traceCapturado.toString()).hasSize(8);
    }
}
