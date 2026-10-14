package io.github.caiomc.nexo.core.retrieval.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.retrieval.domain.model.RankedTool;
import io.github.caiomc.nexo.core.retrieval.port.search.data.LexicalCandidateData;
import io.github.caiomc.nexo.core.retrieval.port.search.data.VectorCandidateData;

/**
 * Candidatos do banco → domínio. A ordem do banco é mantida (é ela que o RRF usa).
 */
@Component
public class CandidateDataMapper {

    public List<ToolName> lexicalNames(List<LexicalCandidateData> candidates) {
        return candidates.stream().map(candidate -> ToolName.of(candidate.toolName())).toList();
    }

    public List<ToolName> vectorNames(List<VectorCandidateData> candidates) {
        return candidates.stream().map(candidate -> ToolName.of(candidate.toolName())).toList();
    }

    public List<RankedTool> lexicalRanking(List<LexicalCandidateData> candidates) {
        return candidates.stream()
                .map(candidate -> new RankedTool(ToolName.of(candidate.toolName()), candidate.rank()))
                .toList();
    }

    /**
     * Distância de cosseno → similaridade (1 = mesmo sentido).
     */
    public List<RankedTool> vectorRanking(List<VectorCandidateData> candidates) {
        return candidates.stream()
                .map(candidate -> new RankedTool(ToolName.of(candidate.toolName()), 1 - candidate.distance()))
                .toList();
    }
}
