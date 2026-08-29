package io.github.trilhalog.autoconfigure;

import io.github.trilhalog.aspect.LogExecutionAspect;
import io.github.trilhalog.logging.AppLog;
import io.github.trilhalog.logging.LogMaskingUtil;
import io.github.trilhalog.web.RequestCorrelationFilter;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoconfiguracao do trilha-log. Cada bean e {@code @ConditionalOnMissingBean}
 * para o consumidor poder sobrescrever, e o filtro web so entra se houver
 * Servlet API no classpath (aplicacao pode nao ser uma app web).
 * <p>
 * Quando Micrometer Tracing esta no classpath, a inner class
 * {@link MicrometerTracingFilterConfiguration} e processada primeiro e injeta
 * o {@code Tracer} no filtro. Caso contrario, o bean padrao usa {@code null}.
 */
@AutoConfiguration
@EnableConfigurationProperties(TrilhaLogProperties.class)
public class TrilhaLogAutoConfiguration {

    public TrilhaLogAutoConfiguration(TrilhaLogProperties properties) {
        LogMaskingUtil.configurarPalavrasChave(properties.getMasking().getExtraKeywords());
    }

    @Bean
    @ConditionalOnMissingBean
    public AppLog appLog() {
        return new AppLog();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "trilha-log.aspect", name = "enabled", havingValue = "true", matchIfMissing = true)
    public LogExecutionAspect logExecutionAspect() {
        return new LogExecutionAspect();
    }

    // Fallback: sem Micrometer Tracing ou quando o inner class nao foi processado
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(Filter.class)
    @ConditionalOnProperty(prefix = "trilha-log.correlation", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RequestCorrelationFilter requestCorrelationFilter(TrilhaLogProperties properties) {
        TrilhaLogProperties.Correlation c = properties.getCorrelation();
        return new RequestCorrelationFilter(c.isLogIp(), c.getIncomingTraceHeaders(), null);
    }

    /**
     * Processada antes dos @Bean da classe externa (inner classes tem prioridade).
     * Quando o bean e criado aqui, o @ConditionalOnMissingBean do metodo externo
     * o encontra e pula — evitando duplicatas.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.micrometer.tracing.Tracer")
    static class MicrometerTracingFilterConfiguration {

        @Bean
        @ConditionalOnMissingBean(RequestCorrelationFilter.class)
        @ConditionalOnClass(Filter.class)
        @ConditionalOnProperty(prefix = "trilha-log.correlation", name = "enabled", havingValue = "true", matchIfMissing = true)
        public RequestCorrelationFilter requestCorrelationFilterWithTracing(
                TrilhaLogProperties properties,
                ObjectProvider<io.micrometer.tracing.Tracer> tracerProvider) {
            TrilhaLogProperties.Correlation c = properties.getCorrelation();
            return new RequestCorrelationFilter(
                    c.isLogIp(), c.getIncomingTraceHeaders(), tracerProvider.getIfAvailable());
        }
    }
}
