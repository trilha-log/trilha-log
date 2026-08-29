package io.github.trilhalog.logging;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Helper para log de eventos de negocio no meio de um metodo, onde
 * {@link io.github.trilhalog.aspect.LogExecution} nao alcanca —
 * o AOP so intercepta borda de entrada/saida de metodo, nao uma condicao no
 * meio da logica.
 * <p>
 * As sobrecargas com {@code Map<String, ?>} adicionam os campos ao MDC
 * temporariamente, antes de emitir o log, e os removem no finally. O template
 * JSON le todos os campos do MDC via {@code $resolver: mdc}, entao os campos
 * aparecem como propriedades independentes no evento — pesquisaveis e indexaveis
 * em Elasticsearch/OpenSearch/Loki.
 * <p>
 * Cuidado: nunca passar {@code Map.of(...)} como {@code context} de objeto. O
 * mascaramento trata Map como "nao reflete" e o resultado vira so o nome da
 * classe do Map, escondendo o dado. Para contexto com mais de um campo, use um
 * record local pequeno — a reflection rasa do {@link LogMaskingUtil} trata
 * records normalmente.
 */
@Component
public class AppLog {

    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    // --- API sem campos MDC extras ---

    public void debug(String message, Object context) {
        Class<?> caller = STACK_WALKER.getCallerClass();
        LoggerFactory.getLogger(caller).debug(formatar(message, context));
    }

    public void info(String message, Object context) {
        Class<?> caller = STACK_WALKER.getCallerClass();
        LoggerFactory.getLogger(caller).info(formatar(message, context));
    }

    public void warn(String message, Object context) {
        Class<?> caller = STACK_WALKER.getCallerClass();
        LoggerFactory.getLogger(caller).warn(formatar(message, context));
    }

    public void error(String message, Object context, Throwable ex) {
        Class<?> caller = STACK_WALKER.getCallerClass();
        LoggerFactory.getLogger(caller).error(formatar(message, context), ex);
    }

    // --- API com campos MDC extras ---

    public void debug(String message, Map<String, ?> campos) {
        Class<?> caller = STACK_WALKER.getCallerClass();
        comCamposMdc(campos, () -> LoggerFactory.getLogger(caller).debug(message));
    }

    public void debug(String message, Object context, Map<String, ?> campos) {
        Class<?> caller = STACK_WALKER.getCallerClass();
        comCamposMdc(campos, () -> LoggerFactory.getLogger(caller).debug(formatar(message, context)));
    }

    public void info(String message, Map<String, ?> campos) {
        Class<?> caller = STACK_WALKER.getCallerClass();
        comCamposMdc(campos, () -> LoggerFactory.getLogger(caller).info(message));
    }

    public void info(String message, Object context, Map<String, ?> campos) {
        Class<?> caller = STACK_WALKER.getCallerClass();
        comCamposMdc(campos, () -> LoggerFactory.getLogger(caller).info(formatar(message, context)));
    }

    public void warn(String message, Map<String, ?> campos) {
        Class<?> caller = STACK_WALKER.getCallerClass();
        comCamposMdc(campos, () -> LoggerFactory.getLogger(caller).warn(message));
    }

    public void warn(String message, Object context, Map<String, ?> campos) {
        Class<?> caller = STACK_WALKER.getCallerClass();
        comCamposMdc(campos, () -> LoggerFactory.getLogger(caller).warn(formatar(message, context)));
    }

    public void error(String message, Throwable ex, Map<String, ?> campos) {
        Class<?> caller = STACK_WALKER.getCallerClass();
        comCamposMdc(campos, () -> LoggerFactory.getLogger(caller).error(message, ex));
    }

    public void error(String message, Object context, Throwable ex, Map<String, ?> campos) {
        Class<?> caller = STACK_WALKER.getCallerClass();
        comCamposMdc(campos, () -> LoggerFactory.getLogger(caller).error(formatar(message, context), ex));
    }

    // --- helpers privados ---

    private void comCamposMdc(Map<String, ?> campos, Runnable log) {
        campos.forEach((k, v) -> MDC.put(k, v != null ? v.toString() : null));
        try {
            log.run();
        } finally {
            campos.keySet().forEach(MDC::remove);
        }
    }

    private String formatar(String message, Object context) {
        if (context == null) {
            return message;
        }
        return message + " — " + LogMaskingUtil.mascarar("context", context);
    }
}
