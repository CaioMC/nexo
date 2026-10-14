# ADR-0001 · Clean Architecture + DDD

**Status:** Proposta · **Data:** 2026-10-13

## Contexto

O Nexo junta cinco assuntos que mudam por motivos diferentes: identidade, contextos, catálogo,
busca e conversa. Os projetos anteriores (alien-code, tool-search-pg) já usam Clean Architecture
com as regras das skills `java-clean-ddd` e `java-code-style`, e o usuário quer o mesmo padrão.

## Decisão

- Cinco contextos delimitados em `core/` (`identity`, `workspace`, `catalog`, `retrieval`, `conversation`) + shared kernel mínimo (`UserId`, `ContextKey`).
- Pacotes segregados (`domain/model`, `domain/exception`, `port/<assunto>`, `usecase`, `usecase/command`, `application`); adapters por tecnologia.
- DTOs só dados + `*WebMapper`; estilo `this.`, `isNull`/`nonNull`, um parâmetro por linha.
- Um contexto usa outro só pelos casos de uso.

## Alternativas

| Alternativa | Por que não |
|---|---|
| Camadas tradicionais (controller/service/repository) | Mistura regra e tecnologia; o tool-search-pg mostrou o ganho de testar o core sem Spring |
| Módulos Maven por contexto | Custo de build alto para uma POC; pacotes + verificador bastam. Reavaliar se crescer |
| Spring Modulith | Bom candidato futuro; hoje adicionaria uma dependência sem necessidade |

## Consequências

- Mais arquivos (um record por arquivo, mappers), compensados por testes rápidos e fronteiras claras.
- Verificadores das skills fazem parte do DoD.
