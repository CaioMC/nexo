package io.github.caiomc.nexo.core.catalog.application;

import java.util.List;

import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.catalog.domain.model.CatalogOverview;
import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolDetail;
import io.github.caiomc.nexo.core.catalog.usecase.DescribeCatalogUseCase;
import io.github.caiomc.nexo.core.catalog.usecase.GetToolUseCase;
import io.github.caiomc.nexo.core.catalog.usecase.ListToolsUseCase;
import io.github.caiomc.nexo.core.catalog.usecase.ProvideCatalogUseCase;
import io.github.caiomc.nexo.core.catalog.usecase.command.GetToolCommand;
import io.github.caiomc.nexo.core.catalog.usecase.command.ListToolsCommand;

@Service
public class CatalogQueryService implements DescribeCatalogUseCase, GetToolUseCase, ListToolsUseCase {

    private final ProvideCatalogUseCase catalog;

    public CatalogQueryService(ProvideCatalogUseCase catalog) {
        this.catalog = catalog;
    }

    @Override
    public CatalogOverview describe() {
        return this.catalog.current().overview();
    }

    @Override
    public ToolDetail get(GetToolCommand command) {
        return this.catalog.current().detailDAG(command.name());
    }

    @Override
    public List<Tool> list(ListToolsCommand command) {
        return this.catalog.current().filter(command.context(), command.namespace());
    }
}
