package io.github.caiomc.nexo.core.conversation.port.approval.command;

import java.util.Map;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;

public record DescribeApprovalCommand(
        ToolName tool,
        Map<String, Object> arguments
) {
}
