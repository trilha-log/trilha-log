package io.github.kevindsousa.trilhalog.aspect;

import org.slf4j.event.Level;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Habilita log automatico de entrada, saida e excecao para um metodo ou para
 * todos os metodos publicos de uma classe. Anotacao em metodo sobrescreve a
 * anotacao em classe.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecution {

    Level level() default Level.DEBUG;

    boolean logArgs() default true;

    boolean logReturn() default true;

    boolean logException() default true;
}
