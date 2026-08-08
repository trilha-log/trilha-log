package io.github.kevindsousa.trilhalog.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um campo como sensivel: {@link LogMaskingUtil} sempre mascara o valor,
 * independente do nome do campo bater alguma palavra-chave conhecida.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {
}
