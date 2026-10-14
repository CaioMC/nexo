package io.github.caiomc.nexo.core.retrieval.domain.policy;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Transforma o pedido do usuário nos termos da consulta full-text: palavras unidas por OU
 * ({@code marque | reuniao | amanha}). Com E, uma palavra a mais no pedido que não está na
 * ferramenta zeraria o resultado. Só sobram letras e dígitos, então o texto do usuário nunca
 * vira sintaxe de {@code tsquery}. Radical e palavras vazias ficam por conta do {@code pt_unaccent}.
 */
public final class LexicalTermsBuilder {

    private static final Pattern SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final int MIN_LENGTH = 2;

    public Optional<String> buildRAG(String query) {
        String normalized = MARKS.matcher(Normalizer.normalize(query, Normalizer.Form.NFD)).replaceAll("");
        String terms = Arrays.stream(SEPARATOR.split(normalized.toLowerCase(Locale.ROOT)))
                .filter(word -> word.length() >= MIN_LENGTH)
                .distinct()
                .collect(Collectors.joining(" | "));

        return terms.isEmpty() ? Optional.empty() : Optional.of(terms);
    }
}
