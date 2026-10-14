package io.github.caiomc.nexo.core.identity.domain.model;

import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.util.Objects.isNull;

/**
 * Escopos OAuth que o usuário já concedeu ao app (a soma de todos os consentimentos, graças ao
 * {@code include_granted_scopes=true}).
 */
public record GrantedScopes(Set<String> values) {

    public GrantedScopes {
        values = isNull(values) ? Set.of() : Set.copyOf(values);
    }

    public static GrantedScopes of(Collection<String> values) {
        return new GrantedScopes(isNull(values) ? Set.of() : Set.copyOf(values));
    }

    public static GrantedScopes none() {
        return new GrantedScopes(Set.of());
    }

    public boolean containsAll(Collection<String> scopes) {
        return this.values.containsAll(scopes);
    }

    public SortedSet<String> sorted() {
        return new TreeSet<>(this.values);
    }
}
