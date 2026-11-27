package io.sourceripple.content.domain;

import io.sourceripple.content.domain.rules.ExtractionRuleSet;
import java.nio.charset.Charset;

public interface SourceAdapter {

    SourceKind kind();

    /** Turns a fetched document into sections; {@code declaredCharset} is null when the server sent none. */
    NormalizedDocument adapt(byte[] body, Charset declaredCharset, String baseUrl, ExtractionRuleSet rules);
}
