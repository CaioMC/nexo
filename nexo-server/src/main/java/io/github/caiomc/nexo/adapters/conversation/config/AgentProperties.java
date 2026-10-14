package io.github.caiomc.nexo.adapters.conversation.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.github.caiomc.nexo.core.conversation.domain.enums.ToolExposure;

/**
 * {@code nexo.agent.*} e {@code nexo.google.*}.
 *
 * @param googleApiBase vazio = APIs reais do Google; em teste, um servidor local
 */
@ConfigurationProperties(prefix = "nexo.agent")
public record AgentProperties(
        int maxModelCalls,
        int maxToolCalls,
        Duration turnTimeout,
        Duration approvalTtl,
        Duration prerequisiteWindow,
        int historyTurns,
        int toolMemoryTurns,
        Duration deltaWindow,
        ToolExposure exposure,
        String defaultTimeZone,
        String googleApiBase
) {
}
