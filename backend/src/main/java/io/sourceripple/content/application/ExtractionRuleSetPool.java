package io.sourceripple.content.application;

import io.sourceripple.content.domain.SourceKind;
import io.sourceripple.content.domain.rules.ExtractionRuleSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ExtractionRuleSetPool {

    public static final ExtractionRuleSet.Signature DEFAULT_HTML = new ExtractionRuleSet.Signature(
            SourceKind.HTML,
            Set.of("script", "style", "noscript", "template", "iframe", "form", "nav", "header", "footer", "aside"),
            Set.of("navigation", "banner", "contentinfo"),
            List.of("main", "article", "[role=main]"),
            4);

    public static final ExtractionRuleSet.Signature DEFAULT_RSS =
            new ExtractionRuleSet.Signature(SourceKind.RSS, Set.of(), Set.of(), List.of(), 0);

    private final Map<ExtractionRuleSet.Signature, ExtractionRuleSet> pool = new ConcurrentHashMap<>();

    public ExtractionRuleSet get(ExtractionRuleSet.Signature signature) {
        return pool.computeIfAbsent(signature, ExtractionRuleSet::new);
    }

    public ExtractionRuleSet defaultFor(SourceKind kind) {
        return get(kind == SourceKind.RSS ? DEFAULT_RSS : DEFAULT_HTML);
    }

    int size() {
        return pool.size();
    }
}
