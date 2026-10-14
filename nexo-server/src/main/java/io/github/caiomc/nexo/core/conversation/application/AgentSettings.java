package io.github.caiomc.nexo.core.conversation.application;

import java.time.Duration;
import java.time.ZoneId;

import io.github.caiomc.nexo.core.conversation.domain.enums.ToolExposure;

import static java.util.Objects.isNull;

/**
 * Configuração do agente ({@code nexo.agent.*}).
 *
 * @param maxModelCalls      RN-11: chamadas ao modelo por turno
 * @param maxToolCalls       RN-11: chamadas de ferramenta por turno
 * @param turnTimeout        RN-11: tempo rodando por turno (esperar aprovação não conta)
 * @param approvalTtl        RN-06: validade da aprovação
 * @param prerequisiteWindow RN-04: janela em que um pré-requisito conta como executado
 * @param historyTurns       turnos anteriores enviados ao modelo
 * @param toolMemoryTurns    turnos cujas ferramentas usadas com sucesso já vão carregadas
 * @param deltaWindow        lote de {@code assistant.delta}
 * @param defaultTimeZone    fuso quando o navegador não informa
 */
public record AgentSettings(
        int maxModelCalls,
        int maxToolCalls,
        Duration turnTimeout,
        Duration approvalTtl,
        Duration prerequisiteWindow,
        int historyTurns,
        int toolMemoryTurns,
        Duration deltaWindow,
        ToolExposure exposure,
        ZoneId defaultTimeZone
) {

    public AgentSettings {
        if (maxModelCalls < 1 || maxToolCalls < 1) {
            throw new IllegalArgumentException("Os limites do turno precisam ser positivos (nexo.agent.max-*)");
        }

        if (isNull(turnTimeout) || isNull(approvalTtl) || isNull(prerequisiteWindow) || isNull(deltaWindow)) {
            throw new IllegalArgumentException("Informe os tempos do agente (nexo.agent.*)");
        }

        exposure = isNull(exposure) ? ToolExposure.CLIENT_TOOL_SEARCH : exposure;
        defaultTimeZone = isNull(defaultTimeZone) ? ZoneId.of("America/Sao_Paulo") : defaultTimeZone;
    }
}
