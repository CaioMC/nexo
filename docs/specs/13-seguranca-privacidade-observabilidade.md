# 13 · Segurança, privacidade e observabilidade

## 1. Modelo de ameaças (STRIDE resumido)

| Ameaça | Exemplo | Controle |
|---|---|---|
| **S**poofing | Sessão roubada usada de outra origem | Cookie `HttpOnly` + `SameSite=Lax`; checagem de `Origin` no WS; troca do id de sessão no login |
| **T**ampering | Modelo altera argumentos depois da aprovação | `arguments_hash` conferido na execução (RN-05) |
| **R**epudiation | "Eu não aprovei isso" | `tool_approval` com `decided_at`, usuário e argumentos; evento `approval.resolved` no Event Store |
| **I**nformation disclosure | Token do Google em log ou no front | Token só no servidor, cifrado em repouso; filtro de logs; `toString()` mascarado |
| **D**enial of service | Loop do modelo gastando cota | Limites por turno (RN-11), rate limit de `user.message` (10/min por usuário) |
| **E**levation of privilege | Modelo usa contexto desligado; usuário lê conversa de outro | `ToolGatekeeperService` checa contexto na execução; toda query filtra por `user_id` da sessão |

## 2. Prompt injection

Conteúdo de e-mails, documentos e mensagens de chat é **não confiável**. Defesas em camadas:

1. **Instrução** de sistema: saída de ferramenta é dado, não ordem (doc 11, seção 6).
2. **Embrulho**: a saída vai ao modelo dentro de `{"source":"google_gmail_get_message","untrusted_content":...}`.
3. **Barreira determinística**: nenhuma mutação roda sem aprovação humana com descrição gerada pelo servidor; contexto desligado é barrado no executor; trava do DAG.
4. **Sem exfiltração silenciosa**: não há ferramenta de requisição HTTP genérica nem envio direto de e-mail (só rascunho).

## 3. Checklist (OWASP ASVS nível 1, itens aplicáveis)

| Área | Item |
|---|---|
| Autenticação | Só OIDC Google; `state`, `nonce` e PKCE; validação completa do ID token |
| Sessão | Expiração por inatividade (8 h) e absoluta (7 dias); logout invalida no servidor |
| Controle de acesso | `userId` sempre da sessão; testes de acesso cruzado (usuário A × conversa de B → 404) |
| Validação | Bean Validation nos requests; esquema da ferramenta validado no gatekeeper; limite de 8000 caracteres por mensagem |
| Criptografia | AES-256-GCM, IV aleatório, chave fora do banco, `key_version` |
| Erros | Problem Details sem stack trace; mensagens do Google repassadas sem tokens |
| Cabeçalhos | CSP restritiva no front (`default-src 'self'`; `img-src` com `lh3.googleusercontent.com`), `X-Content-Type-Options`, `Referrer-Policy: same-origin` |
| Dependências | `mvn dependency-check` / `npm audit` no CI |
| Segredos | `.env` (chave do proxy de LLM, segredos do Google) no `.gitignore` com permissão `600`; só `.env.example` sem valores é versionado; `gitleaks` no pre-commit |

## 4. Privacidade (LGPD e Google API Services User Data Policy)

| Princípio | Como o Nexo cumpre |
|---|---|
| Finalidade e minimização | Escopos pedidos por contexto, só quando o usuário liga (consentimento incremental) |
| Uso limitado (*Limited Use* do Google) | Dados do Google usados só para responder ao próprio usuário; não treinam modelos, não são vendidos nem usados para anúncios; o provedor de LLM configurado com retenção zero / sem treino quando disponível |
| Transparência | Tela de contextos mostra os escopos de cada um; timeline mostra cada ferramenta chamada |
| Armazenamento | Saídas de ferramenta guardadas só resumidas e truncadas (RNF-04); nada de cópia de arquivos |
| Direitos do titular | "Apagar meus dados" (cascade em `app_user`) + revogação do token |
| Retenção | Doc 08, seção 5 |

## 5. Observabilidade

### Logs

- JSON (Logback + `logstash-logback-encoder`), com MDC: `userId` (hash curto), `conversationId`, `turnId`, `callId`.
- `TokenRedactingConverter` remove `Bearer …`, `refresh_token`, `access_token`, `code`, `client_secret`.
- Nunca logar `payload` de evento, argumentos de ferramenta ou saídas do Google em nível `INFO`; em `DEBUG` só os nomes dos campos.

### Métricas (Micrometer, `/actuator/prometheus`)

| Métrica | Tipo | Tags |
|---|---|---|
| `nexo.turn.duration` | timer | `status` |
| `nexo.model.calls`, `nexo.model.tokens` | counter | `kind` (input, cached, output) |
| `nexo.retrieval.select.duration` | timer | `strategy`, `dag` |
| `nexo.retrieval.tools.delivered` | distribution | `origin` (RAG, DAG) |
| `nexo.tool.calls` | counter | `tool`, `status` (SUCCEEDED, FAILED, BLOCKED, REJECTED) |
| `nexo.google.api.duration` | timer | `context`, `status` |
| `nexo.approval.decisions` | counter | `decision`, `severity` |
| `nexo.ws.sessions` | gauge | — |

### Saúde

`/actuator/health` com `db`, `llm` (chave configurada) e `embeddings` (vetores em dia com o `content_hash` calculado pelo `ToolDocumentBuilder`).
Só `health` e `prometheus` expostos, e só em `127.0.0.1`.
