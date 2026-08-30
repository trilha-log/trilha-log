#!/usr/bin/env python3
"""Regera o seletor de versao (dropdown + banner de versao antiga) em todo
docs/*/index.html listado em docs/versions.json, sem tocar no resto do
conteudo da pagina.

Uso, depois de editar docs/versions.json (nova versao lancada, versao antiga
que virou "nao mais latest", etc.):

    python3 docs/scripts/rebuild_version_switchers.py

Cada pagina precisa ja ter os marcadores HTML abaixo em volta do bloco (eles
ja vem de fabrica em qualquer pagina gerada por este mesmo script ou pelo
snapshot inicial):

    <!-- VERSION-SWITCHER:START -->
    ...
    <!-- VERSION-SWITCHER:END -->

Se uma pagina nova (docs/vX.Y/index.html) ainda nao tiver os marcadores,
insere logo depois de <body>.
"""
import json
import re
from pathlib import Path

DOCS_DIR = Path(__file__).resolve().parent.parent
VERSIONS_JSON = DOCS_DIR / "versions.json"
REPO_BASE = "/trilha-log"  # base path do GitHub Pages (project site) -- ajustar se o repo mudar de nome

START_MARKER = "<!-- VERSION-SWITCHER:START -->"
END_MARKER = "<!-- VERSION-SWITCHER:END -->"

SWITCHER_STYLE = """<style>
  .version-bar {
    background: var(--surface-alt);
    border-bottom: 1px solid var(--border);
    padding: 0.55rem 1.5rem;
  }
  .version-bar-inner {
    max-width: 74rem;
    margin: 0 auto;
    display: flex;
    align-items: center;
    gap: 0.6rem;
    font-size: 0.82rem;
    color: var(--ink-dim);
  }
  .version-bar label { font-family: var(--mono); }
  .version-bar select {
    font-family: var(--mono);
    font-size: 0.8rem;
    padding: 0.2rem 0.55rem;
    border-radius: 6px;
    border: 1px solid var(--border-strong);
    background: var(--surface);
    color: var(--ink);
  }
  .version-old-banner {
    background: var(--brand-orange-soft);
    border-bottom: 1px solid var(--border);
    padding: 0.65rem 1.5rem;
    font-size: 0.88rem;
    text-align: center;
    color: var(--ink);
  }
  .version-old-banner a { font-weight: 600; margin-left: 0.4rem; color: var(--brand-navy-fixed); }
</style>"""


def local_path_for(entry):
    """Mapeia o path publicado (ex.: /trilha-log/v0.1/) para o arquivo local."""
    path = entry["path"]
    if path.startswith(REPO_BASE):
        path = path[len(REPO_BASE):]
    suffix = path.strip("/")
    return (DOCS_DIR / suffix / "index.html") if suffix else (DOCS_DIR / "index.html")


def build_switcher_block(current_version, versions, latest_version):
    options = []
    for v in versions:
        selected = " selected" if v["version"] == current_version else ""
        options.append(f'<option value="{v["path"]}"{selected}>{v["label"]}</option>')
    options_html = "\n      ".join(options)

    banner = ""
    if current_version != latest_version:
        latest_entry = next(v for v in versions if v["version"] == latest_version)
        banner = (
            '<div class="version-old-banner">'
            f"Você está vendo a documentação da <strong>v{current_version}</strong> — "
            f'a versão mais recente é a <strong>v{latest_version}</strong>.'
            f'<a href="{latest_entry["path"]}">Ver docs da v{latest_version} →</a>'
            "</div>"
        )

    return (
        f"{START_MARKER}\n{SWITCHER_STYLE}\n"
        '<div class="version-bar">\n  <div class="version-bar-inner">\n'
        '    <label for="versionSelect">Versão da documentação</label>\n'
        '    <select id="versionSelect" onchange="window.location.href=this.value">\n'
        f"      {options_html}\n"
        "    </select>\n  </div>\n</div>\n"
        f"{banner}\n{END_MARKER}"
    )


def apply_to_file(file_path, block):
    html = file_path.read_text(encoding="utf-8")
    pattern = re.compile(re.escape(START_MARKER) + r".*?" + re.escape(END_MARKER), re.DOTALL)
    if pattern.search(html):
        html = pattern.sub(lambda _: block, html, count=1)
    else:
        html = re.sub(r"(<body>)", r"\1\n" + block, html, count=1)
    file_path.write_text(html, encoding="utf-8")


def main():
    manifest = json.loads(VERSIONS_JSON.read_text(encoding="utf-8"))
    versions = manifest["versions"]
    latest_version = manifest["latest"]

    for entry in versions:
        file_path = local_path_for(entry)
        if not file_path.exists():
            print(f"aviso: {file_path} nao existe, pulando")
            continue
        block = build_switcher_block(entry["version"], versions, latest_version)
        apply_to_file(file_path, block)
        print(f"atualizado: {file_path.relative_to(DOCS_DIR.parent)}")


if __name__ == "__main__":
    main()
