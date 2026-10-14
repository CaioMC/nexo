# ADR-0007 · APIs Google por REST e LLM atrás de portas

**Status:** Proposta · **Data:** 2026-10-13

## Contexto

Há bibliotecas oficiais Java para cada API do Google e SDK Java da OpenAI. O google-tools chamava
as APIs REST diretamente; o tool-search-pg falava com a OpenAI por um `OpenAiHttpClient` próprio.

## Decisão

- Google: `RestClient` do Spring, um `*Executor` por contexto no adapter, `GoogleApiClient` comum (token, retry, erros).
- LLM e embeddings: `LanguageModelPort` e `EmbeddingPort` no core; adapter OpenAI (Responses API com streaming SSE) por HTTP próprio, como no tool-search-pg.
- Modelo padrão `gpt-5.4-mini`, embeddings `text-embedding-3-small` (1536): **proposta**, configuráveis.

## Alternativas

| Alternativa | Por que não |
|---|---|
| `google-api-services-*` (8 bibliotecas) | Muitas dependências e modelos gerados grandes para 50 chamadas simples |
| SDK Java da OpenAI | Válido; o cliente HTTP próprio já existe no tool-search-pg e dá controle fino do SSE e do `tool_search` |
| Spring AI | Ainda não expõe o tool search no cliente da Responses API |

## Consequências

- Mais código de mapeamento nos executores, testado com `HttpServer` do JDK e JSON gravado.
- Trocar de provedor de LLM = novo adapter.
