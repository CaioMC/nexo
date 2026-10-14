package io.github.caiomc.nexo.adapters.conversation.google.executor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Junta o texto de um documento do Docs ou de uma apresentação do Slides: os campos cujo nome
 * contém {@code content}, {@code text} ou {@code speakerNotes}, como o {@code collectText} do google-tools.
 */
final class DocumentText {

    static final int MAX = 40_000;

    private DocumentText() {
    }

    static String of(JsonNode document) {
        List<String> parts = new ArrayList<>();

        collect(document, "", parts);

        String text = String.join("\n", parts);

        return text.length() > MAX ? text.substring(0, MAX) : text;
    }

    private static void collect(
            JsonNode node,
            String key,
            List<String> parts
    ) {
        if (node.isTextual()) {
            String name = key.toLowerCase(Locale.ROOT);

            if ((name.contains("content") || name.contains("text") || name.contains("speakernotes")) && !node.asText().isBlank()) {
                parts.add(node.asText());
            }

            return;
        }

        if (node.isArray()) {
            node.forEach(item -> collect(item, key, parts));

            return;
        }

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();

            collect(field.getValue(), field.getKey(), parts);
        }
    }
}
