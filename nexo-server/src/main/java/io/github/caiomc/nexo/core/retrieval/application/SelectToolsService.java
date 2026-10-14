package io.github.caiomc.nexo.core.retrieval.application;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.catalog.domain.model.Availability;
import io.github.caiomc.nexo.core.catalog.domain.model.BlockedTool;
import io.github.caiomc.nexo.core.catalog.domain.model.ExecutionPlan;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolCatalog;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.catalog.usecase.ProvideCatalogUseCase;
import io.github.caiomc.nexo.core.retrieval.domain.enums.RetrievalStrategy;
import io.github.caiomc.nexo.core.retrieval.domain.model.Embedding;
import io.github.caiomc.nexo.core.retrieval.domain.model.RankedTool;
import io.github.caiomc.nexo.core.retrieval.domain.model.ToolSelection;
import io.github.caiomc.nexo.core.retrieval.domain.policy.LexicalTermsBuilder;
import io.github.caiomc.nexo.core.retrieval.domain.policy.ReciprocalRankFusion;
import io.github.caiomc.nexo.core.retrieval.mapper.CandidateDataMapper;
import io.github.caiomc.nexo.core.retrieval.mapper.EmbeddingDataMapper;
import io.github.caiomc.nexo.core.retrieval.mapper.RetrievalCommandMapper;
import io.github.caiomc.nexo.core.retrieval.mapper.SearchHistoryDataMapper;
import io.github.caiomc.nexo.core.retrieval.port.embedding.EmbeddingPort;
import io.github.caiomc.nexo.core.retrieval.port.history.SearchHistoryRepository;
import io.github.caiomc.nexo.core.retrieval.port.search.ToolSearchPort;
import io.github.caiomc.nexo.core.retrieval.port.search.data.LexicalCandidateData;
import io.github.caiomc.nexo.core.retrieval.usecase.SelectToolsUseCase;
import io.github.caiomc.nexo.core.retrieval.usecase.command.SelectToolsCommand;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.workspace.usecase.GetEnabledContextsUseCase;

import static java.util.Objects.isNull;

/**
 * O pipeline de seleção (doc 09), todo em Java: candidatos do banco → fusão (RRF) → filtro de
 * contextos pelo fecho (RN-02) → top-k → ordem de execução (Kahn). O banco só devolve candidatos
 * pelos índices. Os contextos ligados são lidos a cada busca: desligar vale na hora (RN-03).
 */
@Service
public class SelectToolsService implements SelectToolsUseCase {

    private static final Logger log = LoggerFactory.getLogger(SelectToolsService.class);

    private final ToolSearchPort search;
    private final EmbeddingPort embeddings;
    private final SearchHistoryRepository history;
    private final ProvideCatalogUseCase catalog;
    private final GetEnabledContextsUseCase contexts;

    private final ReciprocalRankFusion fusion;
    private final LexicalTermsBuilder terms;

    private final CandidateDataMapper candidateMapper;
    private final EmbeddingDataMapper embeddingMapper;
    private final SearchHistoryDataMapper historyMapper;
    private final RetrievalCommandMapper commands;

    private final RetrievalSettings settings;
    private final Clock clock;

    public SelectToolsService(
            ToolSearchPort search,
            EmbeddingPort embeddings,
            SearchHistoryRepository history,
            ProvideCatalogUseCase catalog,
            GetEnabledContextsUseCase contexts,
            ReciprocalRankFusion fusion,
            LexicalTermsBuilder terms,
            CandidateDataMapper candidateMapper,
            EmbeddingDataMapper embeddingMapper,
            SearchHistoryDataMapper historyMapper,
            RetrievalCommandMapper commands,
            RetrievalSettings settings,
            Clock clock
    ) {
        this.search = search;
        this.embeddings = embeddings;
        this.history = history;
        this.catalog = catalog;
        this.contexts = contexts;
        this.fusion = fusion;
        this.terms = terms;
        this.candidateMapper = candidateMapper;
        this.embeddingMapper = embeddingMapper;
        this.historyMapper = historyMapper;
        this.commands = commands;
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    public ToolSelection selectRAGDAG(SelectToolsCommand command) {
        long start = System.nanoTime();
        ToolCatalog tools = this.catalog.current();
        RetrievalStrategy strategy = this.strategy(command);
        int topK = isNull(command.topK()) ? this.settings.topK() : command.topK();
        boolean applyDag = isNull(command.applyDag()) ? this.settings.applyDag() : command.applyDag();
        Embedding queryEmbedding = strategy.needsEmbedding() ? this.embedQueryRAG(command.query()) : null;
        Set<ContextKey> enabled = isNull(command.userId()) ? null : this.contexts.enabled(this.commands.toEnabledContexts(command.userId()));

        int limit = this.settings.candidates();
        List<RankedTool> ranking = this.rankRAG(
                command,
                strategy,
                queryEmbedding,
                limit
        );
        Availability availability = tools.availabilityDAG(this.names(ranking), enabled);

        while (availability.available().size() < topK && ranking.size() >= limit && limit < this.settings.maxCandidates()) {
            limit = Math.min(limit * 2, this.settings.maxCandidates());
            ranking = this.rankRAG(
                    command,
                    strategy,
                    queryEmbedding,
                    limit
            );
            availability = tools.availabilityDAG(this.names(ranking), enabled);
        }

        List<RankedTool> rag = this.topAvailableRAGDAG(ranking, availability, topK);
        ExecutionPlan plan = applyDag && !rag.isEmpty() ? tools.graph().executionOrderDAG(this.names(rag)) : null;
        List<BlockedTool> suggestions = this.suggestionsRAGDAG(ranking, availability);

        ToolSelection selection = ToolSelection.of(
                command.query(),
                strategy,
                topK,
                rag,
                plan,
                suggestions,
                (System.nanoTime() - start) / 1_000_000
        );
        long searchId = this.history.save(
                this.historyMapper.toData(
                        selection,
                        command,
                        enabled,
                        this.clock.instant()
                )
        );

        log.debug("Busca {} ({}): {} ferramentas, {} bloqueadas", searchId, strategy, selection.tools().size(), suggestions.size());

        return selection.withSearchId(searchId);
    }

    private RetrievalStrategy strategy(SelectToolsCommand command) {
        RetrievalStrategy requested = isNull(command.strategy()) ? this.settings.strategy() : command.strategy();

        if (requested.needsEmbedding() && !this.embeddings.available()) {
            log.info("Sem chave de embeddings: a busca {} cai para FULL_TEXT", requested);

            return RetrievalStrategy.FULL_TEXT;
        }

        return requested;
    }

    private Embedding embedQueryRAG(String query) {
        return this.embeddingMapper.toEmbedding(
                this.embeddings.embed(this.embeddingMapper.toCommand(query)).getFirst()
        );
    }

    private List<RankedTool> rankRAG(
            SelectToolsCommand command,
            RetrievalStrategy strategy,
            Embedding queryEmbedding,
            int limit
    ) {
        return switch (strategy) {
            case FULL_TEXT -> this.candidateMapper.lexicalRanking(this.lexicalRAG(command.query(), limit));
            case VECTOR -> this.candidateMapper.vectorRanking(this.search.vectorCandidatesRAG(queryEmbedding, limit));
            case HYBRID -> this.fusion.fuseRAG(
                    this.candidateMapper.lexicalNames(this.lexicalRAG(command.query(), limit)),
                    this.candidateMapper.vectorNames(this.search.vectorCandidatesRAG(queryEmbedding, limit))
            );
        };
    }

    private List<LexicalCandidateData> lexicalRAG(
            String query,
            int limit
    ) {
        Optional<String> terms = this.terms.buildRAG(query);

        return terms.isEmpty() ? List.of() : this.search.lexicalCandidatesRAG(terms.get(), limit);
    }

    private List<RankedTool> topAvailableRAGDAG(
            List<RankedTool> ranking,
            Availability availability,
            int topK
    ) {
        Set<ToolName> available = Set.copyOf(availability.available());

        return ranking.stream()
                .filter(ranked -> available.contains(ranked.name()))
                .limit(topK)
                .toList();
    }

    private List<BlockedTool> suggestionsRAGDAG(
            List<RankedTool> ranking,
            Availability availability
    ) {
        Set<ToolName> best = Set.copyOf(this.names(ranking.stream().limit(this.settings.suggestionTop()).toList()));

        return availability.blocked()
                .stream()
                .filter(blocked -> best.contains(blocked.name()))
                .toList();
    }

    private List<ToolName> names(List<RankedTool> ranking) {
        return ranking.stream().map(RankedTool::name).toList();
    }
}
