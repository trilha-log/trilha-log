package io.github.trilhalog.aspect;

import io.github.trilhalog.testsupport.CapturaLogAppender;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogExecutionAspectTest {

    private final CapturaLogAppender appender = new CapturaLogAppender();

    @BeforeEach
    void setUp() {
        appender.anexar();
    }

    @AfterEach
    void tearDown() {
        appender.desanexar();
    }

    private <T> T proxy(T target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new LogExecutionAspect());
        return factory.getProxy();
    }

    @LogExecution
    static class ServicoA {
        private final ServicoB servicoB;

        ServicoA(ServicoB servicoB) {
            this.servicoB = servicoB;
        }

        String executar(String nome) {
            return servicoB.processar(nome);
        }
    }

    @LogExecution
    static class ServicoB {
        String processar(String nome) {
            return "ola " + nome;
        }

        String falhar() {
            throw new IllegalStateException("erro proposital");
        }
    }

    @LogExecution
    static class ServicoSensivel {
        String autenticar(String usuario, String senha) {
            return "ok";
        }
    }

    @Test
    void logaEntradaESaidaDoMetodo() {
        ServicoB servico = proxy(new ServicoB());

        String resultado = servico.processar("Kevin");

        assertThat(resultado).isEqualTo("ola Kevin");
        assertThat(appender.contemMensagem("-> ServicoB.processar")).isTrue();
        assertThat(appender.contemMensagem("<- ServicoB.processar")).isTrue();
    }

    @Test
    void acumulaCallChainEmChamadaAninhada() {
        ServicoB servicoBProxy = proxy(new ServicoB());
        ServicoA servicoAProxy = proxy(new ServicoA(servicoBProxy));

        servicoAProxy.executar("Kevin");

        LogEvent entradaServicoB = appender.getEventos().stream()
                .filter(e -> e.getMessage().getFormattedMessage().startsWith("-> ServicoB.processar"))
                .findFirst()
                .orElseThrow();

        String callChain = entradaServicoB.getContextData().getValue("callChain");
        assertThat(callChain).isEqualTo("ServicoA.executar > ServicoB.processar");
    }

    @Test
    void restauraCallChainAoVoltarDaChamadaAninhada() {
        ServicoB servicoBProxy = proxy(new ServicoB());
        ServicoA servicoAProxy = proxy(new ServicoA(servicoBProxy));

        servicoAProxy.executar("Kevin");

        LogEvent saidaServicoA = appender.getEventos().stream()
                .filter(e -> e.getMessage().getFormattedMessage().startsWith("<- ServicoA.executar"))
                .findFirst()
                .orElseThrow();

        String callChain = saidaServicoA.getContextData().getValue("callChain");
        assertThat(callChain).isEqualTo("ServicoA.executar");
    }

    @Test
    void relancaExcecaoOriginalELogaEmError() {
        ServicoB servico = proxy(new ServicoB());

        assertThatThrownBy(servico::falhar)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("erro proposital");

        LogEvent erroEvent = appender.getEventos().stream()
                .filter(e -> e.getLevel() == Level.ERROR)
                .findFirst()
                .orElseThrow();

        assertThat(erroEvent.getThrown()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void mascaraArgumentoSensivelAntesDeLogar() {
        ServicoSensivel servico = proxy(new ServicoSensivel());

        servico.autenticar("kevin", "supersecreta");

        assertThat(appender.getEventos())
                .noneMatch(e -> e.getMessage().getFormattedMessage().contains("supersecreta"));
    }
}
