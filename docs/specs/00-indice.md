# Especificação do Nexo — índice

Cada documento cobre **um assunto** e pode ser revisado sozinho. A ordem abaixo é a ordem de
leitura sugerida: do porquê (produto) para o como (arquitetura, dados, contratos) e o quando
(roadmap).

| # | Documento | Pergunta que responde | Padrão de mercado seguido |
|---|---|---|---|
| 01 | [Visão do produto](01-visao-do-produto.md) | Para quem, por quê, o que entra e o que fica de fora? | PRD |
| 02 | [Requisitos](02-requisitos.md) | O que o sistema tem que fazer e com que qualidade? | SRS (RF, RNF, RN) + histórias com critérios Gherkin |
| 03 | [Arquitetura](03-arquitetura.md) | Quais são as peças e como o código se organiza? | C4 (contexto, contêiner, componente) + Clean Architecture |
| 04 | [Autenticação Google](04-autenticacao-google.md) | Como o usuário entra e como guardamos o acesso ao Google? | OAuth 2.0 / OIDC, BFF, PKCE |
| 05 | [Contextos Google](05-contextos-google.md) | Como o usuário liga e desliga Calendar, Docs, Sheets...? | Consentimento incremental |
| 06 | [Protocolo WebSocket](06-protocolo-websocket.md) | Como front e back conversam em tempo real? | Envelope de eventos + AsyncAPI |
| 07 | [API REST](07-api-rest.md) | Quais rotas HTTP existem? | OpenAPI 3.1 + Problem Details (RFC 9457) |
| 08 | [Modelo de dados](08-modelo-de-dados.md) | Quais tabelas, funções e migrations? | ER + migrations Flyway versionadas |
| 09 | [Tool search, RAG e DAG](09-tool-search-rag-dag.md) | Como o modelo recebe só as ferramentas certas? | Tool search no cliente, RAG híbrido (RRF), ordenação topológica |
| 10 | [Catálogo de ferramentas](10-catalogo-ferramentas.md) | Quais ferramentas e quais dependências (`depends_on`)? | Catálogo versionado como dado |
| 11 | [Agente e aprovações](11-agente-e-aprovacoes.md) | Como funciona o laço do agente e a confirmação de alterações? | Human-in-the-loop |
| 12 | [Front-end](12-frontend.md) | Telas, estado e componentes | — |
| 13 | [Segurança, privacidade e observabilidade](13-seguranca-privacidade-observabilidade.md) | O que protege os dados do usuário e como enxergamos o sistema? | OWASP ASVS, LGPD, Google API Services User Data Policy |
| 14 | [Qualidade e testes](14-qualidade-e-testes.md) | Como o código é escrito e testado? | Pirâmide de testes, Testcontainers |
| 15 | [Roadmap](15-roadmap.md) | Em que ordem construir e quando cada marco está pronto? | Marcos com Definition of Done |

## Artefatos de apoio

| Artefato | Conteúdo |
|---|---|
| [`../adr/`](../adr/) | Decisões de arquitetura (formato MADR curto): o que foi decidido, alternativas e consequências |
| [`../contratos/openapi.yaml`](../contratos/openapi.yaml) | Contrato REST |
| [`../contratos/asyncapi.yaml`](../contratos/asyncapi.yaml) | Contrato do WebSocket |
| [`../catalogo/google-tools.json`](../catalogo/google-tools.json) | As 50 ferramentas com parâmetros, efeito e `depends_on`: fonte da migration de seed |
| [`../guia-do-codigo.md`](../guia-do-codigo.md) | Mapa do código: classes, métodos, a chamada ao modelo, RAG + DAG, execução e pontos de atenção |
| [`../sequencias.md`](../sequencias.md) | Diagramas de sequência do código (M0 a M5), com os nomes reais das classes |

## Convenções destes documentos

- **RF-xx** requisito funcional · **RNF-xx** requisito não funcional · **RN-xx** regra de negócio ·
  **HU-xx** história de usuário. Os outros documentos citam esses códigos.
- Palavras **deve**, **não deve** e **pode** têm o sentido da RFC 2119.
- Nomes de classes, tabelas e eventos são os nomes que o código vai usar. Mudou no código, muda aqui.
- Decisões tomadas sem confirmação explícita do usuário estão marcadas com **(proposta)**.

## Glossário

| Termo | Significado |
|---|---|
| **Contexto** | Um produto Google que o usuário pode ligar ou desligar: `gmail`, `drive`, `docs`, `sheets`, `slides`, `calendar`, `chat`, `people` |
| **Escopo** | Permissão OAuth do Google (ex.: `https://www.googleapis.com/auth/calendar`). Cada contexto exige um conjunto de escopos |
| **Ferramenta** (tool) | Função que o modelo pode chamar, ex.: `google_calendar_create_event` |
| **Namespace** | Grupo de ferramentas de um mesmo recurso (ex.: `google_calendar_event`). É o que o modelo vê primeiro no tool search |
| **Efeito** | `READ`, `WRITE` ou `DESTRUCTIVE`. Define se a chamada precisa de aprovação e com qual severidade |
| **Pré-requisito** (`depends_on`) | Ferramenta que precisa ter rodado antes de outra (aresta do DAG) |
| **Conversa** | Sequência de turnos de um usuário com o assistente |
| **Turno** | Uma mensagem do usuário e todo o trabalho do assistente até a resposta final |
| **Evento** | Linha numerada (`seq`) da conversa, gravada e enviada pelo WebSocket |
| **`...Command`** | Objeto de dados que veio do usuário (ou pedido do core a uma porta). Pacote `command` |
| **`...Data`** | Objeto de dados que veio do banco (ou de sistema externo). Pacote `data` |
| **Aprovação** | Pedido de confirmação de uma chamada `WRITE`/`DESTRUCTIVE`, de uso único e com validade |
