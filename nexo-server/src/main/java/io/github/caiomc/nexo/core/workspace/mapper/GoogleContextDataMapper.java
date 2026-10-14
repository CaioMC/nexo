package io.github.caiomc.nexo.core.workspace.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.workspace.domain.model.GoogleContext;
import io.github.caiomc.nexo.core.workspace.port.repository.data.GoogleContextData;
import io.github.caiomc.nexo.core.workspace.port.repository.data.GoogleContextScopeData;

/**
 * Linhas de {@code google_context} + {@code google_context_scope} → {@link GoogleContext}, na
 * ordem de exibição.
 */
@Component
public class GoogleContextDataMapper {

    public List<GoogleContext> toContexts(
            List<GoogleContextData> contexts,
            List<GoogleContextScopeData> scopes
    ) {
        Map<String, Set<String>> byContext = scopes.stream()
                .collect(Collectors.groupingBy(
                        GoogleContextScopeData::contextKey,
                        Collectors.mapping(GoogleContextScopeData::scope, Collectors.toSet())
                ));

        return contexts.stream()
                .sorted(Comparator.comparingInt(GoogleContextData::position))
                .map(context -> this.toContext(context, byContext.getOrDefault(context.key(), Set.of())))
                .toList();
    }

    private GoogleContext toContext(
            GoogleContextData data,
            Set<String> scopes
    ) {
        return new GoogleContext(
                ContextKey.of(data.key()),
                data.name(),
                data.description(),
                data.googleApi(),
                data.position(),
                scopes
        );
    }
}
