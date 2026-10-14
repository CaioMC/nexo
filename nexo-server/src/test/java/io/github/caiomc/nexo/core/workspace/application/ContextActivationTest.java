package io.github.caiomc.nexo.core.workspace.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.caiomc.nexo.core.identity.usecase.GetGrantedScopesUseCase;
import io.github.caiomc.nexo.core.identity.usecase.command.GetGrantedScopesCommand;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;
import io.github.caiomc.nexo.core.workspace.domain.exception.ContextNotFoundException;
import io.github.caiomc.nexo.core.workspace.domain.exception.ScopesNotGrantedException;
import io.github.caiomc.nexo.core.workspace.domain.model.ContextActivation;
import io.github.caiomc.nexo.core.workspace.mapper.GoogleContextDataMapper;
import io.github.caiomc.nexo.core.workspace.mapper.UserContextDataMapper;
import io.github.caiomc.nexo.core.workspace.mapper.WorkspaceCommandMapper;
import io.github.caiomc.nexo.core.workspace.port.notification.ContextChangeNotifierPort;
import io.github.caiomc.nexo.core.workspace.port.notification.command.NotifyContextChangeCommand;
import io.github.caiomc.nexo.core.workspace.port.repository.GoogleContextRepository;
import io.github.caiomc.nexo.core.workspace.port.repository.UserContextRepository;
import io.github.caiomc.nexo.core.workspace.port.repository.data.GoogleContextData;
import io.github.caiomc.nexo.core.workspace.port.repository.data.GoogleContextScopeData;
import io.github.caiomc.nexo.core.workspace.port.repository.data.UserContextData;
import io.github.caiomc.nexo.core.workspace.usecase.command.CompleteConsentCommand;
import io.github.caiomc.nexo.core.workspace.usecase.command.EnableContextCommand;
import io.github.caiomc.nexo.core.workspace.usecase.command.GetEnabledContextsCommand;
import io.github.caiomc.nexo.core.workspace.usecase.command.ReconcileContextsCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ligar, concluir consentimento e reconciliar (doc 05): RN-01 nos três caminhos.
 */
class ContextActivationTest {

    private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");
    private static final String CHAT = "https://www.googleapis.com/auth/chat.spaces.readonly";
    private static final String CHAT_SEND = "https://www.googleapis.com/auth/chat.messages.create";
    private static final String CALENDAR = "https://www.googleapis.com/auth/calendar";

    private final UserId ana = UserId.of(UUID.randomUUID());
    private final Map<UUID, Map<String, UserContextData>> stored = new HashMap<>();
    private final List<NotifyContextChangeCommand> notified = new ArrayList<>();
    private Set<String> granted = Set.of(CALENDAR);

    private final GoogleContextRepository seed = new GoogleContextRepository() {
        @Override
        public List<GoogleContextData> contexts() {
            return List.of(
                    new GoogleContextData("calendar", "Calendar", "Agenda", "calendar-json.googleapis.com", 6),
                    new GoogleContextData("chat", "Chat", "Mensagens", "chat.googleapis.com", 7)
            );
        }

        @Override
        public List<GoogleContextScopeData> scopes() {
            return List.of(new GoogleContextScopeData("calendar", CALENDAR), new GoogleContextScopeData("chat", CHAT), new GoogleContextScopeData("chat", CHAT_SEND));
        }
    };

    private final UserContextRepository userContexts = new UserContextRepository() {
        @Override
        public List<UserContextData> findByUser(UUID userId) {
            return List.copyOf(ContextActivationTest.this.stored.getOrDefault(userId, Map.of()).values());
        }

        @Override
        public void save(UserContextData context) {
            ContextActivationTest.this.stored.computeIfAbsent(context.userId(), key -> new HashMap<>()).put(context.contextKey(), context);
        }
    };

    private final GetGrantedScopesUseCase scopes = (GetGrantedScopesCommand command) -> this.granted;
    private final ContextChangeNotifierPort notifier = this.notified::add;
    private final ContextDirectoryService directory = new ContextDirectoryService(this.seed, new GoogleContextDataMapper());
    private final GetEnabledContextsService reader = new GetEnabledContextsService(this.userContexts, new UserContextDataMapper());
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final EnableContextService enable = new EnableContextService(
            this.directory,
            this.userContexts,
            this.scopes,
            command -> "/oauth2/authorization/google?context=" + command.context(),
            this.notifier,
            new UserContextDataMapper(),
            new WorkspaceCommandMapper(),
            this.clock
    );

    private final CompleteConsentService complete = new CompleteConsentService(
            this.directory,
            this.userContexts,
            this.notifier,
            new UserContextDataMapper(),
            new WorkspaceCommandMapper(),
            this.clock
    );

    private final ReconcileContextsService reconcile = new ReconcileContextsService(
            this.directory,
            this.reader,
            this.userContexts,
            this.notifier,
            new UserContextDataMapper(),
            new WorkspaceCommandMapper(),
            this.clock
    );

    @Test
    void grantedScopesEnableRightAwayAndNotify() {
        ContextActivation activation = this.enable.enable(new EnableContextCommand(this.ana, "calendar"));

        assertThat(activation).isInstanceOf(ContextActivation.Enabled.class);
        assertThat(this.reader.enabled(new GetEnabledContextsCommand(this.ana))).containsExactly(ContextKey.of("calendar"));
        assertThat(this.notified).singleElement().satisfies(command -> assertThat(command.enabled()).isTrue());
    }

    @Test
    void missingScopesAskForIncrementalConsentAndKeepItOff() {
        ContextActivation activation = this.enable.enable(new EnableContextCommand(this.ana, "chat"));

        assertThat(activation).isInstanceOfSatisfying(
                ContextActivation.ConsentRequired.class,
                consent -> assertThat(consent.authorizationUrl()).isEqualTo("/oauth2/authorization/google?context=chat")
        );
        assertThat(this.stored).isEmpty();
        assertThat(this.notified).isEmpty();
    }

    @Test
    void partialConsentLeavesTheContextOff() {
        assertThatThrownBy(() -> this.complete.complete(new CompleteConsentCommand(this.ana, "chat", Set.of(CALENDAR, CHAT))))
                .isInstanceOf(ScopesNotGrantedException.class)
                .hasMessageContaining(CHAT_SEND);
        assertThat(this.stored).isEmpty();

        this.complete.complete(new CompleteConsentCommand(this.ana, "chat", Set.of(CALENDAR, CHAT, CHAT_SEND)));

        assertThat(this.stored.get(this.ana.value()).get("chat").enabled()).isTrue();
    }

    @Test
    void scopeThatDisappearedTurnsTheContextOff() {
        this.enable.enable(new EnableContextCommand(this.ana, "calendar"));
        this.notified.clear();

        this.reconcile.reconcile(new ReconcileContextsCommand(this.ana, Set.of()));

        assertThat(this.stored.get(this.ana.value()).get("calendar").enabled()).isFalse();
        assertThat(this.notified).singleElement().satisfies(command -> assertThat(command.enabled()).isFalse());
    }

    @Test
    void unknownContextIs404() {
        assertThatThrownBy(() -> this.enable.enable(new EnableContextCommand(this.ana, "fax"))).isInstanceOf(ContextNotFoundException.class);
    }
}
