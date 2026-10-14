"""Gera a migration Flyway do seed do catálogo a partir de docs/catalogo/google-tools.json.

Uso (na raiz do repositório):
    python3 scripts/gerar_migration_catalogo.py                     # gera V8__catalogo_google.sql
    python3 scripts/gerar_migration_catalogo.py --versao 9 --nome catalogo_tasks

Valida antes de escrever: nomes no padrão, namespace e contexto existentes, pré-requisitos
existentes, ausência de ciclo (mostra o caminho) e dag_level igual ao calculado. O banco não tem
função nem gatilho de DAG: a validação mora aqui e no DependencyGraph do nexo-server.
"""
import argparse
import json
import re
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
CATALOGO = RAIZ / "docs" / "catalogo" / "google-tools.json"
MIGRATIONS = RAIZ / "nexo-server" / "src" / "main" / "resources" / "db" / "migration"
NOME = re.compile(r"^[a-z][a-z0-9_]{1,63}$")
TIPOS = {"string", "integer", "number", "boolean", "array"}
EFEITOS = {"READ", "WRITE", "DESTRUCTIVE"}


def falhar(mensagem):
    sys.exit("catálogo inválido: " + mensagem)


def validar(catalogo):
    contextos = {c["key"] for c in catalogo["contexts"]}
    namespaces = {}

    for n in catalogo["namespaces"]:
        if not NOME.match(n["name"]):
            falhar(f"namespace com nome inválido: {n['name']}")

        if n["context"] not in contextos:
            falhar(f"namespace {n['name']} aponta para contexto inexistente {n['context']}")

        namespaces[n["name"]] = n["context"]

    ferramentas = {}

    for t in catalogo["tools"]:
        if not NOME.match(t["name"]):
            falhar(f"ferramenta com nome inválido: {t['name']}")

        if t["name"] in ferramentas:
            falhar(f"ferramenta repetida: {t['name']}")

        if t["namespace"] not in namespaces:
            falhar(f"{t['name']} aponta para namespace inexistente {t['namespace']}")

        if t["effect"] not in EFEITOS:
            falhar(f"{t['name']} tem efeito inválido {t['effect']}")

        for p in t["parameters"]:
            if p["type"] not in TIPOS:
                falhar(f"{t['name']}.{p['name']} tem tipo inválido {p['type']}")

            if (p["type"] == "array") != ("items" in p):
                falhar(f"{t['name']}.{p['name']}: items é obrigatório só em array")

        ferramentas[t["name"]] = t

    for t in ferramentas.values():
        for d in t["depends_on"]:
            if d not in ferramentas:
                falhar(f"{t['name']} depende de {d}, que não existe")

            if d == t["name"]:
                falhar(f"{t['name']} depende de si mesma")

    niveis = {}

    def nivel(nome, caminho):
        if nome in caminho:
            falhar("ciclo de dependências: " + " -> ".join(caminho + [nome]))

        if nome not in niveis:
            deps = ferramentas[nome]["depends_on"]
            niveis[nome] = 0 if not deps else 1 + max(nivel(d, caminho + [nome]) for d in deps)

        return niveis[nome]

    for nome, t in ferramentas.items():
        if nivel(nome, []) != t["dag_level"]:
            falhar(f"{nome}: dag_level {t['dag_level']} no JSON, {niveis[nome]} calculado")


def sql(valor):
    if valor is None:
        return "NULL"

    if isinstance(valor, bool):
        return "true" if valor else "false"

    if isinstance(valor, (int, float)):
        return str(valor)

    return "'" + str(valor).replace("'", "''") + "'"


def array_sql(valores):
    if valores is None:
        return "NULL"

    return "ARRAY[" + ", ".join(sql(v) for v in valores) + "]::text[]"


def insert(tabela, colunas, linhas):
    corpo = ",\n".join("    (" + ", ".join(linha) + ")" for linha in linhas)
    return f"INSERT INTO {tabela} ({', '.join(colunas)}) VALUES\n{corpo};\n"


def gerar(catalogo):
    tools = catalogo["tools"]
    parametros = [(t["name"], p) for t in tools for p in t["parameters"]]
    arestas = [(t["name"], d) for t in tools for d in t["depends_on"]]
    partes = [
        "-- ════════════════════════════════════════════════════════════════════════════════\n",
        "--  Seed do catálogo Google Workspace\n",
        "--\n",
        "--  Gerado por scripts/gerar_migration_catalogo.py a partir de docs/catalogo/google-tools.json.\n",
        "--  Não edite à mão: mude o JSON e gere uma migration nova.\n",
        f"--  {len(catalogo['contexts'])} contextos, {len(catalogo['namespaces'])} namespaces, {len(tools)} ferramentas, ",
        f"{len(parametros)} parâmetros e {len(arestas)} dependências.\n",
        "-- ════════════════════════════════════════════════════════════════════════════════\n\n",
        insert(
            "google_context",
            ["key", "name", "description", "google_api", "position"],
            [[sql(c["key"]), sql(c["name"]), sql(c["description"]), sql(c["google_api"]), sql(i + 1)] for i, c in enumerate(catalogo["contexts"])],
        ),
        "\n",
        insert(
            "google_context_scope",
            ["context_key", "scope"],
            [[sql(c["key"]), sql(s)] for c in catalogo["contexts"] for s in c["scopes"]],
        ),
        "\n",
        insert(
            "tool_namespace",
            ["name", "context_key", "description"],
            [[sql(n["name"]), sql(n["context"]), sql(n["description"])] for n in catalogo["namespaces"]],
        ),
        "\n",
        insert(
            "tool",
            ["name", "namespace", "effect", "description"],
            [[sql(t["name"]), sql(t["namespace"]), sql(t["effect"]), sql(t["description"])] for t in tools],
        ),
        "\n",
        insert(
            "tool_parameter",
            ["tool_name", "ordinal", "name", "type", "description", "required", "minimum", "maximum", "enum_values", "items"],
            [
                [
                    sql(nome),
                    sql(p["ordinal"]),
                    sql(p["name"]),
                    sql(p["type"]),
                    sql(p["description"]),
                    sql(p["required"]),
                    sql(p.get("minimum")),
                    sql(p.get("maximum")),
                    array_sql(p.get("enum")),
                    "NULL" if "items" not in p else sql(json.dumps(p["items"], ensure_ascii=False)) + "::jsonb",
                ]
                for nome, p in parametros
            ],
        ),
        "\n",
        insert("tool_dependency", ["tool_name", "depends_on"], [[sql(a), sql(b)] for a, b in arestas]),
    ]
    return "".join(partes)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--versao", type=int, default=8)
    parser.add_argument("--nome", default="catalogo_google")
    args = parser.parse_args()

    catalogo = json.loads(CATALOGO.read_text(encoding="utf-8"))
    validar(catalogo)

    destino = MIGRATIONS / f"V{args.versao}__{args.nome}.sql"
    destino.write_text(gerar(catalogo), encoding="utf-8")
    print(f"{destino.relative_to(RAIZ)}: {len(catalogo['tools'])} ferramentas")


if __name__ == "__main__":
    main()
