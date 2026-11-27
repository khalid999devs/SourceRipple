package io.sourceripple.content.infrastructure;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

final class JsoupDocuments {

    private JsoupDocuments() {}

    static Document html(byte[] body, Charset declaredCharset, String baseUrl) {
        return parse(body, declaredCharset, baseUrl, Parser.htmlParser());
    }

    static Document xml(byte[] body, Charset declaredCharset, String baseUrl) {
        return parse(body, declaredCharset, baseUrl, Parser.xmlParser());
    }

    private static Document parse(byte[] body, Charset declaredCharset, String baseUrl, Parser parser) {
        try {
            return Jsoup.parse(
                    new ByteArrayInputStream(body),
                    declaredCharset == null ? null : declaredCharset.name(),
                    baseUrl == null ? "" : baseUrl,
                    parser);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
