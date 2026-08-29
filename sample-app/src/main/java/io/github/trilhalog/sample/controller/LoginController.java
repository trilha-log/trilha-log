package io.github.trilhalog.sample.controller;

import io.github.trilhalog.sample.dto.LoginRequest;
import io.github.trilhalog.sample.service.LoginService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request) {
        return loginService.login(request);
    }

    @GetMapping("/saudacao/{nome}")
    public String saudacao(@PathVariable String nome) {
        return "ola, " + HtmlUtils.htmlEscape(nome);
    }
}
