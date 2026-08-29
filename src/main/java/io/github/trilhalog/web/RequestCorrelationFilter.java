package io.github.trilhalog.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Gera um traceId por requisicao e coloca no MDC antes de chamar a
 * cadeia de filtros, para que ja exista quando filtros de seguranca da
 * aplicacao consumidora logarem. {@code @Order(HIGHEST_PRECEDENCE)} garante
 * que roda antes de qualquer filtro de seguranca.
 * <p>
 * Nunca loga corpo ou parametros da requisicao, em nenhum profile — risco de
 * vazar dado sensivel sem mascaramento.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_MDC_KEY = "traceId";

    private final boolean logIp;
    private final List<String> incomingTraceHeaders;

    public RequestCorrelationFilter(boolean logIp, List<String> incomingTraceHeaders) {
        this.logIp = logIp;
        this.incomingTraceHeaders = incomingTraceHeaders;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = resolverTraceId(request);
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        long inicio = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duracaoMs = System.currentTimeMillis() - inicio;
            logar(request, response, duracaoMs);
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    private void logar(HttpServletRequest request, HttpServletResponse response, long duracaoMs) {
        if (logIp) {
            log.info("{} {} status={} duracaoMs={} ip={}",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), duracaoMs, request.getRemoteAddr());
        } else {
            log.info("{} {} status={} duracaoMs={}",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), duracaoMs);
        }
    }

    private String resolverTraceId(HttpServletRequest request) {
        for (String header : incomingTraceHeaders) {
            String valor = request.getHeader(header);
            if (valor != null && !valor.isBlank()) {
                return extrairTraceId(valor);
            }
        }
        return UUID.randomUUID().toString().substring(0, 8);
    }

    // Extrai o traceId de 32 chars do formato W3C traceparent
    // ("00-<traceId>-<spanId>-<flags>"), ou usa o valor direto para outros formatos.
    private static String extrairTraceId(String headerValue) {
        if (headerValue.startsWith("00-") && headerValue.length() >= 35) {
            return headerValue.substring(3, 35);
        }
        return headerValue.length() > 32 ? headerValue.substring(0, 32) : headerValue;
    }
}
