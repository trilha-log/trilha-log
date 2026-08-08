package io.github.trilhalog.logging;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Helper para log de eventos de negocio no meio de um metodo, onde
 * {@link io.github.trilhalog.aspect.LogExecution} nao alcanca —
 * o AOP so intercepta borda de entrada/saida de metodo, nao uma condicao no
 * meio da logica.
 * <p>
 * Cuidado: nunca passar {@code Map.of(...)} como contexto multi-campo. O
 * mascaramento trata Map como "nao reflete" (mesma protecao contra colecao
 * lazy) e o resultado vira so o nome da classe do Map, escondendo o dado que
 * voce queria logar. Para contexto com mais de um campo, use um record local
 * pequeno — a reflection rasa do {@link LogMaskingUtil} trata records normalmente.
 */
@Component
public class AppLog {

    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

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

    private String formatar(String message, Object context) {
        if (context == null) {
            return message;
        }
        return message + " — " + LogMaskingUtil.mascarar("context", context);
    }
}
