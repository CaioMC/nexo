package io.github.caiomc.nexo.core.workspace.domain.model;

import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

import static java.util.Objects.isNull;

/**
 * Um produto Google que o usuário pode liberar (Calendar, Docs...). Por baixo, um conjunto de
 * escopos OAuth.
 */
public record GoogleContext(
        ContextKey key,
        String name,
        String description,
        String googleApi,
        int position,
        Set<String> scopes
) {

    public GoogleContext {
        if (isNull(key) || isNull(name) || name.isBlank()) {
            throw new IllegalArgumentException("Contexto precisa de chave e nome");
        }

        if (isNull(scopes) || scopes.isEmpty()) {
            throw new IllegalArgumentException("Contexto " + key + " sem escopos");
        }

        scopes = Set.copyOf(scopes);
        description = isNull(description) ? "" : description;
    }

    /**
     * RN-01: só pode ficar ligado se todos os escopos dele já foram concedidos.
     */
    public boolean grantedBy(Collection<String> granted) {
        return granted.containsAll(this.scopes);
    }

    public SortedSet<String> sortedScopes() {
        return new TreeSet<>(this.scopes);
    }
}
