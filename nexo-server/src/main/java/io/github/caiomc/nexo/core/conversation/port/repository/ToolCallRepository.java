package io.github.caiomc.nexo.core.conversation.port.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import io.github.caiomc.nexo.core.conversation.port.repository.data.ExecutedToolData;
import io.github.caiomc.nexo.core.conversation.port.repository.data.ToolCallData;

public interface ToolCallRepository {

    /**
     * Grava ou atualiza a chamada (chave: turno + {@code call_id}).
     */
    void save(ToolCallData call);

    /**
     * Ferramentas que rodaram com sucesso na conversa desde {@code since} (RN-04).
     */
    List<ExecutedToolData> succeededSince(
            UUID conversationId,
            Instant since
    );

    List<ToolCallData> findByTurns(Collection<UUID> turnIds);
}
