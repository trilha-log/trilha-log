package io.github.trilhalog.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "trilha-log")
public class TrilhaLogProperties {

    private final Correlation correlation = new Correlation();
    private final Aspect aspect = new Aspect();
    private final Masking masking = new Masking();

    public Correlation getCorrelation() {
        return correlation;
    }

    public Aspect getAspect() {
        return aspect;
    }

    public Masking getMasking() {
        return masking;
    }

    public static class Correlation {
        /** Liga/desliga o RequestCorrelationFilter (traceId no MDC). */
        private boolean enabled = true;
        /** Inclui o IP do cliente no log de cada requisicao. Desabilitar em ambientes com requisitos LGPD/GDPR. */
        private boolean logIp = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isLogIp() {
            return logIp;
        }

        public void setLogIp(boolean logIp) {
            this.logIp = logIp;
        }
    }

    public static class Aspect {
        /** Liga/desliga o LogExecutionAspect (@LogExecution). */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Masking {
        /** Palavras-chave adicionais, somadas as padrao (senha, password, token, secret, apikey, api-key, authorization, chave). */
        private List<String> extraKeywords = new ArrayList<>();

        public List<String> getExtraKeywords() {
            return extraKeywords;
        }

        public void setExtraKeywords(List<String> extraKeywords) {
            this.extraKeywords = extraKeywords;
        }
    }
}
