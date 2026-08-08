# trilha-log

Starter Spring Boot de logging estruturado: correlação de requisição via `traceId`, log automático de entrada/saída/exceção de método via AOP (com breadcrumb de call chain), mascaramento de dados sensíveis e um helper de log de eventos de negócio. Motor de logging é Log4j2, com saída em JSON estruturado em produção.

Extraído de uma arquitetura validada em produção (dev e prod), generalizada aqui para ser reutilizável em qualquer projeto Spring Boot 3.x novo: adiciona a dependência, ganha essa base pronta, customiza só o necessário.

## O que a lib entrega

- **`RequestCorrelationFilter`** — gera um `traceId` curto por requisição, coloca no MDC antes de qualquer filtro de segurança rodar (`@Order(HIGHEST_PRECEDENCE)`) e loga método, URI, status, duração e IP ao final. Nunca loga corpo ou parâmetros da requisição.
- **`@LogExecution` + `LogExecutionAspect`** — loga entrada, saída e exceção de método via AOP, com argumentos e retorno mascarados. Mantém um breadcrumb de call chain no MDC (`ServicoA.metodo > ServicoB.metodo`) acumulando em chamadas aninhadas. Nunca embrulha a exceção capturada — sempre relança o `Throwable` original.
- **`@Sensitive` + `LogMaskingUtil`** — mascaramento central de dados sensíveis. Nunca reflete entidade JPA nem `Collection`/`Map`/array (evita `LazyInitializationException` e dump de coleção grande); campos marcados `@Sensitive` ou com nome batendo uma palavra-chave conhecida (`senha`, `password`, `token`, `secret`, `apikey`, `api-key`, `authorization`, `chave`, mais as que você configurar) viram `***`.
- **`AppLog`** — helper para log de eventos de negócio no meio de um método, onde o AOP não alcança.
- **`CapturaLogAppender`** — helper de teste que captura eventos de log em memória, pra asserir logs sem depender de captura de stdout.

## Instalação

```xml
<dependency>
    <groupId>io.github.trilhalog</groupId>
    <artifactId>trilha-log</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

`trilha-log` já traz o motor de logging (`spring-boot-starter-log4j2` + `log4j-layout-template-json`). Se o seu projeto usa `spring-boot-starter-web` (que traz Logback por padrão), **exclua `spring-boot-starter-logging`** — do contrário as duas bindings SLF4J (Logback e Log4j2) disputam o classpath e o provider ativo fica ambíguo:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

A autoconfiguração (`TrilhaLogAutoConfiguration`) é carregada automaticamente pelo mecanismo padrão do Spring Boot 3.x (`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`) — não precisa de `@Import` nem component-scan manual. Cada bean é `@ConditionalOnMissingBean`, então você pode sobrescrever qualquer peça registrando seu próprio bean do mesmo tipo. O filtro web só é registrado se houver Servlet API no classpath.

### `application.yml`

```yaml
trilha-log:
  correlation:
    enabled: true        # RequestCorrelationFilter — default true
  aspect:
    enabled: true         # LogExecutionAspect — default true
  masking:
    extra-keywords:       # somadas as palavras-chave padrão
      - cpf
      - rg
```

### `log4j2-spring.xml`

Configuração de Log4j2 é por aplicação — copie este arquivo pra `src/main/resources/log4j2-spring.xml` do seu projeto (o template JSON com os campos `traceId`/`callChain` já vem dentro do jar da lib e é referenciado via `classpath:`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="ConsoleDev" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} traceId=%X{traceId} callChain=%X{callChain} -- %msg%n%throwable"/>
        </Console>
        <Console name="ConsoleJson" target="SYSTEM_OUT">
            <JsonTemplateLayout eventTemplateUri="classpath:trilha-log/log4j2-json-template.json"/>
        </Console>
    </Appenders>
    <Loggers>
        <Root level="INFO">
            <SpringProfile name="!prod">
                <AppenderRef ref="ConsoleDev"/>
            </SpringProfile>
            <SpringProfile name="prod">
                <AppenderRef ref="ConsoleJson"/>
            </SpringProfile>
        </Root>
    </Loggers>
</Configuration>
```

Os campos `traceId` e `callChain` do template JSON somem sozinhos quando o MDC não tem valor pra eles (não aparecem como campo vazio).

## Uso

```java
@LogExecution
@Service
public class LoginService {

    private final AppLog appLog;

    public LoginService(AppLog appLog) {
        this.appLog = appLog;
    }

    public String login(LoginRequest request) {
        if (request.senha().isBlank()) {
            appLog.warn("tentativa de login com senha vazia", request);
            return "credenciais invalidas";
        }
        return "bem-vindo, " + request.usuario();
    }
}

public record LoginRequest(String usuario, @Sensitive String senha) {
}
```

Com isso, uma requisição `POST /login` gera algo como:

```
14:32:02.648 [http-nio-8080-exec-2] INFO  LoginService traceId=66678116 callChain=LoginController.login > LoginService.login -- -> LoginService.login args=[LoginRequest{usuario=kevin, senha=***}]
```

`senha` nunca aparece em claro — nem no log de entrada do `@LogExecution`, nem se você passar o `request` pro `AppLog`.

### Regras importantes

- **`AppLog` nunca aceita `Map.of(...)` como contexto de múltiplos campos.** O mascaramento trata `Map` como "não reflete" (mesma proteção contra coleção lazy), e o resultado vira só o nome da classe do `Map`, escondendo justamente o dado que você queria logar. Para contexto com mais de um campo, use um `record` local pequeno — a reflection rasa do `LogMaskingUtil` trata records normalmente.
- **`@LogExecution` nunca embrulha exceção.** Sempre relança o `Throwable` original, pra stack trace completa chegar até quem trata.
- **JPA é opcional.** Se `jakarta.persistence` não estiver no classpath, o mascaramento de entidades simplesmente não entra em ação (nenhum erro, nenhum `NoClassDefFoundError`).

## Testando código que usa a lib

```java
class LoginServiceTest {

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
    void logaTentativaComSenhaVazia() {
        // ...

        assertThat(appender.contemMensagem("tentativa de login com senha vazia")).isTrue();
    }
}
```

## Projeto de exemplo

[`sample-app/`](sample-app) é uma aplicação Spring Boot separada que consome `trilha-log` via Maven local (`mvn install` na raiz, depois `mvn spring-boot:run` dentro de `sample-app/`). Valida na prática: `traceId` correlacionando logs de uma mesma requisição, `callChain` acumulando em chamada de service aninhada (`LoginService.login > AutenticacaoService.autenticar`), saída JSON válida com `-Dspring-boot.run.profiles=prod`, e o campo `@Sensitive` nunca aparecendo em claro.

## Decisões de projeto

- **GroupId**: `io.github.trilhalog` — convenção de reverse-domain pra autor sem domínio próprio (Sonatype/Maven Central recomenda `io.github.<usuario>` nesse caso).
- **Spring Boot**: só 3.x / Jakarta EE. Sem suporte a 2.x/`javax.*`.
- **Módulos**: módulo único por enquanto. Ver [issue sobre split core/web/test-support](../../issues) se o projeto crescer.
- **Publicação**: por ora, só via `mvn install` no repositório Maven local (`~/.m2`). Nenhum repositório remoto configurado ainda.

## Rodando os testes

```bash
mvn test
```

15 testes cobrindo `LogExecutionAspect` (entrada/saída/exceção/call chain/mascaramento de args), `LogMaskingUtil` (todas as regras de mascaramento) e `RequestCorrelationFilter` (traceId no MDC durante e depois da chain, inclusive com exceção).
