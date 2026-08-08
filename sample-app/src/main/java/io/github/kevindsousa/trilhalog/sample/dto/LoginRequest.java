package io.github.kevindsousa.trilhalog.sample.dto;

import io.github.kevindsousa.trilhalog.logging.Sensitive;

public record LoginRequest(String usuario, @Sensitive String senha) {
}
