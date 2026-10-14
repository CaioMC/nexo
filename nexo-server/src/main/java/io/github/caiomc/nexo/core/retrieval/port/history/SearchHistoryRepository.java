package io.github.caiomc.nexo.core.retrieval.port.history;

import java.util.List;
import java.util.UUID;

import io.github.caiomc.nexo.core.retrieval.port.history.data.ToolSearchData;

public interface SearchHistoryRepository {

    /**
     * @return o id gerado
     */
    long save(ToolSearchData search);

    /**
     * As buscas do usuário, mais recentes primeiro.
     */
    List<ToolSearchData> findByUser(
            UUID userId,
            int limit
    );
}
