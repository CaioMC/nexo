package io.github.caiomc.nexo.adapters.catalog.web.mapper;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.adapters.catalog.web.response.CatalogOverviewResponse;
import io.github.caiomc.nexo.adapters.catalog.web.response.ContextSummaryResponse;
import io.github.caiomc.nexo.adapters.catalog.web.response.ExecutionPlanResponse;
import io.github.caiomc.nexo.adapters.catalog.web.response.PlanStepResponse;
import io.github.caiomc.nexo.adapters.catalog.web.response.ToolParameterResponse;
import io.github.caiomc.nexo.adapters.catalog.web.response.ToolResponse;
import io.github.caiomc.nexo.adapters.catalog.web.response.ToolSummaryResponse;
import io.github.caiomc.nexo.core.catalog.domain.model.CatalogOverview;
import io.github.caiomc.nexo.core.catalog.domain.model.ContextSummary;
import io.github.caiomc.nexo.core.catalog.domain.model.ExecutionPlan;
import io.github.caiomc.nexo.core.catalog.domain.model.PlanStep;
import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolDetail;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolParameter;
import io.github.caiomc.nexo.core.catalog.usecase.command.GetToolCommand;
import io.github.caiomc.nexo.core.catalog.usecase.command.ListToolsCommand;
import io.github.caiomc.nexo.core.catalog.usecase.command.PlanExecutionCommand;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

import static java.util.Objects.isNull;

/**
 * Parâmetros HTTP → {@code ...Command} e domínio → respostas. Validação de formato (nome
 * inválido, lista vazia) acontece aqui e vira 400.
 */
@Component
public class CatalogWebMapper {

    public GetToolCommand toGetCommand(String name) {
        return new GetToolCommand(ToolName.of(name));
    }

    public ListToolsCommand toListCommand(
            String context,
            String namespace
    ) {
        return new ListToolsCommand(
                this.blank(context) ? null : ContextKey.of(context),
                this.blank(namespace) ? null : namespace
        );
    }

    public PlanExecutionCommand toPlanCommand(String tools) {
        if (this.blank(tools)) {
            throw new IllegalArgumentException("Informe as ferramentas em ?tools=a,b");
        }

        return new PlanExecutionCommand(
                Arrays.stream(tools.split(","))
                        .map(String::strip)
                        .filter(name -> !name.isEmpty())
                        .map(ToolName::of)
                        .toList()
        );
    }

    public CatalogOverviewResponse toResponse(CatalogOverview overview) {
        return new CatalogOverviewResponse(
                overview.contexts(),
                overview.namespaces(),
                overview.tools(),
                overview.dependencies(),
                overview.byContext().stream().map(this::toResponse).toList()
        );
    }

    public ToolResponse toResponse(ToolDetail detail) {
        Tool tool = detail.tool();

        return new ToolResponse(
                tool.name().value(),
                tool.namespace(),
                tool.context().value(),
                tool.effect().name(),
                tool.description(),
                tool.parameters().stream().map(this::toResponse).toList(),
                ToolName.values(detail.dependsOn()),
                ToolName.values(detail.dependents()),
                detail.level()
        );
    }

    public List<ToolSummaryResponse> toSummaries(List<Tool> tools) {
        return tools.stream().map(this::toSummary).toList();
    }

    public ExecutionPlanResponse toResponse(ExecutionPlan plan) {
        return new ExecutionPlanResponse(
                ToolName.values(plan.requested()),
                plan.steps().stream().map(this::toResponse).toList()
        );
    }

    private ContextSummaryResponse toResponse(ContextSummary summary) {
        return new ContextSummaryResponse(summary.context().value(), summary.tools(), summary.namespaces());
    }

    private ToolSummaryResponse toSummary(Tool tool) {
        return new ToolSummaryResponse(
                tool.name().value(),
                tool.namespace(),
                tool.context().value(),
                tool.effect().name(),
                tool.description()
        );
    }

    private ToolParameterResponse toResponse(ToolParameter parameter) {
        return new ToolParameterResponse(
                parameter.ordinal(),
                parameter.name(),
                parameter.type().name().toLowerCase(Locale.ROOT),
                parameter.description(),
                parameter.required(),
                parameter.minimum(),
                parameter.maximum(),
                parameter.enumValues(),
                parameter.itemsSchema()
        );
    }

    private PlanStepResponse toResponse(PlanStep step) {
        return new PlanStepResponse(
                step.step(),
                step.tool().value(),
                step.level(),
                ToolName.values(step.prerequisites())
        );
    }

    private boolean blank(String value) {
        return isNull(value) || value.isBlank();
    }
}
