package io.sourceripple.content.infrastructure;

import io.sourceripple.content.domain.NormalizedDocument;
import io.sourceripple.content.domain.NormalizedSection;
import io.sourceripple.content.domain.SourceAdapter;
import io.sourceripple.content.domain.SourceKind;
import io.sourceripple.content.domain.TextNormalizer;
import io.sourceripple.content.domain.rules.ExtractionRuleSet;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Evaluator;
import org.jsoup.select.NodeFilter;
import org.jsoup.select.NodeTraversor;
import org.springframework.stereotype.Component;

@Component
public class HtmlPageAdapter implements SourceAdapter {

    static final String INTRO_KEY = "_intro";

    private static final Set<String> TEXT_BLOCKS = Set.of("p", "li", "pre", "blockquote", "dt", "dd", "figcaption");
    private static final String SCRIPT_ROOTS = "#root, #app, #__next, #__nuxt, #svelte, [data-reactroot], app-root";

    @Override
    public SourceKind kind() {
        return SourceKind.HTML;
    }

    @Override
    public NormalizedDocument adapt(byte[] body, Charset declaredCharset, String baseUrl, ExtractionRuleSet rules) {
        Document document = JsoupDocuments.html(body, declaredCharset, baseUrl);
        String title = TextNormalizer.normalize(document.title());
        boolean scriptMounted = isScriptMounted(document);

        rules.removedElements().ifPresent(removed -> document.select(removed).remove());
        Element main = mainContent(document, rules);
        List<NormalizedSection> sections = new SectionBuilder(main, rules.maxHeadingLevel()).build();

        if (title.isEmpty()) {
            title = sections.stream()
                    .map(NormalizedSection::title)
                    .filter(heading -> !heading.isEmpty())
                    .findFirst()
                    .orElse("");
        }
        return NormalizedDocument.of(SourceKind.HTML, title, sections, scriptMounted);
    }

    private static boolean isScriptMounted(Document document) {
        boolean hasScripts = !document.select("script").isEmpty();
        return hasScripts && document.select(SCRIPT_ROOTS).stream().anyMatch(root -> root.text().isBlank());
    }

    private static Element mainContent(Document document, ExtractionRuleSet rules) {
        for (Evaluator selector : rules.mainContent()) {
            Element candidate = document.selectFirst(selector);
            if (candidate != null) {
                return candidate;
            }
        }
        return document.body();
    }

    private static class SectionBuilder implements NodeFilter {

        private final Element main;
        private final int maxHeadingLevel;
        private final List<NormalizedSection> sections = new ArrayList<>();
        private final Map<String, Integer> usedKeys = new HashMap<>();
        private final Deque<HeadingPart> path = new ArrayDeque<>();
        private final List<String> currentText = new ArrayList<>();
        private final StringBuilder inlineText = new StringBuilder();

        private String currentKey = INTRO_KEY;
        private String currentTitle = "";

        SectionBuilder(Element main, int maxHeadingLevel) {
            this.main = main;
            this.maxHeadingLevel = maxHeadingLevel;
        }

        List<NormalizedSection> build() {
            NodeTraversor.filter(this, main);
            finishSection();
            return sections;
        }

        @Override
        public FilterResult head(Node node, int depth) {
            if (node instanceof TextNode text) {
                inlineText.append(text.getWholeText());
                return FilterResult.CONTINUE;
            }
            if (!(node instanceof Element element) || element == main) {
                return FilterResult.CONTINUE;
            }

            int level = headingLevel(element);
            if (level > 0) {
                startSection(element, level);
                return FilterResult.SKIP_ENTIRELY;
            }
            if (element.nameIs("table")) {
                flushInline();
                tableRows(element).forEach(this::addText);
                return FilterResult.SKIP_ENTIRELY;
            }
            if (TEXT_BLOCKS.contains(element.normalName())) {
                flushInline();
                addText(element.text());
                return FilterResult.SKIP_ENTIRELY;
            }
            if (element.nameIs("br")) {
                inlineText.append(' ');
            } else if (element.isBlock()) {
                flushInline();
            }
            return FilterResult.CONTINUE;
        }

        @Override
        public FilterResult tail(Node node, int depth) {
            if (node instanceof Element element && element.isBlock()) {
                flushInline();
            }
            return FilterResult.CONTINUE;
        }

        private int headingLevel(Element element) {
            String tag = element.normalName();
            if (tag.length() == 2 && tag.charAt(0) == 'h' && Character.isDigit(tag.charAt(1))) {
                int level = tag.charAt(1) - '0';
                return level <= maxHeadingLevel ? level : 0;
            }
            return 0;
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
            if (previous != null && previous.nameIs("a") && previous.text().isBlank()) {
                String name = previous.hasAttr("id") ? previous.id() : previous.attr("name");
                return name.isBlank() ? null : name.trim();
            }
            return null;
        }

        private String unique(String base) {
            int seen = usedKeys.merge(base, 1, Integer::sum);
            return seen == 1 ? base : base + "-" + seen;
        }

        private void flushInline() {
            addText(inlineText.toString());
            inlineText.setLength(0);
        }

        private void addText(String raw) {
            String text = TextNormalizer.normalize(raw);
            if (!text.isEmpty()) {
                currentText.add(text);
            }
        }

        private void finishSection() {
            flushInline();
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
