<h1 align="center">Nexo</h1>

<p align="center">
  Assistente conversacional que conversa com <b>todo o Google Workspace</b> do usuário
  (Gmail, Drive, Docs, Sheets, Slides, Calendar, Chat e Contatos), só nos contextos que ele ativou.
</p>

<p align="center">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-orange" />
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-3.5-6db33f" />
  <img alt="PostgreSQL + pgvector" src="https://img.shields.io/badge/PostgreSQL%2017-pgvector-336791" />
  <img alt="React" src="https://img.shields.io/badge/React-19-61dafb" />
  <img alt="Marco" src="https://img.shields.io/badge/marco-M5-7ee787" />
</p>

---

> **Estado:** marcos **M0 a M5 entregues**: login com Google (BFF), contextos com consentimento
> incremental, conversa ao vivo por WebSocket, tool search no cliente com RAG + DAG, executores dos 8
> produtos e aprovação de alterações. Falta o M6 (acabamento). Specs em [`docs/specs/00-indice.md`](docs/specs/00-indice.md).

## Em uma frase

O usuário entra com a conta Google, liga os contextos que quer liberar (ex.: Calendar e Sheets),
e conversa com o assistente por WebSocket. A cada pedido, o servidor escolhe as ferramentas certas
no catálogo (**RAG** híbrido com pgvector + **DAG** de dependências, ambos calculados no código), executa as leituras e
pede confirmação antes de qualquer alteração.

## De onde vem

| Aprendizado | Projeto de origem | Como entra aqui |
|---|---|---|
| WebSocket com replay por `lastSeq` e Event Store | `alien-code` | Canal da conversa: eventos numerados, reconexão sem buraco nem repetição |
| Tool search no cliente + RAG + DAG (Kahn) | `dta-tool-search` · [`tool-search-pg`](https://github.com/CaioMC/tool-search-pg) | Mesmas tabelas do tool-search-pg; RAG (RRF) e DAG (Kahn) no código Java, banco só guarda e indexa |
| 50 funções do Google Workspace + aprovação de mutações | `google-tools` | Seed do catálogo e fluxo de confirmação |
| Clean Architecture + DDD + estilo `this.` | skills `java-clean-ddd` e `java-code-style` | Regras de código do `nexo-server` |

## Como rodar

**Pré-requisitos:** Docker, Java 21, Maven, Node 20+ e um OAuth Client do Google.

### 1. Google Cloud (uma vez)

Crie um OAuth Client do tipo **Web application** (doc 04, seção 2):

- redirect URI: `http://localhost:8080/login/oauth2/code/google`
- origem JavaScript: `http://localhost:5173`
- tela de consentimento em modo *Testing*, com o seu e-mail como usuário de teste
- APIs habilitadas: Gmail, Drive, Docs, Sheets, Slides, Calendar, Chat e People

### 2. Servidor e front

```bash
cp .env.example .env                 # OPENAI_*, GOOGLE_CLIENT_ID/SECRET, NEXO_TOKEN_KEY (o .env não é versionado)
docker compose up -d postgres        # PostgreSQL 17 + pgvector em 127.0.0.1:5433
cd nexo-server && mvn spring-boot:run   # Flyway, catálogo, embeddings; escuta em 127.0.0.1:8080
cd nexo-web && npm install && npm run dev   # abra http://localhost:5173
```

Entre com o Google, ligue um contexto (ex.: Calendar) e pergunte "o que tenho amanhã?". O Vite
repassa `/api`, `/ws` e `/oauth2` ao servidor, então o cookie de sessão vale para os dois.

Sem `GOOGLE_CLIENT_ID` o servidor sobe, mas o Google recusa o login. Sem `OPENAI_API_KEY` a busca usa
só full-text e o WebSocket recusa mensagens (`MODEL_NOT_CONFIGURED`). Sem `NEXO_TOKEN_KEY` a chave
dos refresh tokens é aleatória a cada subida.

### Testes

| Comando | O que roda |
|---|---|
| `mvn test` | 94 unitários: domínio (DAG, RRF, turno, argumentos, hash), serviços com dublês (gatekeeper, contextos, tokens), adapters com `HttpServer` (Google, OpenAI), OAuth (resolver e handler) |
| `mvn verify` | + 19 de integração com PostgreSQL/pgvector real (Testcontainers), entre eles o `AgentFlowIT`: WebSocket com sessão real e modelo/Google falsos |
| `NEXO_LIVE=1 mvn verify -Dit.test=LiveModelIT` | O laço contra o modelo do `.env` (proxy), com Google falso |
| `cd nexo-web && npm test` | 14 testes Vitest: reducer, socket com reconexão, aprovação, consentimento |
| `python3 scripts/gerar_migration_catalogo.py` | Valida `docs/catalogo/google-tools.json` e gera a migration do seed |

## Estrutura

```
nexo/
├── nexo-server/        Java 21 + Spring Boot 3.5 (Clean Architecture): identidade, contextos,
│                       catálogo, busca (RAG + DAG) e conversa (agente, aprovações, WebSocket)
├── nexo-web/           React 19 + TypeScript + Vite: login, contextos, conversa ao vivo
├── scripts/            gerar_migration_catalogo.py (JSON → migration Flyway)
├── compose.yaml        PostgreSQL 17 + pgvector
└── docs/
    ├── specs/          especificação, dividida por assunto (comece pelo índice)
    ├── adr/            decisões de arquitetura
    ├── contratos/      openapi.yaml (REST) e asyncapi.yaml (WebSocket)
    ├── catalogo/       google-tools.json: as 50 ferramentas, fonte do seed
    ├── guia-do-codigo.md  mapa das classes e métodos, da chamada ao modelo aos pontos de atenção
    └── sequencias.md   diagramas de sequência do código, classe a classe
```
