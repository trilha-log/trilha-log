package io.github.trilhalog.tracing;

/**
 * Ponte isolada para acesso ao Micrometer Tracing. Esta classe referencia
 * tipos de {@code io.micrometer.tracing} diretamente, por isso so deve ser
 * carregada quando {@code MICROMETER_TRACING_PRESENT} for verdadeiro — caso
 * contrario o JVM lancaria {@link NoClassDefFoundError}.
 * <p>
 * Aceita {@link Object} para evitar que o chamador (RequestCorrelationFilter)
 * importe {@code io.micrometer.tracing.Tracer} diretamente.
 */
public final class MicrometerTracingBridge {

    private MicrometerTracingBridge() {}

    public static String traceId(Object tracer) {
        io.micrometer.tracing.Span span = ((io.micrometer.tracing.Tracer) tracer).currentSpan();
        return span != null ? span.context().traceId() : null;
    }

    public static String spanId(Object tracer) {
        io.micrometer.tracing.Span span = ((io.micrometer.tracing.Tracer) tracer).currentSpan();
        return span != null ? span.context().spanId() : null;
    }
}
