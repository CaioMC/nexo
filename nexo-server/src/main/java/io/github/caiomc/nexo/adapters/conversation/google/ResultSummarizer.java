package io.github.caiomc.nexo.adapters.conversation.google;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolEffect;

/**
 * Frase curta sobre o resultado, para o cartão da ferramenta e para o histórico dos próximos turnos
 * (o histórico leva o resumo, não a saída completa).
 */
public class ResultSummarizer {

    private static final List<Map.Entry<String, String>> COLLECTIONS = List.of(
            Map.entry("messages", "mensagens"),
            Map.entry("files", "arquivos"),
            Map.entry("items", "itens"),
            Map.entry("drafts", "rascunhos"),
            Map.entry("labels", "etiquetas"),
            Map.entry("permissions", "permissões"),
            Map.entry("spaces", "espaços"),
            Map.entry("memberships", "membros"),
            Map.entry("results", "contatos"),
            Map.entry("values", "linhas"),
            Map.entry("sheets", "abas")
    );

    public String summarize(
            ToolEffect effect,
            JsonNode result
    ) {
        for (Map.Entry<String, String> collection : COLLECTIONS) {
            JsonNode values = result.path(collection.getKey());

            if (values.isArray()) {
                return values.size() + " " + collection.getValue();
            }
        }

        if (result.has("text")) {
            return result.path("text").asText().length() + " caracteres de texto";
        }

        if (effect == ToolEffect.READ) {
            return this.title(result, "Lido");
        }

        return effect == ToolEffect.DESTRUCTIVE ? "Feito" : this.title(result, "Alteração feita");
    }

    private String title(
            JsonNode result,
            String fallback
    ) {
        for (String field : List.of("summary", "name", "title", "displayName")) {
            if (result.path(field).isTextual()) {
                return fallback + ": " + result.path(field).asText();
            }
        }

        return fallback;
    }
}
