package io.github.caiomc.nexo.core.conversation.port.repository.data;

import java.time.Instant;

/**
 * Uma ferramenta que rodou com sucesso na conversa (RN-04).
 */
public record ExecutedToolData(
        String toolName,
        Instant finishedAt
) {
}
