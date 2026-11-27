package io.sourceripple.content.domain.rules;

import io.sourceripple.content.domain.SourceKind;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jsoup.select.Evaluator;
import org.jsoup.select.QueryParser;

/** Extraction rules compiled once and shared between every document that uses the same signature. */
public final class ExtractionRuleSet {

    private final Signature signature;
    private final Evaluator removed;
    private final List<Evaluator> mainContent;

    public ExtractionRuleSet(Signature signature) {
        this.signature = signature;
        String removedQuery = Stream.concat(
                        signature.removedTags().stream().sorted(),
                        signature.removedRoles().stream().sorted().map(role -> "[role=" + role + "]"))
                .collect(Collectors.joining(", "));
        this.removed = removedQuery.isEmpty() ? null : QueryParser.parse(removedQuery);
        this.mainContent = signature.mainContentSelectors().stream().map(QueryParser::parse).toList();
    }

    public Signature signature() {
        return signature;
    }

    public SourceKind kind() {
        return signature.kind();
    }

    public Optional<Evaluator> removedElements() {
        return Optional.ofNullable(removed);
    }

    public List<Evaluator> mainContent() {
        return mainContent;
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
