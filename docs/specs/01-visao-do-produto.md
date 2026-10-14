# 01 · Visão do produto (PRD)

## 1. Problema

Quem usa o Google Workspace no dia a dia pula entre seis ou sete produtos para fazer uma tarefa
simples: "marca uma reunião com quem respondeu o e-mail do orçamento e coloca o link da planilha
na descrição" passa por Gmail, Contatos, Sheets e Calendar. Assistentes genéricos não enxergam esses
dados, e os que enxergam costumam pedir acesso a tudo de uma vez.

## 2. Proposta

Um assistente conversacional (POC) que:

1. entra **só com a conta Google**;
2. deixa o usuário **escolher quais contextos** o assistente pode usar e mudar isso a qualquer momento;
3. entende o pedido, **encontra as ferramentas certas** num catálogo de dezenas de funções sem mandar
   o catálogo inteiro ao modelo, e respeita a **ordem** em que elas precisam rodar;
4. **lê livremente** dentro dos contextos ligados e **pede confirmação** antes de alterar qualquer coisa;
5. mostra **ao vivo**, pelo WebSocket, o que está fazendo: ferramentas buscadas, chamadas, resultados.

## 3. Público

| Persona | Descrição | O que espera |
|---|---|---|
| **Profissional do conhecimento** | Usa Gmail, Calendar, Docs e Sheets o dia todo, conta pessoal ou Workspace | Delegar tarefas repetitivas em linguagem natural |
| **Pessoa preocupada com privacidade** | Quer ajuda, mas não quer liberar o e-mail | Ligar só Calendar e Docs e ter certeza de que o resto não é tocado |
| **Desenvolvedor estudando agentes** (público interno da POC) | Quer ver tool search, RAG e DAG funcionando com ferramentas reais | Código legível, decisões documentadas, consultas SQL reproduzíveis |

## 4. Objetivos e métricas da POC

| Objetivo | Métrica | Meta |
|---|---|---|
| Escolher bem as ferramentas | Recall@8 do RAG híbrido no conjunto de avaliação (seção 9.6 do doc 09) | ≥ 0,90 |
| Não mandar o catálogo inteiro | Ferramentas carregadas por turno (mediana) | ≤ 10 de 50 |
| Respeitar dependências | Chamadas executadas sem os pré-requisitos | 0 (a trava do DAG barra) |
| Nunca alterar sem permissão | Mutações executadas sem aprovação `APPROVED` | 0 |
| Respeitar os contextos | Chamadas a contexto desligado | 0 (barradas na busca **e** na execução) |
| Conversa fluida | Tempo até o primeiro `assistant.delta` (p50, modelo na nuvem) | ≤ 3 s |
| Reconexão sem perda | Eventos perdidos ou duplicados após reconectar | 0 |

## 5. Escopo

### 5.1 Dentro (POC)

- Login e logout só com Google (OIDC) e sessão no servidor.
- Tela de contextos com liga/desliga e consentimento incremental de escopos.
- Conversas: criar, listar, renomear, apagar; histórico persistido.
- Canal WebSocket por conversa com replay por `lastSeq`, mensagens do usuário, decisões de aprovação e "Parar".
- Catálogo de ferramentas Google no PostgreSQL (seed com as 50 funções do `google-tools`).
- Tool search no cliente com RAG híbrido (full-text + pgvector, RRF) e DAG (fecho + Kahn + trava).
- Execução real das ferramentas nas APIs REST do Google com o token do usuário.
- Aprovação de mutações com validade de 15 minutos e uso único.
- Auditoria: buscas, chamadas, aprovações e consumo de tokens.

### 5.2 Fora (por enquanto)

| Item | Motivo |
|---|---|
| Login por e-mail/senha ou outros provedores | Requisito: autenticação **só** via Google |
| Voz (GPT-Live / WebRTC) | O `google-tools` tem; fica para depois do M5 |
| RAG sobre o **conteúdo** dos arquivos do usuário (indexar Drive) | Custo, privacidade e política de dados do Google; ver "Futuro" no doc 15 |
| Envio direto de e-mail | O catálogo só cria rascunho (`google_gmail_create_draft`), como no `google-tools` |
| Multi-tenant corporativo / delegação de domínio (service account) | POC é por usuário final |
| App mobile | Web responsiva basta |
| Publicação do app no Google (verificação de escopos sensíveis/restritos) | POC roda em modo **Testing** com usuários de teste (doc 04, seção 8) |

## 6. Premissas e restrições

- Um projeto no Google Cloud com OAuth Client do tipo **Web application** e as APIs dos contextos habilitadas.
- Em modo Testing o Google limita a 100 usuários de teste e o refresh token expira em **7 dias**: o usuário terá que entrar de novo. Aceitável na POC.
- Modelo de linguagem com **tool search** (OpenAI Responses API, `gpt-5.4-mini` ou superior) e embeddings `text-embedding-3-small` (1536 dimensões), atrás de portas (doc 03): trocar de provedor é trocar adaptador. **(proposta)**
- O servidor roda em `127.0.0.1` na POC; HTTPS fica no proxy reverso quando sair da máquina.

## 7. Riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| Modelo escolhe ferramenta errada ou alucina argumentos | Ação errada no Google | Aprovação obrigatória em mutações; trava do DAG; esquema `strict` |
| Prompt injection vindo de conteúdo do usuário (e-mail, doc) | Modelo tenta agir contra o usuário | Saídas de ferramenta marcadas como dado; mutações sempre aprovadas; contexto desligado barrado no executor (doc 13) |
| Vazamento de token | Acesso à conta Google | Refresh token cifrado (AES-256-GCM), nunca vai ao front nem ao modelo nem aos logs |
| Escopos restritos (Gmail, Drive) exigem verificação de segurança do Google para produção | Bloqueia lançamento público | Fora do escopo da POC; registrado no roadmap |
| Cotas das APIs do Google | Erros 429 | Retry com backoff só em 429/5xx (doc 11) |
