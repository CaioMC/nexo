package io.github.caiomc.nexo.adapters.catalog.web.response;

import java.util.List;

public record ExecutionPlanResponse(
        List<String> requested,
        List<PlanStepResponse> steps
) {
}
