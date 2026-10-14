package io.github.caiomc.nexo.core.conversation.application.agent;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.conversation.application.AgentSettings;
import io.github.caiomc.nexo.core.conversation.application.ConversationEventHubService;
import io.github.caiomc.nexo.core.conversation.domain.enums.FailureCode;
import io.github.caiomc.nexo.core.conversation.domain.exception.TurnAbortedException;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.domain.model.Usage;
import io.github.caiomc.nexo.core.conversation.domain.policy.ConversationEvents;
import io.github.caiomc.nexo.core.conversation.domain.policy.DeltaBuffer;
import io.github.caiomc.nexo.core.conversation.mapper.ModelCommandMapper;
import io.github.caiomc.nexo.core.conversation.port.model.ModelSession;
import io.github.caiomc.nexo.core.conversation.port.model.command.ToolOutputCommand;
import io.github.caiomc.nexo.core.conversation.port.model.data.ModelOutputData;
import io.github.caiomc.nexo.core.conversation.port.model.data.UsageData;

/**
 * Uma chamada ao modelo dentro do turno:
 * <ol>
 *     <li>confere o limite de chamadas por turno (RN-11);</li>
 *     <li>chama {@link ModelSession#call} com as respostas aos pedidos da chamada anterior;</li>
 *     <li>os pedaços de texto em streaming viram {@code assistant.delta}, em lotes;</li>
 *     <li>o consumo de tokens é somado ao turno, gravado e vira {@code usage.updated}.</li>
 * </ol>
 */
@Service
public class CallModelService {

    private final ConversationEventHubService hub;
    private final RecordTurnStateService recordTurnState;

    private final ConversationEvents events;
    private final ModelCommandMapper modelCommands;

    private final AgentSettings settings;
    private final Clock clock;

    public CallModelService(
            ConversationEventHubService hub,
            RecordTurnStateService recordTurnState,
            ConversationEvents events,
            ModelCommandMapper modelCommands,
            AgentSettings settings,
            Clock clock
    ) {
        this.hub = hub;
        this.recordTurnState = recordTurnState;
        this.events = events;
        this.modelCommands = modelCommands;
        this.settings = settings;
        this.clock = clock;
    }

    public ModelOutputData call(
            TurnRun run,
            ModelSession session,
            List<ToolOutputCommand> toolOutputs
    ) {
        if (run.turn().usage().modelCalls() >= this.settings.maxModelCalls()) {
            throw new TurnAbortedException(FailureCode.LIMIT_MODEL_CALLS);
        }

        DeltaBuffer buffer = new DeltaBuffer(this.clock, this.settings.deltaWindow());
        ModelOutputData output = session.call(
                this.modelCommands.toCall(toolOutputs),
                delta -> buffer.add(delta.text()).ifPresent(text -> this.publishDelta(run, text))
        );

        buffer.drain().ifPresent(text -> this.publishDelta(run, text));
        run.ensureCanContinue(this.settings.turnTimeout());
        this.recordUsage(run, output.usage());

        return output;
    }

    private void publishDelta(
            TurnRun run,
            String text
    ) {
        this.hub.publish(run.turn().conversationId(), this.events.assistantDelta(run.turn().id(), text));
    }

    private void recordUsage(
            TurnRun run,
            UsageData spent
    ) {
        Turn turn = run.turn();
        Usage usage = turn.usage().plus(
                spent.inputTokens(),
                spent.cachedInputTokens(),
                spent.outputTokens()
        );

        this.recordTurnState.record(run, turn.withUsage(usage));
        this.hub.publish(turn.conversationId(), this.events.usageUpdated(turn.id(), usage));
    }
}
