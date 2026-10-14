# 02 · Requisitos

## 1. Requisitos funcionais

### Identidade

| Código | Requisito | Detalhe |
|---|---|---|
| RF-01 | O usuário **deve** entrar exclusivamente com a conta Google (OIDC). | Doc 04. Não existe cadastro, senha ou outro provedor. |
| RF-02 | No primeiro login o sistema **deve** criar o usuário a partir do `sub`, e-mail, nome e foto do ID token. | Chave natural: `google_subject` (`sub`), nunca o e-mail. |
| RF-03 | O sistema **deve** manter a sessão no servidor e entregar ao navegador só um cookie de sessão `HttpOnly`. | Tokens do Google nunca chegam ao navegador. |
| RF-04 | O usuário **deve** poder sair (logout local) e **desconectar** a conta (logout + revogação do token no Google + apagar credenciais). | |
| RF-05 | O sistema **deve** exibir nome, e-mail e foto do usuário logado. | `GET /api/me` |

### Contextos

| Código | Requisito | Detalhe |
|---|---|---|
| RF-10 | O sistema **deve** listar os contextos disponíveis com nome, descrição, estado (ligado/desligado) e se os escopos já foram concedidos. | Doc 05 |
| RF-11 | Ao ligar um contexto cujos escopos ainda não foram concedidos, o sistema **deve** iniciar o consentimento incremental só com os escopos desse contexto. | `include_granted_scopes=true` |
| RF-12 | Ao ligar um contexto cujos escopos já foram concedidos, o sistema **deve** ligá-lo sem redirecionar ao Google. | |
| RF-13 | O usuário **deve** poder desligar um contexto a qualquer momento; o efeito vale para a próxima busca e a próxima chamada de ferramenta, inclusive no turno em andamento. | RN-03 |
| RF-14 | Mudanças de contexto **devem** ser avisadas nas conversas abertas por um evento `context.changed`. | Doc 06 |

### Conversa

| Código | Requisito | Detalhe |
|---|---|---|
| RF-20 | O usuário **deve** poder criar, listar, renomear e apagar conversas. | REST, doc 07 |
| RF-21 | O usuário **deve** enviar mensagens pelo WebSocket da conversa e receber a resposta em streaming. | `user.message` → `assistant.delta`* → `assistant.message` |
| RF-22 | Ao reconectar com `lastSeq=N`, o cliente **deve** receber todos os eventos com `seq > N` e depois os novos, sem buraco nem repetição. | Mesmo mecanismo do alien-code |
| RF-23 | O usuário **deve** poder parar o turno em andamento. | `turn.stop` |
| RF-24 | O sistema **deve** mostrar, como eventos, cada busca de ferramentas (o que o RAG achou e o que o DAG acrescentou), cada chamada e seu resultado resumido. | `tool.search`, `tool.started`, `tool.completed` |
| RF-25 | Uma conversa **deve** aceitar um turno por vez; mensagem enviada durante um turno ativo é recusada com `nack` `TURN_IN_PROGRESS`. | |
| RF-26 | Mensagens repetidas com o mesmo `clientMessageId` **não devem** criar um segundo turno (idempotência). | |

### Ferramentas

| Código | Requisito | Detalhe |
|---|---|---|
| RF-30 | O catálogo **deve** vir pré-carregado no banco (migration de seed) com as ferramentas do Google, seus parâmetros, efeito e `depends_on`. | Doc 10 |
| RF-31 | O modelo **deve** receber as ferramentas sob demanda via tool search no cliente; o servidor responde com RAG híbrido + DAG. | Doc 09 |
| RF-32 | A busca **deve** considerar só ferramentas de contextos ligados; ferramentas de contexto desligado que seriam relevantes **devem** gerar o evento `context.suggested`. | RN-03 |
| RF-33 | O executor **deve** barrar a chamada cujo pré-requisito direto ainda não rodou com sucesso na conversa (trava do DAG) e devolver ao modelo quais faltam. | `tool.blocked` |
| RF-34 | Chamadas `READ` **devem** executar sem confirmação. | |
| RF-35 | Chamadas `WRITE` e `DESTRUCTIVE` **devem** ser pausadas até o usuário aprovar ou recusar. | Doc 11 |
| RF-36 | O usuário **deve** ver a descrição legível da operação e os argumentos exatos antes de aprovar. | `approval.requested` |
| RF-37 | O catálogo **deve** ser consultável por REST (lista, detalhe, plano de execução) para estudo e depuração. | Doc 07 |
| RF-38 | Os embeddings do catálogo **devem** ser gerados na subida para ferramentas novas ou com texto alterado (`content_hash`). | Doc 09 |

## 2. Regras de negócio

| Código | Regra |
|---|---|
| RN-01 | Um contexto só pode ficar **ligado** se **todos** os escopos dele estiverem concedidos ao app. |
| RN-02 | Uma ferramenta está **disponível** para o usuário quando o contexto do namespace dela está ligado **e** os contextos de todo o seu fecho de pré-requisitos também estão. |
| RN-03 | Contexto desligado vale imediatamente: a busca não devolve, e o executor recusa (defesa em profundidade), mesmo que o modelo já tenha a definição carregada. |
| RN-04 | Pré-requisito satisfeito = a ferramenta rodou com **sucesso** na mesma conversa dentro da janela `nexo.agent.prerequisite-window` (padrão 30 min). |
| RN-05 | Toda chamada `WRITE`/`DESTRUCTIVE` exige uma aprovação `APPROVED`, de **uso único**, para **exatamente** os argumentos mostrados (hash SHA-256 dos argumentos canônicos). |
| RN-06 | Aprovação vence em 15 minutos; vencida conta como recusa e o turno segue. |
| RN-07 | Severidade da aprovação: `WRITE` → `warning`; `DESTRUCTIVE` → `danger`. |
| RN-08 | O grafo de dependências é acíclico; o gerador da migration e o `DependencyGraph` (na subida) recusam ciclo e mostram o caminho. |
| RN-09 | Desligar um contexto **não** revoga escopos no Google (a API não revoga escopos parcialmente); só "Desconectar conta" revoga. |
| RN-10 | Um usuário só enxerga as próprias conversas, eventos e aprovações. |
| RN-11 | Limites por turno: até 12 chamadas ao modelo, 20 chamadas de ferramenta e 3 minutos. Estourou → `turn.failed` com o motivo. |

## 3. Requisitos não funcionais

| Código | Categoria | Requisito |
|---|---|---|
| RNF-01 | Segurança | Refresh token cifrado em repouso com AES-256-GCM; chave fora do banco (variável de ambiente), com `key_version` para rotação. |
| RNF-02 | Segurança | Cookie de sessão `HttpOnly`, `SameSite=Lax`, `Secure` fora do localhost; proteção CSRF nas rotas REST que alteram estado; checagem de `Origin` no handshake do WebSocket. |
| RNF-03 | Segurança | Tokens, cabeçalhos `Authorization` e conteúdo das respostas do Google **não** aparecem em logs. |
| RNF-04 | Privacidade | Saídas de ferramentas guardadas só resumidas e truncadas (4 mil caracteres no evento); conteúdo completo só em memória durante o turno. |
| RNF-05 | Desempenho | Busca de ferramentas (RAG + DAG, sem o embedding) em ≤ 50 ms p95 com 50 ferramentas e ≤ 150 ms com 1000. |
| RNF-06 | Desempenho | Primeiro `assistant.delta` em ≤ 3 s p50 com modelo na nuvem. |
| RNF-07 | Confiabilidade | Eventos persistidos antes de enviados; replay por `seq` sem perda nem duplicação. |
| RNF-08 | Confiabilidade | Turnos ativos durante um reinício viram `FAILED` na subida (como as missões do alien-code); aprovações pendentes viram `EXPIRED`. |
| RNF-09 | Manutenibilidade | Clean Architecture: o `core` não importa Spring Web, JDBC, HTTP nem SDKs; verificadores das skills sem achados. |
| RNF-10 | Manutenibilidade | Banco versionado por Flyway; o catálogo é dado (JSON → migration gerada), não código. |
| RNF-11 | Observabilidade | Logs estruturados com `conversationId`, `turnId`, `userId` (pseudônimo); métricas Micrometer; endpoint `/actuator/health`. |
| RNF-12 | Testabilidade | Testes do core sem Spring nem rede; integração com PostgreSQL real (Testcontainers + pgvector); LLM e Google substituídos por servidores roteirizados. |
| RNF-13 | Portabilidade | Sobe com `docker compose up -d postgres` + `mvn spring-boot:run` + `npm run dev`. |
| RNF-14 | Usabilidade | Interface em português, responsiva até 360 px, tema claro e escuro. |

## 4. Histórias de usuário e critérios de aceite

### HU-01 · Entrar com Google

```gherkin
Funcionalidade: Login com Google
  Cenário: primeiro acesso
    Dado que não tenho sessão
    Quando clico em "Entrar com Google" e autorizo openid, email e profile
    Então sou redirecionado para a tela de conversas
    E meu usuário é criado com o meu "sub" do Google
    E nenhum contexto está ligado

  Cenário: acesso a rota protegida sem sessão
    Quando chamo GET /api/conversations sem cookie de sessão
    Então recebo 401 com Problem Details "nao-autenticado"
```

### HU-02 · Ligar um contexto

```gherkin
Funcionalidade: Contextos
  Cenário: ligar Calendar pela primeira vez
    Dado que estou logado e o Calendar está desligado e sem escopo concedido
    Quando ligo o Calendar
    Então sou levado ao consentimento do Google pedindo só o escopo do Calendar
    E ao voltar o Calendar aparece ligado

  Cenário: religar um contexto já concedido
    Dado que concedi o Calendar e depois desliguei
    Quando ligo o Calendar de novo
    Então ele fica ligado sem passar pelo Google

  Cenário: recusa no consentimento
    Quando recuso o escopo do Calendar na tela do Google
    Então volto para a tela de contextos com o aviso "Permissão não concedida"
    E o Calendar continua desligado
```

### HU-03 · Conversar e ver o que o assistente faz

```gherkin
Funcionalidade: Conversa
  Cenário: pergunta de leitura
    Dado que o Calendar está ligado
    Quando envio "o que tenho amanhã?"
    Então vejo um evento tool.search com google_calendar_list_events e o pré-requisito google_calendar_list_calendars
    E vejo tool.started e tool.completed de cada chamada
    E recebo a resposta em streaming terminando com assistant.message e turn.completed

  Cenário: contexto desligado
    Dado que o Gmail está desligado
    Quando envio "resuma meus e-mails de hoje"
    Então recebo context.suggested para "gmail"
    E nenhuma ferramenta do Gmail é chamada
```

### HU-04 · Aprovar uma alteração

```gherkin
Funcionalidade: Aprovação de mutações
  Cenário: criar evento
    Dado que o Calendar está ligado
    Quando envio "marque reunião amanhã às 10h com ana@exemplo.com"
    Então recebo approval.requested com severidade warning e os argumentos exatos
    Quando aprovo
    Então o evento é criado e recebo tool.completed

  Cenário: excluir com severidade danger
    Quando o assistente pede para executar google_calendar_delete_event
    Então a aprovação vem com severidade danger

  Cenário: aprovação reutilizada
    Dado uma aprovação já consumida
    Quando envio approval.decide para ela de novo
    Então recebo nack APPROVAL_NOT_PENDING
```

### HU-05 · Reconectar sem perder nada

```gherkin
Funcionalidade: Replay
  Cenário: rede caiu no meio do turno
    Dado que recebi os eventos até seq 41
    Quando reconecto em /ws/conversations/{id}?lastSeq=41
    Então recebo os eventos 42 em diante, em ordem, sem repetir nenhum
```

### HU-06 · Desconectar a conta

```gherkin
Funcionalidade: Desconectar
  Cenário: revogar acesso
    Quando clico em "Desconectar conta Google"
    Então o token é revogado no Google
    E minhas credenciais e contextos ligados são apagados
    E minha sessão é encerrada
```

## 5. Rastreabilidade

| Requisito | Documento | Marco |
|---|---|---|
| RF-01..05, RNF-01..03 | 04 | M1 |
| RF-10..14, RN-01, RN-09 | 05 | M2 |
| RF-20..26, RNF-07..08 | 06, 07 | M3 |
| RF-30..33, RF-37..38, RN-02..04, RN-08 | 08, 09, 10 | M0 (catálogo) e M4 (busca e execução) |
| RF-34..36, RN-05..07 | 11 | M5 |
