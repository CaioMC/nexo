package io.github.caiomc.nexo.core.retrieval.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.caiomc.nexo.core.catalog.domain.model.BlockedTool;
import io.github.caiomc.nexo.core.retrieval.domain.enums.RetrievalStrategy;
import io.github.caiomc.nexo.core.retrieval.domain.enums.SelectionOrigin;
import io.github.caiomc.nexo.core.retrieval.domain.model.SelectedTool;
import io.github.caiomc.nexo.core.retrieval.domain.model.ToolSelection;
import io.github.caiomc.nexo.core.retrieval.domain.policy.LexicalTermsBuilder;
import io.github.caiomc.nexo.core.retrieval.domain.policy.ReciprocalRankFusion;
import io.github.caiomc.nexo.core.retrieval.mapper.CandidateDataMapper;
import io.github.caiomc.nexo.core.retrieval.mapper.EmbeddingDataMapper;
import io.github.caiomc.nexo.core.retrieval.mapper.RetrievalCommandMapper;
import io.github.caiomc.nexo.core.retrieval.mapper.SearchHistoryDataMapper;
import io.github.caiomc.nexo.core.retrieval.port.history.data.ToolSearchResultData;
import io.github.caiomc.nexo.core.retrieval.port.search.data.LexicalCandidateData;
import io.github.caiomc.nexo.core.retrieval.port.search.data.VectorCandidateData;
import io.github.caiomc.nexo.core.retrieval.usecase.command.SelectToolsCommand;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;
import io.github.caiomc.nexo.core.support.CatalogFixtures;
import io.github.caiomc.nexo.core.support.RetrievalTestDoubles.FakeEmbeddings;
import io.github.caiomc.nexo.core.support.RetrievalTestDoubles.FixedCatalog;
import io.github.caiomc.nexo.core.support.RetrievalTestDoubles.FixedContexts;
import io.github.caiomc.nexo.core.support.RetrievalTestDoubles.InMemorySearchHistory;
import io.github.caiomc.nexo.core.support.RetrievalTestDoubles.ScriptedSearch;

import static io.github.caiomc.nexo.core.support.CatalogFixtures.CALENDAR;
import static io.github.caiomc.nexo.core.support.CatalogFixtures.DOCS;
import static io.github.caiomc.nexo.core.support.CatalogFixtures.GMAIL;
import static io.github.caiomc.nexo.core.support.CatalogFixtures.name;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class SelectToolsServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");

    private static final UserId USER = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    private final InMemorySearchHistory history = new InMemorySearchHistory();
    private final FixedContexts contexts = new FixedContexts();

    @Test
    void hybridFusesBothListsAndTheDagAddsPrerequisitesInOrder() {
        ScriptedSearch search = new ScriptedSearch(
                List.of(this.lexical("calendar_create_event", 0.5), this.lexical("gmail_search", 0.1)),
                List.of(this.vector("calendar_create_event", 0.1), this.vector("calendar_list_events", 0.2))
        );

        ToolSelection selection = this.service(search, true).selectRAGDAG(this.command("marque reunião amanhã", RetrievalStrategy.HYBRID, 2, null));

        assertThat(selection.strategy()).isEqualTo(RetrievalStrategy.HYBRID);
        assertThat(selection.tools())
                .extracting(SelectedTool::name, SelectedTool::origin, SelectedTool::level)
                .containsExactly(
                        tuple(name("calendar_list_calendars"), SelectionOrigin.DAG, 0),
                        tuple(name("calendar_list_events"), SelectionOrigin.RAG, 1),
                        tuple(name("calendar_create_event"), SelectionOrigin.RAG, 2)
                );
        assertThat(selection.searchId()).isEqualTo(1L);
        assertThat(search.termsAsked).containsExactly("marque | reuniao | amanha");
    }

    @Test
    void withoutEmbeddingsFallsBackToFullText() {
        ScriptedSearch search = new ScriptedSearch(List.of(this.lexical("docs_read", 0.3)), List.of());

        ToolSelection selection = this.service(search, false).selectRAGDAG(this.command("ler documento", RetrievalStrategy.HYBRID, 3, null));

        assertThat(selection.strategy()).isEqualTo(RetrievalStrategy.FULL_TEXT);
        assertThat(selection.names()).containsExactly(name("docs_read"));
    }

    @Test
    void disabledContextsNeverReachTheModelAndBecomeSuggestions() {
        ScriptedSearch search = new ScriptedSearch(
                List.of(this.lexical("gmail_search", 0.9), this.lexical("docs_read", 0.5), this.lexical("docs_summarize_shared", 0.4)),
                List.of()
        );

        ToolSelection selection = this.service(search, false).selectRAGDAG(this.command("resuma os e-mails", RetrievalStrategy.FULL_TEXT, 5, Set.of(DOCS)));

        assertThat(selection.names()).containsExactly(name("docs_read"));
        assertThat(selection.blocked())
                .extracting(BlockedTool::name)
                .containsExactly(name("gmail_search"), name("docs_summarize_shared"));
        assertThat(this.history.saved.getFirst().results())
                .extracting(ToolSearchResultData::toolName, ToolSearchResultData::origin)
                .containsExactly(
                        tuple("docs_read", "RAG"),
                        tuple("gmail_search", "BLOCKED"),
                        tuple("docs_summarize_shared", "BLOCKED")
                );
        assertThat(this.history.saved.getFirst().allowedContexts()).containsExactly("docs");
        assertThat(this.history.saved.getFirst().userId()).isEqualTo(USER.value());
    }

    @Test
    void withoutDagKeepsRagOrder() {
        ScriptedSearch search = new ScriptedSearch(List.of(this.lexical("calendar_get_event", 0.9), this.lexical("calendar_list_events", 0.5)), List.of());

        this.contexts.enabled = Set.of(CALENDAR, GMAIL);

        ToolSelection selection = this.service(search, false).selectRAGDAG(new SelectToolsCommand("evento", RetrievalStrategy.FULL_TEXT, 5, false, USER, null));

        assertThat(selection.dagApplied()).isFalse();
        assertThat(selection.names()).containsExactly(name("calendar_get_event"), name("calendar_list_events"));
    }

    @Test
    void asksForMoreCandidatesWhenTheFilterLeavesTooFew() {
        ScriptedSearch search = new ScriptedSearch(
                List.of(this.lexical("gmail_search", 0.9), this.lexical("calendar_list_events", 0.5), this.lexical("docs_read", 0.4)),
                List.of()
        );

        this.service(search, false, 2, 4).selectRAGDAG(this.command("qualquer coisa", RetrievalStrategy.FULL_TEXT, 2, Set.of(DOCS, CALENDAR)));

        assertThat(search.limitsAsked).containsExactly(2, 4);
    }

    private SelectToolsService service(
            ScriptedSearch search,
            boolean embeddingsAvailable
    ) {
        return this.service(search, embeddingsAvailable, 50, 400);
    }

    private SelectToolsService service(
            ScriptedSearch search,
            boolean embeddingsAvailable,
            int candidates,
            int maxCandidates
    ) {
        return new SelectToolsService(
                search,
                new FakeEmbeddings(embeddingsAvailable),
                this.history,
                new FixedCatalog(CatalogFixtures.catalog()),
                this.contexts,
                new ReciprocalRankFusion(60),
                new LexicalTermsBuilder(),
                new CandidateDataMapper(),
                new EmbeddingDataMapper(),
                new SearchHistoryDataMapper(),
                new RetrievalCommandMapper(),
                new RetrievalSettings(RetrievalStrategy.HYBRID, 2, candidates, maxCandidates, 60, true, 3),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private SelectToolsCommand command(
            String query,
            RetrievalStrategy strategy,
            int topK,
            Set<ContextKey> enabled
    ) {
        if (nonNull(enabled)) {
            this.contexts.enabled = enabled;
        }

        return new SelectToolsCommand(
                query,
                strategy,
                topK,
                true,
                isNull(enabled) ? null : USER,
                null
        );
    }

    private LexicalCandidateData lexical(
            String tool,
            double rank
    ) {
        return new LexicalCandidateData(tool, rank);
    }

    private VectorCandidateData vector(
            String tool,
            double distance
    ) {
        return new VectorCandidateData(tool, distance);
    }
}
