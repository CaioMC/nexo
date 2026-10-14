# Decisões de arquitetura (ADR)

Formato MADR curto: contexto, decisão, alternativas e consequências. ADR aceita não se edita;
uma decisão nova substitui a antiga ("Substituída por ADR-00xx").

| # | Decisão | Status |
|---|---|---|
| [0001](0001-clean-architecture-ddd.md) | Clean Architecture + DDD com os padrões das skills | Proposta |
| [0002](0002-websocket-com-replay-por-seq.md) | WebSocket bidirecional com Event Store e replay por `seq` | Proposta |
| [0003](0003-postgres-pgvector-catalogo-no-banco.md) | PostgreSQL + pgvector guarda e indexa; RAG e DAG no código | Proposta (revisada) |
| [0004](0004-oauth-google-bff.md) | Login só Google, BFF com sessão no servidor e consentimento incremental | Proposta |
| [0005](0005-tool-search-cliente-rag-dag.md) | Tool search no cliente com RAG híbrido + DAG | Proposta |
| [0006](0006-aprovacao-de-mutacoes.md) | Aprovação humana de uso único para toda mutação | Proposta |
| [0007](0007-google-rest-e-llm-atras-de-portas.md) | APIs Google por REST e LLM atrás de portas, sem SDKs no core | Proposta |
| [0008](0008-convencao-command-data.md) | Objetos simples: `...Command` (do usuário) e `...Data` (do banco), em pacotes próprios | Proposta |
