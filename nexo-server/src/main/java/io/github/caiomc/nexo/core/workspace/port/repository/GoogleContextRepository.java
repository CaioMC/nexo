package io.github.caiomc.nexo.core.workspace.port.repository;

import java.util.List;

import io.github.caiomc.nexo.core.workspace.port.repository.data.GoogleContextData;
import io.github.caiomc.nexo.core.workspace.port.repository.data.GoogleContextScopeData;

public interface GoogleContextRepository {

    List<GoogleContextData> contexts();

    List<GoogleContextScopeData> scopes();
}
