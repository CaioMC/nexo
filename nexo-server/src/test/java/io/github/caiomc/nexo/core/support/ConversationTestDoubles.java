package io.github.caiomc.nexo.core.support;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import io.github.caiomc.nexo.core.conversation.port.approval.ApprovalDescriberPort;
import io.github.caiomc.nexo.core.conversation.port.approval.command.DescribeApprovalCommand;
import io.github.caiomc.nexo.core.conversation.port.repository.ToolCallRepository;
import io.github.caiomc.nexo.core.conversation.port.repository.data.ExecutedToolData;
import io.github.caiomc.nexo.core.conversation.port.repository.data.ToolCallData;

/**
 * Dublês escritos à mão para os testes do core de conversa.
 */
public final class ConversationTestDoubles {

    private ConversationTestDoubles() {
    }

    /**
     * Ferramentas que "já rodaram" na conversa, para a trava do DAG.
     */
    public static final class RecordingToolCalls implements ToolCallRepository {

        public final List<ExecutedToolData> executed = new ArrayList<>();
        public final List<ToolCallData> saved = new ArrayList<>();
        public Instant askedSince;

        public void ran(String tool) {
            this.executed.add(new ExecutedToolData(tool, Instant.parse("2026-10-13T11:55:00Z")));
        }

        @Override
        public void save(ToolCallData call) {
            this.saved.add(call);
        }

        @Override
        public List<ExecutedToolData> succeededSince(
                UUID conversationId,
                Instant since
        ) {
            this.askedSince = since;

            return this.executed;
        }

        @Override
        public List<ToolCallData> findByTurns(Collection<UUID> turnIds) {
            return List.of();
        }
    }

    public static final class FixedDescriber implements ApprovalDescriberPort {

        @Override
        public String describe(DescribeApprovalCommand command) {
            return "Executar " + command.tool() + " com " + command.arguments();
        }
    }
}
