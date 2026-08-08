package io.github.trilhalog.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

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
}
