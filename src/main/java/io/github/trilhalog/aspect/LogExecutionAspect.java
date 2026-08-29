package io.github.trilhalog.aspect;

import io.github.trilhalog.logging.LogMaskingUtil;
import io.github.trilhalog.logging.Sensitive;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Intercepta metodos anotados com {@link LogExecution} (na classe ou no proprio metodo)
 * e loga entrada/saida/excecao, mantendo um breadcrumb de call chain no MDC.
 * <p>
 * Nunca embrulha a excecao capturada: sempre relanca o {@link Throwable} original,
 * para que a stack trace completa chegue a quem trata.
 */
@Aspect
@Component
public class LogExecutionAspect {

    private static final String CALL_CHAIN_MDC_KEY = "callChain";

    @Around("@within(io.github.trilhalog.aspect.LogExecution) || @annotation(io.github.trilhalog.aspect.LogExecution)")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Class<?> targetClass = joinPoint.getTarget() != null ? joinPoint.getTarget().getClass() : method.getDeclaringClass();
        LogExecution config = resolveConfig(method, targetClass);

        Logger logger = LoggerFactory.getLogger(targetClass);
        String frame = targetClass.getSimpleName() + "." + method.getName();

        String previousChain = MDC.get(CALL_CHAIN_MDC_KEY);
        MDC.put(CALL_CHAIN_MDC_KEY, previousChain == null ? frame : previousChain + " > " + frame);

        try {
            logEntry(logger, config, frame, method, joinPoint.getArgs());
            Object result = joinPoint.proceed();
            logExit(logger, config, frame, result);
            return result;
        } catch (Throwable ex) {
            if (config.logException()) {
                logger.atLevel(Level.ERROR)
                        .setCause(ex)
                        .log("x {} lancou {}", frame, ex.getClass().getSimpleName());
            }
            throw ex;
        } finally {
            if (previousChain == null) {
                MDC.remove(CALL_CHAIN_MDC_KEY);
            } else {
                MDC.put(CALL_CHAIN_MDC_KEY, previousChain);
            }
        }
    }

    private void logEntry(Logger logger, LogExecution config, String frame, Method method, Object[] args) {
        if (config.logArgs()) {
            logger.atLevel(config.level()).log("-> {} args={}", frame, maskArgs(method, args));
        } else {
            logger.atLevel(config.level()).log("-> {}", frame);
        }
    }

    private void logExit(Logger logger, LogExecution config, String frame, Object result) {
        if (config.logReturn()) {
            logger.atLevel(config.level()).log("<- {} return={}", frame, LogMaskingUtil.mascarar("return", result));
        } else {
            logger.atLevel(config.level()).log("<- {}", frame);
        }
    }

    private LogExecution resolveConfig(Method method, Class<?> targetClass) {
        LogExecution methodConfig = method.getAnnotation(LogExecution.class);
        if (methodConfig != null) {
            return methodConfig;
        }
        return targetClass.getAnnotation(LogExecution.class);
    }

    private String maskArgs(Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            if (i < parameters.length) {
                Parameter param = parameters[i];
                Object mascarado = param.isAnnotationPresent(Sensitive.class)
                        ? "***"
                        : LogMaskingUtil.mascarar(param.getName(), args[i]);
                sb.append(param.getName()).append("=").append(mascarado);
            } else {
                sb.append(LogMaskingUtil.mascarar("arg" + i, args[i]));
            }
        }
        return sb.append("]").toString();
    }
}
