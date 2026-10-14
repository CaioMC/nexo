package io.github.caiomc.nexo.core.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolCatalog;
import io.github.caiomc.nexo.core.catalog.usecase.ProvideCatalogUseCase;
import io.github.caiomc.nexo.core.retrieval.domain.model.Embedding;
import io.github.caiomc.nexo.core.retrieval.port.embedding.EmbeddingIndexRepository;
import io.github.caiomc.nexo.core.retrieval.port.embedding.EmbeddingPort;
import io.github.caiomc.nexo.core.retrieval.port.embedding.command.EmbedTextsCommand;
import io.github.caiomc.nexo.core.retrieval.port.embedding.data.EmbeddingData;
import io.github.caiomc.nexo.core.retrieval.port.embedding.data.IndexedEmbeddingData;
import io.github.caiomc.nexo.core.retrieval.port.embedding.data.ToolEmbeddingData;
import io.github.caiomc.nexo.core.retrieval.port.history.SearchHistoryRepository;
import io.github.caiomc.nexo.core.retrieval.port.history.data.ToolSearchData;
import io.github.caiomc.nexo.core.retrieval.port.search.ToolSearchPort;
import io.github.caiomc.nexo.core.retrieval.port.search.data.LexicalCandidateData;
import io.github.caiomc.nexo.core.retrieval.port.search.data.VectorCandidateData;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.workspace.usecase.GetEnabledContextsUseCase;
import io.github.caiomc.nexo.core.workspace.usecase.command.GetEnabledContextsCommand;

/**
 * Dublês escritos à mão para os testes do core de retrieval: sem Spring, sem banco, sem rede.
 */
public final class RetrievalTestDoubles {

    private RetrievalTestDoubles() {
    }

    public static final class FixedCatalog implements ProvideCatalogUseCase {

        private final ToolCatalog catalog;

        public FixedCatalog(ToolCatalog catalog) {
            this.catalog = catalog;
        }

        @Override
        public ToolCatalog current() {
            return this.catalog;
        }
    }

    /**
     * Devolve listas fixas, cortadas pelo limite, e anota os termos e limites pedidos.
     */
    public static final class ScriptedSearch implements ToolSearchPort {

        public final List<String> termsAsked = new ArrayList<>();
        public final List<Integer> limitsAsked = new ArrayList<>();

        private final List<LexicalCandidateData> lexical;
        private final List<VectorCandidateData> vector;

        public ScriptedSearch(
                List<LexicalCandidateData> lexical,
                List<VectorCandidateData> vector
        ) {
            this.lexical = lexical;
            this.vector = vector;
        }

        @Override
        public List<LexicalCandidateData> lexicalCandidatesRAG(
                String terms,
                int limit
        ) {
            this.termsAsked.add(terms);
            this.limitsAsked.add(limit);

            return this.lexical.stream().limit(limit).toList();
        }

        @Override
        public List<VectorCandidateData> vectorCandidatesRAG(
                Embedding query,
                int limit
        ) {
            this.limitsAsked.add(limit);

            return this.vector.stream().limit(limit).toList();
        }
    }

    public static final class FakeEmbeddings implements EmbeddingPort {

        public final List<List<String>> requests = new ArrayList<>();

        private final boolean available;

        public FakeEmbeddings(boolean available) {
            this.available = available;
        }

        @Override
        public List<EmbeddingData> embed(EmbedTextsCommand command) {
            this.requests.add(command.texts());

            return command.texts().stream().map(text -> new EmbeddingData(new double[] {text.length(), 1})).toList();
        }

        @Override
        public String model() {
            return "modelo-teste";
        }

        @Override
        public boolean available() {
            return this.available;
        }
    }

    public static final class InMemoryEmbeddingIndex implements EmbeddingIndexRepository {

        public final Map<String, ToolEmbeddingData> rows = new HashMap<>();

        @Override
        public List<IndexedEmbeddingData> indexed(String model) {
            return this.rows.values()
                    .stream()
                    .filter(row -> row.model().equals(model))
                    .map(row -> new IndexedEmbeddingData(row.toolName(), row.contentHash()))
                    .toList();
        }

        @Override
        public void save(ToolEmbeddingData embedding) {
            this.rows.put(embedding.toolName(), embedding);
        }
    }

    public static final class InMemorySearchHistory implements SearchHistoryRepository {

        public final List<ToolSearchData> saved = new ArrayList<>();

        @Override
        public long save(ToolSearchData search) {
            this.saved.add(search);

            return this.saved.size();
        }

        @Override
        public List<ToolSearchData> findByUser(
                UUID userId,
                int limit
        ) {
            return this.saved.stream()
                    .filter(search -> userId.equals(search.userId()))
                    .limit(limit)
                    .toList();
        }
    }

    /**
     * Contextos ligados, iguais para qualquer usuário.
     */
    public static final class FixedContexts implements GetEnabledContextsUseCase {

        public Set<ContextKey> enabled = Set.of();

        @Override
        public Set<ContextKey> enabled(GetEnabledContextsCommand command) {
            return this.enabled;
        }
    }
}
