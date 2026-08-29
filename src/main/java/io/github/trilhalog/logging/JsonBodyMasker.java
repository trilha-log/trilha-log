package io.github.trilhalog.logging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Classe isolada que acessa Jackson diretamente. So deve ser carregada quando
 * {@code JACKSON_PRESENT} for verdadeiro em {@link LogMaskingUtil}.
 */
final class JsonBodyMasker {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private JsonBodyMasker() {}

    static String mascarar(String json) throws Exception {
        Map<String, Object> mapa = MAPPER.readValue(json, MAP_TYPE);
        Map<String, Object> mascarado = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : mapa.entrySet()) {
            mascarado.put(entry.getKey(), LogMaskingUtil.mascarar(entry.getKey(), entry.getValue()));
        }
        return MAPPER.writeValueAsString(mascarado);
    }
}
