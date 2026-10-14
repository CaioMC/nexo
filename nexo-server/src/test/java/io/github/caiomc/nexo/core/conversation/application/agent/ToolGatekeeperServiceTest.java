package io.github.caiomc.nexo.core.conversation.application.agent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.caiomc.nexo.core.conversation.domain.enums.BlockReason;
import io.github.caiomc.nexo.core.conversation.domain.enums.Severity;
import io.github.caiomc.nexo.core.conversation.domain.enums.ToolExposure;
import io.github.caiomc.nexo.core.conversation.application.AgentSettings;
import io.github.caiomc.nexo.core.conversation.domain.model.Admission;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.domain.policy.ArgumentsHasher;
import io.github.caiomc.nexo.core.conversation.domain.policy.ArgumentsValidator;
import io.github.caiomc.nexo.core.conversation.mapper.AgentCommandMapper;
import io.github.caiomc.nexo.core.conversation.mapper.ExecuteToolCommandMapper;
import io.github.caiomc.nexo.core.conversation.mapper.ExecutedToolMapper;
import io.github.caiomc.nexo.core.conversation.mapper.ToolApprovalDataMapper;
import io.github.caiomc.nexo.core.conversation.port.model.data.FunctionCallData;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;
import io.github.caiomc.nexo.core.support.CatalogFixtures;
import io.github.caiomc.nexo.core.support.ConversationTestDoubles.FixedDescriber;
import io.github.caiomc.nexo.core.support.ConversationTestDoubles.RecordingToolCalls;
import io.github.caiomc.nexo.core.support.RetrievalTestDoubles.FixedCatalog;
import io.github.caiomc.nexo.core.support.RetrievalTestDoubles.FixedContexts;

import static io.github.caiomc.nexo.core.support.CatalogFixtures.CALENDAR;
import static io.github.caiomc.nexo.core.support.CatalogFixtures.DOCS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * As regras do porteiro (doc 11, seção 4), na ordem: existe, contexto, argumentos, pré-requisitos, efeito.
 */
class ToolGatekeeperServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");

    private final FixedContexts contexts = new FixedContexts();
    private final RecordingToolCalls calls = new RecordingToolCalls();
    private final ArgumentsHasher hasher = new ArgumentsHasher();

    private final Turn turn = Turn.start(UUID.randomUUID(), UserId.of(UUID.randomUUID()), UUID.randomUUID(), "marque reunião", "m", List.of(), NOW);

    private final ToolGatekeeperService gatekeeper = new ToolGatekeeperService(
            new FixedCatalog(CatalogFixtures.catalog()),
            this.contexts,
            this.calls,
            new FixedDescriber(),
            new ArgumentsValidator(),
            this.hasher,
            new ExecuteToolCommandMapper(),
            new ExecutedToolMapper(),
            new ToolApprovalDataMapper(),
            new AgentCommandMapper(),
            new AgentSettings(
                    12,
                    20,
                    Duration.ofMinutes(3),
                    Duration.ofMinutes(15),
                    Duration.ofMinutes(30),
                    20,
                    3,
                    Duration.ofMillis(50),
                    ToolExposure.CLIENT_TOOL_SEARCH,
                    ZoneOffset.UTC
            ),
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void unknownToolIsAnErrorForTheModel() {
        assertThat(this.admit("calendar_teleport")).isInstanceOf(Admission.Unknown.class);
        assertThat(this.admit("Nome Inválido")).isInstanceOf(Admission.Unknown.class);
    }

    @Test
    void disabledContextBlocksEvenWithTheDefinitionLoaded() {
        this.contexts.enabled = Set.of(DOCS);

        Admission admission = this.admit("calendar_list_calendars");

        assertThat(admission).isInstanceOfSatisfying(Admission.Blocked.class, blocked -> {
            assertThat(blocked.reason()).isEqualTo(BlockReason.CONTEXT_DISABLED);
            assertThat(blocked.missing()).containsExactly("calendar");
        });
    }

    @Test
    void argumentsOutsideTheSchemaAreInvalid() {
        this.contexts.enabled = Set.of(CALENDAR);

        Admission admission = this.gatekeeper.admit(this.turn, new FunctionCallData("c1", "calendar_list_calendars", Map.of(), "os argumentos não são um JSON válido: {"));

        assertThat(admission).isInstanceOfSatisfying(Admission.Invalid.class, invalid -> assertThat(invalid.errors()).hasSize(1));
    }

    @Test
    void missingDirectPrerequisiteBlocksWithinTheWindow() {
        this.contexts.enabled = Set.of(CALENDAR);
        this.calls.ran("calendar_list_calendars");

        Admission admission = this.admit("calendar_create_event");

        assertThat(admission).isInstanceOfSatisfying(Admission.Blocked.class, blocked -> {
            assertThat(blocked.reason()).isEqualTo(BlockReason.MISSING_PREREQUISITES);
            assertThat(blocked.missing()).containsExactly("calendar_list_events");
        });
        assertThat(this.calls.askedSince).isEqualTo(NOW.minus(Duration.ofMinutes(30)));
    }

    @Test
    void readRunsDirectly() {
        this.contexts.enabled = Set.of(CALENDAR);

        assertThat(this.admit("calendar_list_calendars")).isInstanceOfSatisfying(Admission.Run.class, run -> {
            assertThat(run.command().context()).isEqualTo(CALENDAR);
            assertThat(run.command().userId()).isEqualTo(this.turn.userId());
        });
    }

    @Test
    void writeWaitsForAWarningApprovalBoundToTheArgumentsHash() {
        this.contexts.enabled = Set.of(CALENDAR);
        this.calls.ran("calendar_list_events");

        Admission admission = this.admit("calendar_create_event");

        assertThat(admission).isInstanceOfSatisfying(Admission.AwaitApproval.class, waiting -> {
            assertThat(waiting.approval().severity()).isEqualTo(Severity.WARNING);
            assertThat(waiting.approval().argumentsHash()).isEqualTo(this.hasher.hash(Map.of()));
            assertThat(waiting.approval().callId()).isEqualTo("c1");
            assertThat(waiting.approval().expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
            assertThat(waiting.approval().description()).contains("calendar_create_event");
        });
    }

    @Test
    void destructiveIsDanger() {
        this.contexts.enabled = Set.of(CALENDAR);
        this.calls.ran("calendar_get_event");

        assertThat(this.admit("calendar_delete_event")).isInstanceOfSatisfying(
                Admission.AwaitApproval.class,
                waiting -> assertThat(waiting.approval().severity()).isEqualTo(Severity.DANGER)
        );
    }

    private Admission admit(String tool) {
        return this.gatekeeper.admit(this.turn, new FunctionCallData("c1", tool, Map.of(), null));
    }
}
