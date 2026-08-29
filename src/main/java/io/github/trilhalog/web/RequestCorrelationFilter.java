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
import java.util.UUID;

/**
 * Gera um traceId curto por requisicao e coloca no MDC antes de chamar a
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

    public RequestCorrelationFilter(boolean logIp) {
        this.logIp = logIp;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
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
}
