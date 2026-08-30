#!/usr/bin/env python3
"""Congela o conteudo ATUAL de docs/index.html (a versao que esta prestes a
deixar de ser "latest") em docs/vX.Y/index.html, ajustando os caminhos de
asset para o novo nivel de pasta.

Roda isso ANTES de sobrescrever docs/index.html com o conteudo da proxima
versao. Depois:
  1. Atualize docs/versions.json: mova a versao recem-congelada pra lista de
     historicas (path apontando pro novo docs/vX.Y/) e mude "latest" pra
     versao nova.
  2. Sobrescreva docs/index.html com o conteudo da nova versao.
  3. Rode rebuild_version_switchers.py pra atualizar o dropdown em todas as
     paginas.

Uso:
    python3 docs/scripts/freeze_current_version.py 0.2
"""
import sys
from pathlib import Path

DOCS_DIR = Path(__file__).resolve().parent.parent


def main():
    if len(sys.argv) != 2:
        print("uso: freeze_current_version.py <versao, ex: 0.2>", file=sys.stderr)
        sys.exit(1)

    version = sys.argv[1]
    dest_dir = DOCS_DIR / f"v{version}"
    dest_file = dest_dir / "index.html"

    if dest_file.exists():
        print(f"docs/v{version}/index.html ja existe -- abortando pra nao sobrescrever.", file=sys.stderr)
        sys.exit(1)

    current = (DOCS_DIR / "index.html").read_text(encoding="utf-8")
    current = current.replace('href="assets/', 'href="../assets/').replace('src="assets/', 'src="../assets/')

    dest_dir.mkdir(parents=True, exist_ok=True)
    dest_file.write_text(current, encoding="utf-8")

    print(f"Congelado em docs/v{version}/index.html")
    print("Proximo passo: edite docs/versions.json e rode rebuild_version_switchers.py")


if __name__ == "__main__":
    main()
