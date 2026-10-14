package io.github.caiomc.nexo.core.retrieval.domain.policy;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.caiomc.nexo.core.retrieval.domain.model.RankedTool;

import static io.github.caiomc.nexo.core.support.CatalogFixtures.name;
import static io.github.caiomc.nexo.core.support.CatalogFixtures.names;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ReciprocalRankFusionTest {

    private final ReciprocalRankFusion fusion = new ReciprocalRankFusion(60);

    @Test
    void sumsOneOverKPlusPositionFromEachList() {
        List<RankedTool> fused = this.fusion.fuseRAG(names("a_tool", "b_tool"), names("b_tool", "c_tool"));

        assertThat(fused).extracting(RankedTool::name).containsExactly(name("b_tool"), name("a_tool"), name("c_tool"));
        assertThat(fused.getFirst().score()).isCloseTo(1.0 / 62 + 1.0 / 61, within(1e-12));
        assertThat(fused.get(1).score()).isCloseTo(1.0 / 61, within(1e-12));
    }

    @Test
    void tiesAreBrokenByName() {
        assertThat(this.fusion.fuseRAG(names("z_tool"), names("a_tool")))
                .extracting(RankedTool::name)
                .containsExactly(name("a_tool"), name("z_tool"));
    }

    @Test
    void emptyListsGiveEmptyRanking() {
        assertThat(this.fusion.fuseRAG(List.of(), List.of())).isEmpty();
        assertThat(this.fusion.fuseRAG(names("a_tool"), List.of())).extracting(RankedTool::name).containsExactly(name("a_tool"));
    }

    @Test
    void kMustBePositive() {
        assertThatThrownBy(() -> new ReciprocalRankFusion(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
