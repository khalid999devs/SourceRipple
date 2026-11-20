package io.sourceripple.content.domain.rules;

import io.sourceripple.content.domain.SourceKind;
import java.util.List;
import java.util.Set;

/** Immutable extraction rules, shared between every document that uses the same signature. */
public final class ExtractionRuleSet {

    private final Signature signature;

    public ExtractionRuleSet(Signature signature) {
        this.signature = signature;
    }

    public Signature signature() {
        return signature;
    }

    public SourceKind kind() {
        return signature.kind();
    }

    public Set<String> removedTags() {
        return signature.removedTags();
    }

    public Set<String> removedRoles() {
        return signature.removedRoles();
    }

    public List<String> mainContentSelectors() {
        return signature.mainContentSelectors();
    }

    public int maxHeadingLevel() {
        return signature.maxHeadingLevel();
    }

    public record Signature(
            SourceKind kind,
            Set<String> removedTags,
            Set<String> removedRoles,
            List<String> mainContentSelectors,
            int maxHeadingLevel) {

        public Signature {
            removedTags = Set.copyOf(removedTags);
            removedRoles = Set.copyOf(removedRoles);
            mainContentSelectors = List.copyOf(mainContentSelectors);
        }
    }
}
