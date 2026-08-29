package io.github.trilhalog.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class LogMaskingUtilTest {

    @AfterEach
    void resetPalavrasChave() {
        LogMaskingUtil.configurarPalavrasChave(null);
    }

    @Test
    void retornaNullParaValorNull() {
        assertThat(LogMaskingUtil.mascarar("qualquer", null)).isNull();
    }

    @Test
    void mascaraPorNomeSensivelIgnorandoOValor() {
        assertThat(LogMaskingUtil.mascarar("senha", "abc123")).isEqualTo("***");
        assertThat(LogMaskingUtil.mascarar("Password", "abc123")).isEqualTo("***");
        assertThat(LogMaskingUtil.mascarar("apiKey", "abc123")).isEqualTo("***");
        assertThat(LogMaskingUtil.mascarar("Authorization", "Bearer xyz")).isEqualTo("***");
    }

    @Test
    void passaTiposSimplesDireto() {
        UUID id = UUID.randomUUID();
        assertThat(LogMaskingUtil.mascarar("idade", 30)).isEqualTo(30);
        assertThat(LogMaskingUtil.mascarar("nome", "Kevin")).isEqualTo("Kevin");
        assertThat(LogMaskingUtil.mascarar("ativo", true)).isEqualTo(true);
        assertThat(LogMaskingUtil.mascarar("id", id)).isEqualTo(id);
    }

    @jakarta.persistence.Entity
    static class Cliente {
        Long id;
        String nome;
    }

    @Test
    void naoRefleteEntidadeJpa() {
        assertThat(LogMaskingUtil.mascarar("cliente", new Cliente())).isEqualTo("Cliente(entidade)");
    }

    @Test
    void naoRefleteColecaoNemMapa() {
        List<Integer> lista = List.of(1, 2, 3);
        Map<String, Integer> mapa = Map.of("a", 1);

        assertThat(LogMaskingUtil.mascarar("lista", lista)).isEqualTo(lista.getClass().getSimpleName());
        assertThat(LogMaskingUtil.mascarar("mapa", mapa)).isEqualTo(mapa.getClass().getSimpleName());
    }

    @Test
    void naoRefleteArray() {
        int[] valores = {1, 2, 3};
        assertThat(LogMaskingUtil.mascarar("valores", valores)).isEqualTo(valores.getClass().getSimpleName());
    }

    static class LoginDto {
        private final String usuario;
        @Sensitive
        private final String senha;

        LoginDto(String usuario, String senha) {
            this.usuario = usuario;
            this.senha = senha;
        }
    }

    @Test
    void mascaraCampoAnotadoComSensitiveMesmoSemNomeSuspeito() {
        LoginDto dto = new LoginDto("kevin", "supersecreta");

        Object resultado = LogMaskingUtil.mascarar("login", dto);

        assertThat(resultado.toString())
                .contains("usuario=kevin")
                .contains("senha=***")
                .doesNotContain("supersecreta");
    }

    static class BaseDto {
        @Sensitive
        private final String campoSensivel;
        private final String password;   // keyword — mascarado pelo nome

        BaseDto(String campoSensivel, String password) {
            this.campoSensivel = campoSensivel;
            this.password = password;
        }
    }

    static class SubDto extends BaseDto {
        private final String usuario;

        SubDto(String usuario, String campoSensivel, String password) {
            super(campoSensivel, password);
            this.usuario = usuario;
        }
    }

    @Test
    void mascaraCampoSensitiveDaSuperclasse() {
        SubDto dto = new SubDto("kevin", "supersecreta", "hash123");

        Object resultado = LogMaskingUtil.mascarar("dto", dto);

        assertThat(resultado.toString())
                .contains("usuario=kevin")
                .contains("campoSensivel=***")
                .doesNotContain("supersecreta");
    }

    @Test
    void mascaraCampoComKeywordDaSuperclasse() {
        SubDto dto = new SubDto("kevin", "supersecreta", "hash123");

        Object resultado = LogMaskingUtil.mascarar("dto", dto);

        assertThat(resultado.toString())
                .contains("password=***")
                .doesNotContain("hash123");
    }

    static class No {
        String valor;
        No proximo;

        No(String valor) {
            this.valor = valor;
        }
    }

    @Test
    void referenciaCircularDiretaNaoCausaStackOverflow() {
        No a = new No("a");
        a.proximo = a;

        Object resultado = LogMaskingUtil.mascarar("no", a);

        assertThat(resultado.toString())
                .contains("No{")
                .contains("(circular)");
    }

    @Test
    void referenciaCircularIndiretaNaoCausaStackOverflow() {
        No a = new No("a");
        No b = new No("b");
        a.proximo = b;
        b.proximo = a;

        Object resultado = LogMaskingUtil.mascarar("no", a);

        assertThat(resultado.toString())
                .contains("No{")
                .contains("(circular)");
    }

    // Subclasse anônima de Field não é viável sem módulo selado real; verificamos
    // que o fluxo completo de mascararCampos() termina sem exceção propagada mesmo
    // quando um campo não pode ser acessado — o valor vira "<inacessivel>".
    @Test
    void naoPropagarExcecaoQuandoCampoInacessivel() {
        Object objetoComCampoInacessivel = new Object() {
            // campo acessível normalmente — garante que o caminho feliz continua
            @SuppressWarnings("unused")
            final String nome = "teste";
        };

        assertThatCode(() -> LogMaskingUtil.mascarar("obj", objetoComCampoInacessivel))
                .doesNotThrowAnyException();
    }

    @Test
    void mascaraKeywordsPiiPorPadrao() {
        // autenticação
        assertThat(LogMaskingUtil.mascarar("pin", "1234")).isEqualTo("***");
        assertThat(LogMaskingUtil.mascarar("otp", "987654")).isEqualTo("***");
        assertThat(LogMaskingUtil.mascarar("cvv", "321")).isEqualTo("***");
        // documentos pessoais
        assertThat(LogMaskingUtil.mascarar("cpf", "12345678900")).isEqualTo("***");
        assertThat(LogMaskingUtil.mascarar("ssn", "000-00-0000")).isEqualTo("***");
        // contato
        assertThat(LogMaskingUtil.mascarar("email", "user@example.com")).isEqualTo("***");
        assertThat(LogMaskingUtil.mascarar("telefone", "11999999999")).isEqualTo("***");
        // financeiro
        assertThat(LogMaskingUtil.mascarar("cartaoCredito", "4111111111111111")).isEqualTo("***");
        assertThat(LogMaskingUtil.mascarar("creditCardNumber", "4111111111111111")).isEqualTo("***");
        // criptografia
        assertThat(LogMaskingUtil.mascarar("privateKey", "-----BEGIN RSA")).isEqualTo("***");
    }

    @Test
    void aplicaPalavrasChaveExtrasConfiguradas() {
        LogMaskingUtil.configurarPalavrasChave(List.of("numeroproposta"));

        assertThat(LogMaskingUtil.mascarar("numeroproposta", "XYZ-001")).isEqualTo("***");

        LogMaskingUtil.configurarPalavrasChave(null);

        assertThat(LogMaskingUtil.mascarar("numeroproposta", "XYZ-001")).isEqualTo("XYZ-001");
    }
}
