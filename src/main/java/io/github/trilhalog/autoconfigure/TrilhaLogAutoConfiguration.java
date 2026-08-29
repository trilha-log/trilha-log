package io.github.trilhalog.autoconfigure;

import io.github.trilhalog.aspect.LogExecutionAspect;
import io.github.trilhalog.logging.AppLog;
import io.github.trilhalog.logging.LogMaskingUtil;
import io.github.trilhalog.web.RequestCorrelationFilter;
import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguracao do trilha-log. Cada bean e {@code @ConditionalOnMissingBean}
 * para o consumidor poder sobrescrever, e o filtro web so entra se houver
 * Servlet API no classpath (aplicacao pode nao ser uma app web).
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

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(Filter.class)
    @ConditionalOnProperty(prefix = "trilha-log.correlation", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RequestCorrelationFilter requestCorrelationFilter(TrilhaLogProperties properties) {
        return new RequestCorrelationFilter(properties.getCorrelation().isLogIp());
    }
}
