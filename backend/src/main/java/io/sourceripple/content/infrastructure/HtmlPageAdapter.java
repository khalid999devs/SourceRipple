package io.sourceripple.content.infrastructure;

import io.sourceripple.content.domain.NormalizedDocument;
import io.sourceripple.content.domain.NormalizedSection;
import io.sourceripple.content.domain.SourceAdapter;
import io.sourceripple.content.domain.SourceKind;
import io.sourceripple.content.domain.TextNormalizer;
import io.sourceripple.content.domain.rules.ExtractionRuleSet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class HtmlPageAdapter implements SourceAdapter {

    static final String INTRO_KEY = "_intro";

    private static final Set<String> TEXT_BLOCKS =
            Set.of("p", "li", "pre", "blockquote", "dt", "dd", "figcaption", "h5", "h6", "table", "caption");

    @Override
    public SourceKind kind() {
        return SourceKind.HTML;
    }

    @Override
    public NormalizedDocument adapt(byte[] body, Charset declaredCharset, String baseUrl, ExtractionRuleSet rules) {
        Document document = parse(body, declaredCharset, baseUrl);
        String title = TextNormalizer.normalize(document.title());
        clean(document, rules);
        Element main = mainContent(document, rules);
        List<NormalizedSection> sections = new SectionBuilder(rules.maxHeadingLevel()).build(main);
        if (title.isEmpty()) {
            title = sections.stream()
                    .map(NormalizedSection::title)
                    .filter(heading -> !heading.isEmpty())
                    .findFirst()
                    .orElse("");
        }
        return NormalizedDocument.of(SourceKind.HTML, title, sections);
    }

    private static Document parse(byte[] body, Charset declaredCharset, String baseUrl) {
        try {
            return Jsoup.parse(
                    new ByteArrayInputStream(body),
                    declaredCharset == null ? null : declaredCharset.name(),
                    baseUrl == null ? "" : baseUrl);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void clean(Document document, ExtractionRuleSet rules) {
        rules.removedTags().forEach(tag -> document.select(tag).remove());
        rules.removedRoles().forEach(role -> document.select("[role=" + role + "]").remove());
    }

    private static Element mainContent(Document document, ExtractionRuleSet rules) {
        for (String selector : rules.mainContentSelectors()) {
            Element candidate = document.selectFirst(selector);
            if (candidate != null) {
                return candidate;
            }
        }
        return document.body();
    }

    private static final class SectionBuilder {

        private final int maxHeadingLevel;
        private final List<NormalizedSection> sections = new ArrayList<>();
        private final Map<String, Integer> usedKeys = new HashMap<>();
        private final Deque<HeadingPart> path = new ArrayDeque<>();
        private String currentKey = INTRO_KEY;
        private String currentTitle = "";
        private final List<String> currentText = new ArrayList<>();

        SectionBuilder(int maxHeadingLevel) {
            this.maxHeadingLevel = maxHeadingLevel;
        }

        List<NormalizedSection> build(Element main) {
            for (Element element : main.getAllElements()) {
                if (element == main) {
                    continue;
                }
                int level = headingLevel(element);
                if (level > 0) {
                    startSection(element, level);
                } else if (element.tagName().equals("table") && !insideTextBlock(element, main)) {
                    tableRows(element).forEach(this::addText);
                } else if (isTextBlock(element) && !insideTextBlock(element, main)) {
                    addText(element.text());
                } else if (!insideTextBlock(element, main) && !element.ownText().isBlank()) {
                    addText(element.ownText());
                }
            }
            finishSection();
            return sections;
        }

        private int headingLevel(Element element) {
            String tag = element.tagName();
            if (tag.length() == 2 && tag.charAt(0) == 'h' && Character.isDigit(tag.charAt(1))) {
                int level = tag.charAt(1) - '0';
                return level <= maxHeadingLevel ? level : 0;
            }
            return 0;
        }

        private boolean isTextBlock(Element element) {
            return TEXT_BLOCKS.contains(element.tagName()) && headingLevel(element) == 0;
        }

        private boolean insideTextBlock(Element element, Element main) {
            for (Element parent = element.parent(); parent != null && parent != main; parent = parent.parent()) {
                if (isTextBlock(parent) || headingLevel(parent) > 0) {
                    return true;
                }
            }
            return false;
        }

        private void startSection(Element heading, int level) {
            finishSection();
            String title = TextNormalizer.normalize(heading.text());
            while (!path.isEmpty() && path.peekLast().level() >= level) {
                path.removeLast();
            }
            String slug = TextNormalizer.slugify(title);
            path.addLast(new HeadingPart(level, slug.isEmpty() ? "section" : slug));
            String anchor = anchorOf(heading);
            String base = anchor != null
                    ? anchor
                    : path.stream().map(HeadingPart::slug).collect(Collectors.joining("/"));
            currentKey = unique(base);
            currentTitle = title;
        }

        private static String anchorOf(Element heading) {
            if (!heading.id().isBlank()) {
                return heading.id().trim();
            }
            Element inner = heading.selectFirst("a[id], a[name]");
            if (inner != null) {
                return inner.hasAttr("id") ? inner.id().trim() : inner.attr("name").trim();
            }
            Element previous = heading.previousElementSibling();
            if (previous != null && previous.tagName().equals("a") && previous.text().isBlank()) {
                String name = previous.hasAttr("id") ? previous.id() : previous.attr("name");
                return name.isBlank() ? null : name.trim();
            }
            return null;
        }

        private String unique(String base) {
            int seen = usedKeys.merge(base, 1, Integer::sum);
            return seen == 1 ? base : base + "-" + seen;
        }

        private void addText(String raw) {
            String text = TextNormalizer.normalize(raw);
            if (!text.isEmpty()) {
                currentText.add(text);
            }
        }

        private void finishSection() {
            String text = String.join("\n", currentText);
            boolean emptyIntro = currentKey.equals(INTRO_KEY) && text.isEmpty();
            if (!emptyIntro) {
                sections.add(NormalizedSection.of(currentKey, currentTitle, text));
            }
            currentText.clear();
        }

        private static List<String> tableRows(Element table) {
            return table.select("tr").stream()
                    .map(row -> row.select("th, td").stream()
                            .map(cell -> TextNormalizer.normalize(cell.text()))
                            .collect(Collectors.joining(" | ")))
                    .filter(row -> !row.isBlank())
                    .toList();
        }

        private record HeadingPart(int level, String slug) {}
    }
}
