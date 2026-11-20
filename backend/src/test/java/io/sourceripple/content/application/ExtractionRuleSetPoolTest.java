package io.sourceripple.content.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.sourceripple.content.domain.SourceKind;
import io.sourceripple.content.domain.rules.ExtractionRuleSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExtractionRuleSetPoolTest {

    private final ExtractionRuleSetPool pool = new ExtractionRuleSetPool();

    @Test
    void equalSignaturesShareOneInstance() {
        ExtractionRuleSet first = pool.get(signature(Set.of("nav", "footer")));
        ExtractionRuleSet second = pool.get(signature(Set.of("footer", "nav")));

        assertThat(second).isSameAs(first);
        assertThat(pool.defaultFor(SourceKind.HTML)).isSameAs(pool.defaultFor(SourceKind.HTML));
        assertThat(pool.size()).isEqualTo(2);
    }

    @Test
    void differentSignaturesGetDifferentInstances() {
        assertThat(pool.get(signature(Set.of("nav")))).isNotSameAs(pool.get(signature(Set.of("nav", "aside"))));
        assertThat(pool.defaultFor(SourceKind.RSS)).isNotSameAs(pool.defaultFor(SourceKind.HTML));
    }

    private static ExtractionRuleSet.Signature signature(Set<String> removedTags) {
        return new ExtractionRuleSet.Signature(SourceKind.HTML, removedTags, Set.of("banner"), List.of("main"), 4);
    }
}
