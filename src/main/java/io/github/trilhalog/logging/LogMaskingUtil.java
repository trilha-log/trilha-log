package io.github.trilhalog.logging;

import io.github.trilhalog.logging.jpa.JpaEntityDetector;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Ponto central de mascaramento de dados sensiveis para log. Usado pelo
 * {@link io.github.trilhalog.aspect.LogExecutionAspect}, pelo
 * {@link AppLog} e por qualquer codigo que precise logar um valor que pode
 * conter dado sensivel.
 * <p>
 * Nunca reflete entidade JPA nem Collection/Map/array: vira placeholder. Essa
 * e a defesa contra {@code LazyInitializationException} em relacao lazy fora de
 * sessao Hibernate ativa e contra dump de colecao grande no log.
 */
public final class LogMaskingUtil {

    private static final Set<String> DEFAULT_KEYWORDS = Set.of(
            "senha", "password", "token", "secret", "apikey", "api-key", "authorization", "chave"
    );

    private static final boolean JPA_PRESENT = ClassUtils.isPresent(
            "jakarta.persistence.Entity", LogMaskingUtil.class.getClassLoader());

    private static volatile Set<String> keywords = DEFAULT_KEYWORDS;

    private LogMaskingUtil() {
    }

    /**
     * Combina as palavras-chave padrao com as extras configuradas via
     * {@code trilha-log.masking.extra-keywords}. Chamado uma vez pela
     * autoconfiguracao na subida da aplicacao.
     */
    public static void configurarPalavrasChave(List<String> extras) {
        if (extras == null || extras.isEmpty()) {
            keywords = DEFAULT_KEYWORDS;
            return;
        }
        Set<String> combinadas = new HashSet<>(DEFAULT_KEYWORDS);
        for (String extra : extras) {
            if (extra != null && !extra.isBlank()) {
                combinadas.add(extra.toLowerCase(Locale.ROOT));
            }
        }
        keywords = Set.copyOf(combinadas);
    }

    public static Object mascarar(String nome, Object valor) {
        if (valor == null) {
            return null;
        }
        if (isNomeSensivel(nome)) {
            return "***";
        }
        if (isTipoSimples(valor)) {
            return valor;
        }
        Class<?> tipo = valor.getClass();
        if (JPA_PRESENT && JpaEntityDetector.isJpaEntity(tipo)) {
            return tipo.getSimpleName() + "(entidade)";
        }
        if (valor instanceof Collection || valor instanceof Map || tipo.isArray()) {
            return tipo.getSimpleName();
        }
        return mascararCampos(valor);
    }

    private static boolean isNomeSensivel(String nome) {
        if (nome == null) {
            return false;
        }
        String nomeLower = nome.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (nomeLower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTipoSimples(Object valor) {
        Class<?> tipo = valor.getClass();
        return tipo.isPrimitive()
                || valor instanceof CharSequence
                || valor instanceof Number
                || valor instanceof Boolean
                || tipo.isEnum()
                || valor instanceof UUID;
    }

    private static String mascararCampos(Object valor) {
        Class<?> tipo = valor.getClass();
        StringJoiner joiner = new StringJoiner(", ", tipo.getSimpleName() + "{", "}");
        for (Field field : tipo.getDeclaredFields()) {
            if (field.isSynthetic()) {
                continue;
            }
            field.setAccessible(true);
            Object valorCampo;
            try {
                valorCampo = field.get(valor);
            } catch (IllegalAccessException e) {
                valorCampo = "<inacessivel>";
            }
            Object valorMascarado = field.isAnnotationPresent(Sensitive.class)
                    ? "***"
                    : mascarar(field.getName(), valorCampo);
            joiner.add(field.getName() + "=" + valorMascarado);
        }
        return joiner.toString();
    }
}
