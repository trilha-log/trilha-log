package io.github.trilhalog.sample.dto;

import io.github.trilhalog.logging.Sensitive;

public record LoginRequest(String usuario, @Sensitive String senha) {
}
