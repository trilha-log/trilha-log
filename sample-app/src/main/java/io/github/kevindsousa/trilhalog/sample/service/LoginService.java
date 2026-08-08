package io.github.kevindsousa.trilhalog.sample.service;

import io.github.kevindsousa.trilhalog.aspect.LogExecution;
import io.github.kevindsousa.trilhalog.sample.dto.LoginRequest;
import org.springframework.stereotype.Service;

@LogExecution
@Service
public class LoginService {

    private final AutenticacaoService autenticacaoService;

    public LoginService(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    public String login(LoginRequest request) {
        boolean autenticado = autenticacaoService.autenticar(request);
        return autenticado ? "bem-vindo, " + request.usuario() : "credenciais invalidas";
    }
}
