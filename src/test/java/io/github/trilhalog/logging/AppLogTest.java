package io.github.trilhalog.logging;

import io.github.trilhalog.testsupport.CapturaLogAppender;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AppLogTest {

    private final AppLog appLog = new AppLog();
    private final CapturaLogAppender appender = new CapturaLogAppender();

    @BeforeEach
    void setUp() {
        appender.anexar();
    }

    @AfterEach
    void tearDown() {
        appender.desanexar();
    }

    @Test
    void camposApareceNoMdcDuranteOLog() {
        appLog.info("operacao realizada", Map.of("userId", "abc123", "role", "ADMIN"));

        LogEvent evento = appender.getEventos().stream()
                .filter(e -> e.getMessage().getFormattedMessage().equals("operacao realizada"))
                .findFirst()
                .orElseThrow();

        assertThat((String) evento.getContextData().getValue("userId")).isEqualTo("abc123");
        assertThat((String) evento.getContextData().getValue("role")).isEqualTo("ADMIN");
    }

    @Test
    void camposSaoRemovidosDoMdcAposOLog() {
        appLog.info("operacao realizada", Map.of("userId", "abc123"));

        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    void camposComContextoAparecemJuntosNoLog() {
        record Req(String usuario) {}

        appLog.info("login", new Req("kevin"), Map.of("origem", "web"));

        LogEvent evento = appender.getEventos().stream()
                .filter(e -> e.getMessage().getFormattedMessage().contains("login"))
                .findFirst()
                .orElseThrow();

        assertThat(evento.getMessage().getFormattedMessage()).contains("kevin");
        assertThat((String) evento.getContextData().getValue("origem")).isEqualTo("web");
    }

    @Test
    void camposSaoRemovidosMesmoQuandoLogLancaExcecao() {
        appLog.error("falha critica", new RuntimeException("boom"),
                Map.of("operacao", "pagamento"));

        assertThat(MDC.get("operacao")).isNull();
    }

    @Test
    void apiOriginalSemCamposContinuaFuncionando() {
        appLog.info("sem campos extras", (Object) null);

        assertThat(appender.contemMensagem("sem campos extras")).isTrue();
    }
}
