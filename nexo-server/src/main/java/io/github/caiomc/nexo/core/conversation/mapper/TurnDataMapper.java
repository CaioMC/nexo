package io.github.caiomc.nexo.core.conversation.mapper;

import java.time.Instant;
import java.util.Collection;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.conversation.domain.enums.FailureCode;
import io.github.caiomc.nexo.core.conversation.domain.enums.TurnStatus;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.domain.model.Usage;
import io.github.caiomc.nexo.core.conversation.port.repository.data.TurnData;
import io.github.caiomc.nexo.core.conversation.usecase.command.SendMessageCommand;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

/**
 * {@link TurnData} ⇄ {@link Turn}.
 */
@Component
public class TurnDataMapper {

    public Turn toTurn(TurnData data) {
        return new Turn(
                data.id(),
                data.conversationId(),
                UserId.of(data.userId()),
                data.clientMessageId(),
                data.userMessage(),
                data.assistantMessage(),
                TurnStatus.valueOf(data.status()),
                isNull(data.failureCode()) ? null : FailureCode.valueOf(data.failureCode()),
                data.model(),
                data.enabledContexts().stream().map(ContextKey::of).toList(),
                new Usage(
                        data.modelCalls(),
                        data.inputTokens(),
                        data.cachedInputTokens(),
                        data.outputTokens()
                ),
                data.startedAt(),
                data.finishedAt()
        );
    }

    public TurnData toData(Turn turn) {
        return new TurnData(
                turn.id(),
                turn.conversationId(),
                turn.userId().value(),
                turn.clientMessageId(),
                turn.userMessage(),
                turn.assistantMessage(),
                turn.status().name(),
                isNull(turn.failure()) ? null : turn.failure().name(),
                turn.model(),
                turn.enabledContexts().stream().map(ContextKey::value).toList(),
                turn.usage().modelCalls(),
                turn.usage().inputTokens(),
                turn.usage().cachedInputTokens(),
                turn.usage().outputTokens(),
                turn.startedAt(),
                turn.finishedAt()
        );
    }

    public Turn start(
            SendMessageCommand command,
            String model,
            Collection<ContextKey> contexts,
            Instant now
    ) {
        return Turn.start(
                command.conversationId(),
                command.userId(),
                command.clientMessageId(),
                command.text(),
                model,
                contexts.stream().sorted().toList(),
                now
        );
    }
}
