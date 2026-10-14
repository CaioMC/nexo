package io.github.caiomc.nexo.adapters.catalog.web.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.caiomc.nexo.adapters.catalog.web.mapper.CatalogWebMapper;
import io.github.caiomc.nexo.adapters.catalog.web.response.CatalogOverviewResponse;
import io.github.caiomc.nexo.adapters.catalog.web.response.ExecutionPlanResponse;
import io.github.caiomc.nexo.adapters.catalog.web.response.ToolResponse;
import io.github.caiomc.nexo.adapters.catalog.web.response.ToolSummaryResponse;
import io.github.caiomc.nexo.core.catalog.usecase.DescribeCatalogUseCase;
import io.github.caiomc.nexo.core.catalog.usecase.GetToolUseCase;
import io.github.caiomc.nexo.core.catalog.usecase.ListToolsUseCase;
import io.github.caiomc.nexo.core.catalog.usecase.PlanExecutionUseCase;

/**
 * Catálogo para estudo e depuração (RF-37): visão geral, ferramentas e ordem de execução.
 */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final DescribeCatalogUseCase describe;
    private final ListToolsUseCase list;
    private final GetToolUseCase get;
    private final PlanExecutionUseCase plan;

    private final CatalogWebMapper mapper;

    public CatalogController(
            DescribeCatalogUseCase describe,
            ListToolsUseCase list,
            GetToolUseCase get,
            PlanExecutionUseCase plan,
            CatalogWebMapper mapper
    ) {
        this.describe = describe;
        this.list = list;
        this.get = get;
        this.plan = plan;
        this.mapper = mapper;
    }

    @GetMapping
    public CatalogOverviewResponse describe() {
        return this.mapper.toResponse(this.describe.describe());
    }

    @GetMapping("/tools")
    public List<ToolSummaryResponse> list(
            @RequestParam(required = false) String context,
            @RequestParam(required = false) String namespace
    ) {
        return this.mapper.toSummaries(
                this.list.list(
                        this.mapper.toListCommand(context, namespace)
                )
        );
    }

    @GetMapping("/tools/{name}")
    public ToolResponse get(@PathVariable String name) {
        return this.mapper.toResponse(
                this.get.get(
                        this.mapper.toGetCommand(name)
                )
        );
    }

    @GetMapping("/plan")
    public ExecutionPlanResponse plan(@RequestParam(required = false) String tools) {
        return this.mapper.toResponse(
                this.plan.planDAG(
                        this.mapper.toPlanCommand(tools)
                )
        );
    }
}
