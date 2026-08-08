package io.github.trilhalog.logging.jpa;

import jakarta.persistence.Entity;

/**
 * Isolada em classe propria de proposito: so e carregada (e so precisa que
 * jakarta.persistence esteja no classpath) quando {@code isJpaEntity} e
 * efetivamente chamada. O chamador deve checar antes se a classe
 * {@code jakarta.persistence.Entity} existe (capability detection via
 * {@code ClassUtils.isPresent}) para nunca disparar {@code NoClassDefFoundError}
 * em projetos sem JPA no classpath.
 */
public final class JpaEntityDetector {

    private JpaEntityDetector() {
    }

    public static boolean isJpaEntity(Class<?> tipo) {
        return tipo.isAnnotationPresent(Entity.class);
    }
}
