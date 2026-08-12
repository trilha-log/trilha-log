<div align="center">

# trilha·log

**Logging estruturado para Spring Boot** — correlação de requisição, execução de método via AOP e mascaramento de dados sensíveis, prontos como starter.

[![License: MIT](https://img.shields.io/github/license/trilha-log/trilha-log)](LICENSE)
[![Build](https://img.shields.io/github/actions/workflow/status/trilha-log/trilha-log/publish.yml?branch=main)](https://github.com/trilha-log/trilha-log/actions/workflows/publish.yml)
![Java](https://img.shields.io/badge/Java-17%2B-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F)

[📖 Documentação completa](https://trilha-log.github.io/trilha-log/) · [Issues](https://github.com/trilha-log/trilha-log/issues) · [Sample app](sample-app)

</div>

---

Nasceu de uma arquitetura de logging validada em produção (dev e prod, Log4j2, JSON estruturado) e foi generalizada para qualquer projeto Spring Boot 4.x novo. Adiciona a dependência, exclui o Logback, e ganha:

- **`traceId` por requisição** — correlaciona todo log de uma mesma requisição, inclusive de bibliotecas de terceiro (Hibernate, Spring Security), via MDC/ThreadContext.
- **`@LogExecution`** — log automático de entrada/saída/exceção de método via AOP, com breadcrumb de call chain acumulando em chamadas aninhadas. Nunca embrulha a exceção capturada.
- **`@Sensitive` + mascaramento central** — nunca reflete entidade JPA nem `Collection`/`Map`/array; campos sensíveis (por anotação ou por nome) viram `***` em qualquer log.
- **`AppLog`** — log de evento de negócio no meio de um método, onde o AOP não alcança.
- **`CapturaLogAppender`** — captura eventos de log em memória nos seus testes, sem depender de stdout.

Cada peça é registrada por autoconfiguração padrão do Spring Boot 4.x e pode ser sobrescrita (`@ConditionalOnMissingBean`). A documentação completa — conceitos, diagramas, referência de configuração, distribuição via GitHub Packages e armadilhas conhecidas — está em **[trilha-log.github.io/trilha-log](https://trilha-log.github.io/trilha-log/)**.

## Instalação

trilha-log é distribuído via **GitHub Packages** (não Maven Central). O pacote é público, mas o protocolo Maven do GitHub Packages exige um token de qualquer forma — veja [autenticação](https://trilha-log.github.io/trilha-log/#instalacao) na documentação completa para os três cenários (build local, Docker, CI).

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/trilha-log/trilha-log</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>io.github.trilhalog</groupId>
        <artifactId>trilha-log</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

trilha-log já traz o motor de logging (Log4j2). Se o seu projeto usa `spring-boot-starter-web` ou qualquer starter que puxe `spring-boot-starter-logging` (Logback) transitivamente, exclua-o:

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

## Uso mínimo

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

`senha` nunca aparece em claro em nenhum log — nem no `@LogExecution`, nem no `AppLog`. Para o resto (`application.yml`, `log4j2-spring.xml`, convenção de níveis, testes com `CapturaLogAppender`, perguntas frequentes), veja a **[documentação completa](https://trilha-log.github.io/trilha-log/)**.

## Projeto de exemplo

[`sample-app/`](sample-app) é uma aplicação Spring Boot separada que consome trilha-log via Maven local, validando de ponta a ponta: `traceId` correlacionando uma requisição, `callChain` acumulando em chamada de service aninhada, saída JSON válida em produção, e mascaramento de campo sensível.

```bash
mvn install                              # instala a lib no repositório Maven local
cd sample-app && mvn spring-boot:run     # sobe o app de exemplo consumindo ela
```

## Contribuindo

Issues e PRs são bem-vindos.

- `mvn test` roda a suíte completa antes de qualquer PR.
- Discussões de arquitetura maiores (ex.: split em módulos `core`/`web`/`test-support`) ficam registradas como [issue](https://github.com/trilha-log/trilha-log/issues) antes de virar código.
- Java 17+, Spring Boot 4.x / Jakarta EE apenas — sem suporte a 2.x/`javax.*`.

## Licença

[MIT](LICENSE)
