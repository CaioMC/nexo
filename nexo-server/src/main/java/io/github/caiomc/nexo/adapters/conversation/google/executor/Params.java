package io.github.caiomc.nexo.adapters.conversation.google.executor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mapas de query e de corpo que aceitam valor nulo (o cliente HTTP pula os nulos).
 */
final class Params {

    private Params() {
    }

    static Map<String, Object> of(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();

        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }

        return map;
    }

    /**
     * Segmento de caminho (ids que o modelo mandou não podem mudar a rota).
     */
    static String path(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
