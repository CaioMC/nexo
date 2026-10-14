package io.github.caiomc.nexo.core.conversation.domain.policy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.caiomc.nexo.core.catalog.domain.model.ParameterType;
import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolEffect;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolParameter;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

import static org.assertj.core.api.Assertions.assertThat;

class ArgumentsValidatorTest {

    private final ArgumentsValidator validator = new ArgumentsValidator();

    private final Tool tool = new Tool(
            ToolName.of("google_calendar_respond_to_event"),
            "google_calendar_event",
            ContextKey.of("calendar"),
            ToolEffect.WRITE,
            "Responde a um convite.",
            List.of(
                    new ToolParameter(1, "eventId", ParameterType.STRING, "Id", true, null, null, List.of(), null),
                    new ToolParameter(2, "responseStatus", ParameterType.STRING, "Resposta", true, null, null, List.of("accepted", "declined"), null),
                    new ToolParameter(3, "maxResults", ParameterType.INTEGER, "Máximo", false, 1.0, 50.0, List.of(), null),
                    new ToolParameter(4, "attendees", ParameterType.ARRAY, "E-mails", false, null, null, List.of(), "{\"type\":\"string\"}")
            )
    );

    @Test
    void validArgumentsHaveNoErrors() {
        assertThat(this.validator.validate(this.tool, Map.of("eventId", "ev1", "responseStatus", "accepted", "maxResults", 10, "attendees", List.of("a@b.c")))).isEmpty();
    }

    @Test
    void nullCountsAsNotInformedButMissingRequiredIsAnError() {
        Map<String, Object> arguments = new HashMap<>();

        arguments.put("eventId", "ev1");
        arguments.put("maxResults", null);

        assertThat(this.validator.validate(this.tool, arguments)).containsExactly("responseStatus: obrigatório");
    }

    @Test
    void typesRangesEnumsAndUnknownNamesAreChecked() {
        List<String> errors = this.validator.validate(
                this.tool,
                Map.of(
                        "eventId", 42,
                        "responseStatus", "maybe",
                        "maxResults", 2.5,
                        "attendees", "a@b.c",
                        "extra", true
                )
        );

        assertThat(errors).containsExactly(
                "eventId: esperava texto",
                "responseStatus: use um de [accepted, declined]",
                "maxResults: esperava inteiro",
                "attendees: esperava lista",
                "extra: parâmetro desconhecido"
        );
        assertThat(this.validator.validate(this.tool, Map.of("eventId", "e", "responseStatus", "accepted", "maxResults", 51))).containsExactly("maxResults: máximo 50");
    }
}
