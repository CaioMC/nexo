package io.github.caiomc.nexo.core.conversation.domain.policy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Objects.isNull;

/**
 * RN-05: SHA-256 dos argumentos em forma canônica (chaves ordenadas, sem espaços). A aprovação vale
 * para exatamente esses argumentos.
 */
public class ArgumentsHasher {

    public String hash(Map<String, Object> arguments) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(this.canonical(arguments).getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }

    public String canonical(Object value) {
        StringBuilder out = new StringBuilder();

        this.write(value, out);

        return out.toString();
    }

    private void write(
            Object value,
            StringBuilder out
    ) {
        if (isNull(value)) {
            out.append("null");
        } else if (value instanceof Map<?, ?> map) {
            this.writeObject(map, out);
        } else if (value instanceof List<?> list) {
            this.writeArray(list, out);
        } else if (value instanceof String text) {
            this.writeString(text, out);
        } else if (value instanceof Number number) {
            out.append(this.number(number));
        } else {
            out.append(value);
        }
    }

    private void writeObject(
            Map<?, ?> map,
            StringBuilder out
    ) {
        TreeMap<String, Object> sorted = new TreeMap<>();

        map.forEach((key, item) -> sorted.put(String.valueOf(key), item));
        out.append('{');

        boolean first = true;

        for (Map.Entry<String, Object> entry : sorted.entrySet()) {
            if (!first) {
                out.append(',');
            }

            this.writeString(entry.getKey(), out);
            out.append(':');
            this.write(entry.getValue(), out);
            first = false;
        }

        out.append('}');
    }

    private void writeArray(
            List<?> list,
            StringBuilder out
    ) {
        out.append('[');

        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                out.append(',');
            }

            this.write(list.get(i), out);
        }

        out.append(']');
    }

    private void writeString(
            String text,
            StringBuilder out
    ) {
        out.append('"');

        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }

        out.append('"');
    }

    /**
     * 3 e 3.0 viram o mesmo texto: o JSON não distingue e o hash também não pode.
     */
    private String number(Number number) {
        double value = number.doubleValue();

        return value == Math.rint(value) && !Double.isInfinite(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
