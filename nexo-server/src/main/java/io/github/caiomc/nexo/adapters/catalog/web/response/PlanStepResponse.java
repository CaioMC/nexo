package io.github.caiomc.nexo.adapters.catalog.web.response;

import java.util.List;

public record PlanStepResponse(
        int step,
        String tool,
        int level,
        List<String> prerequisites
) {
}
