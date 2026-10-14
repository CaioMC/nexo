package io.github.caiomc.nexo.adapters.catalog.web.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ToolParameterResponse(
        int ordinal,
        String name,
        String type,
        String description,
        boolean required,
        Double minimum,
        Double maximum,
        List<String> enumValues,
        @JsonRawValue String items
) {
}
