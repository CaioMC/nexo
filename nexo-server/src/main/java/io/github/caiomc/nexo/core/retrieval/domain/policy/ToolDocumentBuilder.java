package io.github.caiomc.nexo.core.retrieval.domain.policy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Collectors;

import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolParameter;
import io.github.caiomc.nexo.core.retrieval.domain.model.ToolDocument;

/**
 * O texto que representa a ferramenta no RAG semântico: nome legível, namespace, descrição e
 * nomes dos parâmetros. O {@code contentHash} (SHA-256) muda quando o texto muda, e o
 * indexador sabe o que precisa gerar de novo.
 */
public final class ToolDocumentBuilder {

    public ToolDocument buildRAG(Tool tool) {
        String parameters = tool.parameters()
                .stream()
                .map(ToolParameter::name)
                .collect(Collectors.joining(" "));
        String content = String.join(
                " ",
                tool.name().value().replace('_', ' '),
                tool.namespace().replace('_', ' '),
                tool.description(),
                parameters
        ).strip();

        return new ToolDocument(tool.name(), content, sha256(content));
    }

    private static String sha256(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }
}
