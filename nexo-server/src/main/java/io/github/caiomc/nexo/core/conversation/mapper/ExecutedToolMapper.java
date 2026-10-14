package io.github.caiomc.nexo.core.conversation.mapper;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.conversation.port.repository.data.ExecutedToolData;

/**
 * Ferramentas que já rodaram → o conjunto que a trava do DAG consulta.
 */
@Component
public class ExecutedToolMapper {

    public Set<ToolName> toNames(Collection<ExecutedToolData> executed) {
        return executed.stream().map(tool -> ToolName.of(tool.toolName())).collect(Collectors.toUnmodifiableSet());
    }
}
