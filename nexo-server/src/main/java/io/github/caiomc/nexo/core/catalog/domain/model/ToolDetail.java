package io.github.caiomc.nexo.core.catalog.domain.model;

import java.util.List;

/**
 * Uma ferramenta com a vizinhança dela no grafo: de quem depende, quem depende dela e o nível.
 */
public record ToolDetail(
        Tool tool,
        List<ToolName> dependsOn,
        List<ToolName> dependents,
        int level
) {

    public ToolDetail {
        dependsOn = List.copyOf(dependsOn);
        dependents = List.copyOf(dependents);
    }
}
