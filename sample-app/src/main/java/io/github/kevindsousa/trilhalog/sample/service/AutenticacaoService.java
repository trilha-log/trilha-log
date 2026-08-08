package io.github.kevindsousa.trilhalog.sample.service;

import io.github.kevindsousa.trilhalog.aspect.LogExecution;
import io.github.kevindsousa.trilhalog.logging.AppLog;
import io.github.kevindsousa.trilhalog.sample.dto.LoginRequest;
import org.springframework.stereotype.Service;

/**
 * Chamado por {@link LoginService} — demonstra o callChain acumulando em
 * chamada de service aninhada (LoginService.login {@literal >} AutenticacaoService.autenticar).
 */
@LogExecution
@Service
public class AutenticacaoService {

    private final AppLog appLog;

    public AutenticacaoService(AppLog appLog) {
        this.appLog = appLog;
    }

    public boolean autenticar(LoginRequest request) {
        boolean autenticado = request.senha() != null && !request.senha().isBlank();
        if (autenticado) {
            appLog.info("login bem-sucedido", request);
        } else {
            appLog.warn("tentativa de login com senha vazia", request);
        }
        return autenticado;
    }
}
