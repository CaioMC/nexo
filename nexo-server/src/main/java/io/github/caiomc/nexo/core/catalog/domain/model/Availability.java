package io.github.caiomc.nexo.core.catalog.domain.model;

import java.util.List;

/**
 * Resultado de {@link ToolCatalog#availability}: as disponíveis e as bloqueadas, cada lista na
 * ordem em que as ferramentas foram pedidas.
 */
public record Availability(
        List<ToolName> available,
        List<BlockedTool> blocked
) {

    public Availability {
        available = List.copyOf(available);
        blocked = List.copyOf(blocked);
    }
}
