package io.github.caiomc.nexo.core.retrieval.domain.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LexicalTermsBuilderTest {

    private final LexicalTermsBuilder builder = new LexicalTermsBuilder();

    @Test
    void joinsWordsWithOrWithoutAccents() {
        assertThat(this.builder.buildRAG("Marque reunião amanhã às 10h!")).contains("marque | reuniao | amanha | as | 10h");
    }

    @Test
    void neverLetsTsquerySyntaxThrough() {
        assertThat(this.builder.buildRAG("ab & cd | !ef :* 'gh'")).contains("ab | cd | ef | gh");
        assertThat(this.builder.buildRAG("planilha & (orçamento)")).contains("planilha | orcamento");
    }

    @Test
    void emptyWhenNothingUsefulIsLeft() {
        assertThat(this.builder.buildRAG("  ?! ")).isEmpty();
    }
}
