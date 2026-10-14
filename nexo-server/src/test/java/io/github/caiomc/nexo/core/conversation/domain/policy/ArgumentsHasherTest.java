package io.github.caiomc.nexo.core.conversation.domain.policy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArgumentsHasherTest {

    private final ArgumentsHasher hasher = new ArgumentsHasher();

    @Test
    void keyOrderAndIntegralNumbersDoNotChangeTheHash() {
        Map<String, Object> first = new LinkedHashMap<>();
        Map<String, Object> second = new LinkedHashMap<>();

        first.put("summary", "Reunião \"importante\"");
        first.put("count", 3);
        second.put("count", 3.0);
        second.put("summary", "Reunião \"importante\"");

        assertThat(this.hasher.hash(first)).isEqualTo(this.hasher.hash(second)).hasSize(64);
        assertThat(this.hasher.canonical(first)).isEqualTo("{\"count\":3,\"summary\":\"Reunião \\\"importante\\\"\"}");
    }

    @Test
    void anyChangeInTheArgumentsChangesTheHash() {
        String approved = this.hasher.hash(Map.of("fileId", "abc", "parents", List.of("x")));

        assertThat(this.hasher.hash(Map.of("fileId", "abd", "parents", List.of("x")))).isNotEqualTo(approved);
        assertThat(this.hasher.hash(Map.of("fileId", "abc", "parents", List.of("y")))).isNotEqualTo(approved);
    }
}
