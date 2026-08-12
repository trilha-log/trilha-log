# Security Policy

## Supported Versions

trilha-log ainda está em desenvolvimento inicial (`0.x`, pré-1.0) — não há branches de manutenção paralelas. Só a última versão publicada recebe atualização de segurança.

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x (latest) | :white_check_mark: |
| < 0.1   | :x:                |

Quando o projeto chegar em `1.0.0`, esta tabela passa a listar as minor releases com suporte ativo.

## Reporting a Vulnerability

**Não abra uma issue pública para vulnerabilidades.** Use a aba [Security do repositório](https://github.com/trilha-log/trilha-log/security/advisories/new) ("Report a vulnerability") para reportar de forma privada.

O que esperar:

- Confirmação de recebimento em até 5 dias úteis.
- Uma avaliação inicial (aceita/rejeitada, severidade) em até 15 dias úteis — este é um projeto mantido em regime de melhor esforço, não há SLA contratual.
- Se aceita: correção em uma nova versão, com aviso via [GitHub Security Advisory](https://github.com/trilha-log/trilha-log/security/advisories) e crédito ao relator (a menos que peça anonimato).
- Se rejeitada (não reproduzível, fora de escopo, etc.): explicação do motivo.

Peço, se possível, incluir:

- Versão do trilha-log afetada.
- Um cenário mínimo de reprodução (código ou passos).
- Impacto esperado (ex.: vazamento de dado mascarado, bypass de mascaramento, etc.).
