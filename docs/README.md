# Documentação versionada

`docs/` é servido via GitHub Pages ([trilha-log.github.io/trilha-log](https://trilha-log.github.io/trilha-log/)). Esquema:

- **`docs/index.html`** — sempre a versão mais recente (hoje: v0.2). É pra onde o README e os badges apontam.
- **`docs/vX.Y/index.html`** — snapshot congelado de uma versão que já deixou de ser a mais recente (ex.: `docs/v0.1/`). Nunca muda depois de criado.
- **`docs/versions.json`** — lista as versões disponíveis (usada só como referência humana; cada página já carrega sua própria cópia inline do dropdown).
- **`docs/assets/`** — logo/favicon compartilhados por todas as versões (`../assets/...` nas páginas de `docs/vX.Y/`).

Cada página tem um seletor de versão (dropdown) injetado entre os marcadores `<!-- VERSION-SWITCHER:START -->` / `...:END`, e as páginas que não são mais a última mostram um aviso ("você está vendo a v0.1, a mais recente é a v0.2").

## Ao lançar uma nova versão (ex.: v0.3)

1. **Congele a versão atual antes de sobrescrevê-la**:
   ```bash
   python3 docs/scripts/freeze_current_version.py 0.2
   ```
   Isso copia o `docs/index.html` atual (ainda descrevendo a v0.2) para `docs/v0.2/index.html`, já ajustando os caminhos de asset.

2. **Sobrescreva `docs/index.html`** com o conteúdo documentando a v0.3 (o que mudou, novas seções, etc.).

3. **Atualize `docs/versions.json`**: mude `"latest"` para `"0.3"` e adicione a entrada da v0.2 recém-congelada na lista `"versions"` (path: `/trilha-log/v0.2/`).

4. **Regenere os seletores em todas as páginas**:
   ```bash
   python3 docs/scripts/rebuild_version_switchers.py
   ```

5. Confira visualmente (abrir os arquivos localmente ou no preview do PR) antes de mergear.

## Por que arquivos estáticos, sem framework

O repo é uma lib Java — trazer um toolchain Node/React (Docusaurus, por exemplo) só pra versionar a doc é peso desproporcional pro tamanho atual do site. Se a documentação crescer muito (muitas versões, busca, i18n), migrar pra uma ferramenta de docs versionada de verdade (Docusaurus é a opção natural, já resolve isso nativamente) vale a pena reconsiderar — ver issue correspondente no repositório.
