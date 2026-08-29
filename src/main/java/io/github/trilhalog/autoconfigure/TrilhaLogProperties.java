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
        /**
         * Cabecalhos HTTP inspecionados em ordem para reutilizar um traceId externo.
         * O primeiro cabecalho presente e nao vazio e usado; caso nenhum exista, um
         * novo UUID e gerado. Suporta formato W3C traceparent (extrai o traceId de 32
         * chars), X-Request-ID, X-Correlation-ID e X-B3-TraceId.
         * Lista vazia desativa a propagacao e sempre gera novo traceId.
         */
        private List<String> incomingTraceHeaders = List.of(
                "traceparent", "X-Request-ID", "X-Correlation-ID", "X-B3-TraceId"
        );
        /**
         * Comprimento do traceId gerado. 0 usa o UUID completo (32 chars hex, sem hifens).
         * Valores positivos truncam para os primeiros N chars. Nao afeta traceIds propagados
         * de cabecalhos de entrada.
         */
        private int traceIdLength = 0;
        /** Opt-in: loga o corpo da requisicao com mascaramento automatico de campos sensiveis. Default false. */
        private boolean logRequestBody = false;
        /** Limite em bytes do corpo logado; trunca payloads grandes. */
        private int maxBodyLogBytes = 2048;

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

        public List<String> getIncomingTraceHeaders() {
            return incomingTraceHeaders;
        }

        public void setIncomingTraceHeaders(List<String> incomingTraceHeaders) {
            this.incomingTraceHeaders = incomingTraceHeaders;
        }

        public int getTraceIdLength() {
            return traceIdLength;
        }

        public void setTraceIdLength(int traceIdLength) {
            this.traceIdLength = traceIdLength;
        }

        public boolean isLogRequestBody() {
            return logRequestBody;
        }

        public void setLogRequestBody(boolean logRequestBody) {
            this.logRequestBody = logRequestBody;
        }

        public int getMaxBodyLogBytes() {
            return maxBodyLogBytes;
        }

        public void setMaxBodyLogBytes(int maxBodyLogBytes) {
            this.maxBodyLogBytes = maxBodyLogBytes;
        }
    }

    public static class Aspect {
        /** Liga/desliga o LogExecutionAspect (@LogExecution). */
        private boolean enabled = true;
        /**
         * Numero maximo de frames mantidos no callChain do MDC. 0 sem limite.
         * Quando ultrapassado, os frames mais antigos sao substituidos por "... >"
         * para manter o contexto mais recente (onde o erro efetivamente ocorre).
         */
        private int maxCallChainFrames = 0;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxCallChainFrames() {
            return maxCallChainFrames;
        }

        public void setMaxCallChainFrames(int maxCallChainFrames) {
            this.maxCallChainFrames = maxCallChainFrames;
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
