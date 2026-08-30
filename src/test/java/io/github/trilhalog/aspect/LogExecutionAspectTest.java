package io.github.trilhalog.aspect;

import io.github.trilhalog.logging.Sensitive;
import io.github.trilhalog.testsupport.CapturaLogAppender;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
        return proxy(target, new LogExecutionAspect());
    }

    private <T> T proxy(T target, LogExecutionAspect aspecto) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspecto);
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

    @LogExecution
    static class ServicoC {
        private final ServicoA servicoA;

        ServicoC(ServicoA servicoA) {
            this.servicoA = servicoA;
        }

        String iniciar(String nome) {
            return servicoA.executar(nome);
        }
    }

    @LogExecution
    static class ServicoComParametroSensivel {

        String mudarSenha(String login, @Sensitive String novoValor) {
            return "ok";
        }
    }

    /**
     * Retorno concreto usado para validar que tipos nao excluidos
     * continuam sendo logados normalmente.
     */
    @LogExecution(excludeReturnTypes = {byte[].class})
    static class ServicoComRetornoNormal {

        String retornarTexto() {
            return "texto-normal";
        }
    }

    /**
     * Retorno byte[] usado para validar exclusao de arrays de tipos primitivos.
     */
    @LogExecution(excludeReturnTypes = {byte[].class})
    static class ServicoComRetornoByteArray {

        byte[] retornarBytes() {
            return new byte[]{1, 2, 3, 4};
        }
    }

    /**
     * Classe abstrata usada para validar isAssignableFrom.
     */
    abstract static class RetornoAbstrato {
    }

    static class RetornoConcreto extends RetornoAbstrato {
    }

    @LogExecution(excludeReturnTypes = {RetornoAbstrato.class})
    static class ServicoComRetornoAbstrato {

        RetornoAbstrato retornarAbstrato() {
            return new RetornoConcreto();
        }
    }

    /**
     * Interface usada para validar isAssignableFrom contra implementacoes concretas.
     */
    interface RetornoInterface {
    }

    static class RetornoInterfaceImpl implements RetornoInterface {
    }

    @LogExecution(excludeReturnTypes = {RetornoInterface.class})
    static class ServicoComRetornoInterface {

        RetornoInterface retornarInterface() {
            return new RetornoInterfaceImpl();
        }
    }

    @LogExecution(excludeReturnTypes = {byte[].class, InputStream.class})
    static class ServicoComMultiplosTiposExcluidos {

        byte[] retornarBytes() {
            return new byte[]{10, 20, 30};
        }

        InputStream retornarStream() {
            return new ByteArrayInputStream(new byte[]{1, 2, 3});
        }

        String retornarTexto() {
            return "texto";
        }
    }

    @LogExecution(excludeReturnTypes = {String.class})
    static class ServicoComRetornoNulo {

        String retornarNulo() {
            return null;
        }
    }

    @LogExecution(logReturn = false)
    static class ServicoSemLogDeRetorno {

        String retornarTexto() {
            return "nao-deve-aparecer";
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
                .filter(e -> e.getMessage().getFormattedMessage()
                        .startsWith("-> ServicoB.processar"))
                .findFirst()
                .orElseThrow();

        String callChain = entradaServicoB.getContextData().getValue("callChain");

        assertThat(callChain)
                .isEqualTo("ServicoA.executar > ServicoB.processar");
    }

    @Test
    void restauraCallChainAoVoltarDaChamadaAninhada() {
        ServicoB servicoBProxy = proxy(new ServicoB());
        ServicoA servicoAProxy = proxy(new ServicoA(servicoBProxy));

        servicoAProxy.executar("Kevin");

        LogEvent saidaServicoA = appender.getEventos().stream()
                .filter(e -> e.getMessage().getFormattedMessage()
                        .startsWith("<- ServicoA.executar"))
                .findFirst()
                .orElseThrow();

        String callChain = saidaServicoA.getContextData().getValue("callChain");

        assertThat(callChain)
                .isEqualTo("ServicoA.executar");
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

        assertThat(erroEvent.getThrown())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void mascaraArgumentoSensivelAntesDeLogar() {
        ServicoSensivel servico = proxy(new ServicoSensivel());

        servico.autenticar("kevin", "supersecreta");

        assertThat(appender.getEventos())
                .noneMatch(e -> e.getMessage().getFormattedMessage()
                        .contains("supersecreta"));
    }

    @Test
    void truncaCallChainQuandoLimiteAtingido() {
        LogExecutionAspect aspecto = new LogExecutionAspect(2);

        ServicoB servicoB = proxy(new ServicoB(), aspecto);
        ServicoA servicoA = proxy(new ServicoA(servicoB), aspecto);
        ServicoC servicoC = proxy(new ServicoC(servicoA), aspecto);

        servicoC.iniciar("Kevin");

        LogEvent entradaServicoB = appender.getEventos().stream()
                .filter(e -> e.getMessage().getFormattedMessage()
                        .startsWith("-> ServicoB.processar"))
                .findFirst()
                .orElseThrow();

        String callChain = entradaServicoB.getContextData().getValue("callChain");

        assertThat(callChain)
                .isEqualTo("... > ServicoA.executar > ServicoB.processar");
    }

    @Test
    void restauraCallChainCorretamenteAposLimiteTingido() {
        LogExecutionAspect aspecto = new LogExecutionAspect(2);

        ServicoB servicoB = proxy(new ServicoB(), aspecto);
        ServicoA servicoA = proxy(new ServicoA(servicoB), aspecto);
        ServicoC servicoC = proxy(new ServicoC(servicoA), aspecto);

        servicoC.iniciar("Kevin");

        LogEvent saidaServicoC = appender.getEventos().stream()
                .filter(e -> e.getMessage().getFormattedMessage()
                        .startsWith("<- ServicoC.iniciar"))
                .findFirst()
                .orElseThrow();

        String callChain = saidaServicoC.getContextData().getValue("callChain");

        assertThat(callChain)
                .isEqualTo("ServicoC.iniciar");
    }

    @Test
    void mascaraParametroAnotadoComSensitive() {
        ServicoComParametroSensivel servico = proxy(new ServicoComParametroSensivel());

        servico.mudarSenha("kevin", "supersecreta");

        assertThat(appender.getEventos())
                .noneMatch(e -> e.getMessage().getFormattedMessage()
                        .contains("supersecreta"));
    }

    @Test
    void logaRetornoNormalQuandoNenhumTipoEstaExcluido() {
        ServicoComRetornoNormal servico = proxy(new ServicoComRetornoNormal());

        String resultado = servico.retornarTexto();

        assertThat(resultado).isEqualTo("texto-normal");

        assertThat(appender.getEventos())
                .anyMatch(e -> {
                    String mensagem = e.getMessage().getFormattedMessage();
                    return mensagem.contains("<- ServicoComRetornoNormal.retornarTexto")
                            && mensagem.contains("return=texto-normal");
                });
    }

    @Test
    void naoLogaValorDoRetornoQuandoTipoPrimitivoArrayEstaExcluido() {
        ServicoComRetornoByteArray servico = proxy(new ServicoComRetornoByteArray());

        byte[] resultado = servico.retornarBytes();

        assertThat(resultado).containsExactly(1, 2, 3, 4);

        assertThat(appender.getEventos())
                .anyMatch(e -> {
                    String mensagem = e.getMessage().getFormattedMessage();

                    return mensagem.equals("<- ServicoComRetornoByteArray.retornarBytes");
                });
    }

    @Test
    void naoLogaRetornoQuandoClasseAbstrataEstaExcluida() {
        ServicoComRetornoAbstrato servico = proxy(new ServicoComRetornoAbstrato());

        RetornoAbstrato resultado = servico.retornarAbstrato();

        assertThat(resultado).isInstanceOf(RetornoConcreto.class);

        assertThat(appender.getEventos())
                .anyMatch(e -> {
                    String mensagem = e.getMessage().getFormattedMessage();

                    return mensagem.equals("<- ServicoComRetornoAbstrato.retornarAbstrato");
                });
    }

    @Test
    void naoLogaRetornoQuandoInterfaceEstaExcluida() {
        ServicoComRetornoInterface servico = proxy(new ServicoComRetornoInterface());

        RetornoInterface resultado = servico.retornarInterface();

        assertThat(resultado).isInstanceOf(RetornoInterfaceImpl.class);

        assertThat(appender.getEventos())
                .anyMatch(e -> {
                    String mensagem = e.getMessage().getFormattedMessage();

                    return mensagem.equals("<- ServicoComRetornoInterface.retornarInterface");
                });
    }

    @Test
    void excluiMultiplosTiposDeRetorno() {
        ServicoComMultiplosTiposExcluidos servico =
                proxy(new ServicoComMultiplosTiposExcluidos());

        servico.retornarBytes();
        servico.retornarStream();
        servico.retornarTexto();

        assertThat(appender.getEventos())
                .anyMatch(e -> e.getMessage().getFormattedMessage()
                        .equals("<- ServicoComMultiplosTiposExcluidos.retornarBytes"));

        assertThat(appender.getEventos())
                .anyMatch(e -> e.getMessage().getFormattedMessage()
                        .equals("<- ServicoComMultiplosTiposExcluidos.retornarStream"));

        assertThat(appender.getEventos())
                .anyMatch(e -> {
                    String mensagem = e.getMessage().getFormattedMessage();

                    return mensagem.contains("<- ServicoComMultiplosTiposExcluidos.retornarTexto")
                            && mensagem.contains("return=texto");
                });
    }

    @Test
    void naoExcluiRetornoNuloMesmoQuandoStringEstaExcluida() {
        ServicoComRetornoNulo servico = proxy(new ServicoComRetornoNulo());

        String resultado = servico.retornarNulo();

        assertThat(resultado).isNull();

        assertThat(appender.getEventos())
                .anyMatch(e -> {
                    String mensagem = e.getMessage().getFormattedMessage();

                    return mensagem.contains("<- ServicoComRetornoNulo.retornarNulo")
                            && mensagem.contains("return=null");
                });
    }

    @Test
    void logReturnFalseContinuaSemLogarValorDoRetorno() {
        ServicoSemLogDeRetorno servico = proxy(new ServicoSemLogDeRetorno());

        String resultado = servico.retornarTexto();

        assertThat(resultado).isEqualTo("nao-deve-aparecer");

        assertThat(appender.getEventos())
                .anyMatch(e -> {
                    String mensagem = e.getMessage().getFormattedMessage();

                    return mensagem.equals("<- ServicoSemLogDeRetorno.retornarTexto");
                });
    }
}
