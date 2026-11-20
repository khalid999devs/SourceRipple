package io.sourceripple.source.infrastructure.fetch;

import io.sourceripple.content.application.UnsupportedContentException;
import io.sourceripple.shared.domain.GlobalFetchLimits;
import io.sourceripple.source.application.fetch.FetchPurpose;
import io.sourceripple.source.application.fetch.FetchRequest;
import io.sourceripple.source.application.fetch.FetchResult;
import io.sourceripple.source.application.fetch.WebFetcher;
import io.sourceripple.source.application.fetch.failure.SourceFetchBlockedException;
import io.sourceripple.source.application.fetch.failure.SourceFetchFailedException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import okhttp3.Dns;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

public class OkHttpWebFetcher implements WebFetcher {

    private static final String PAGE_ACCEPT = "text/html,application/xhtml+xml,application/rss+xml,"
            + "application/atom+xml,application/xml;q=0.9,text/xml;q=0.9";
    private static final long READ_CHUNK = 8 * 1024;

    private final OkHttpClient client;
    private final GlobalFetchLimits limits;
    private final String userAgent;

    public OkHttpWebFetcher(GlobalFetchLimits limits, Dns dns, Interceptor networkInterceptor, String userAgent) {
        this.limits = limits;
        this.userAgent = userAgent;
        // Redirects are followed by the proxy so every hop is validated again.
        this.client = new OkHttpClient.Builder()
                .dns(dns)
                .addNetworkInterceptor(networkInterceptor)
                .followRedirects(false)
                .followSslRedirects(false)
                .connectTimeout(limits.connectTimeout())
                .readTimeout(limits.readTimeout())
                .callTimeout(limits.totalTimeout())
                .build();
    }

    @Override
    public FetchResult fetch(FetchRequest request) {
        long started = System.nanoTime();
        Request.Builder builder = new Request.Builder()
                .url(request.url())
                .get()
                .header("User-Agent", userAgent)
                .header("Accept", request.purpose() == FetchPurpose.ROBOTS ? "text/plain" : PAGE_ACCEPT)
                // Asking for gzip ourselves turns off OkHttp's transparent decoding, so both caps can be enforced.
                .header("Accept-Encoding", "gzip");
        if (request.etag() != null) {
            builder.header("If-None-Match", request.etag());
        }
        if (request.lastModified() != null) {
            builder.header("If-Modified-Since", request.lastModified());
        }
        try (Response response = client.newCall(builder.build()).execute()) {
            return toResult(request, response, started);
        } catch (IOException e) {
            throw translate(e);
        }
    }

    private FetchResult toResult(FetchRequest request, Response response, long started) throws IOException {
        String contentType = response.header("Content-Type");
        byte[] body = new byte[0];
        long wireBytes = 0;
        if (response.isSuccessful()) {
            requireAllowedType(request.purpose(), contentType);
            Buffer wire = readCapped(response.body());
            wireBytes = wire.size();
            body = decode(wire, response.header("Content-Encoding"));
        }
        return new FetchResult(
                response.code(),
                request.url(),
                contentType,
                body,
                response.header("ETag"),
                response.header("Last-Modified"),
                response.header("Location"),
                response.header("Retry-After"),
                wireBytes,
                (System.nanoTime() - started) / 1_000_000);
    }

    private void requireAllowedType(FetchPurpose purpose, String contentType) {
        if (contentType == null) {
            return;
        }
        String mediaType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        boolean robotsText = purpose == FetchPurpose.ROBOTS && mediaType.equals("text/plain");
        if (!robotsText && !limits.allowsContentType(mediaType)) {
            throw new UnsupportedContentException("The content type " + mediaType + " is not supported.");
        }
    }

    private Buffer readCapped(ResponseBody body) throws IOException {
        long cap = limits.maxCompressedBytes();
        if (body.contentLength() > cap) {
            throw tooLarge(cap);
        }
        Buffer buffer = new Buffer();
        BufferedSource source = body.source();
        while (source.read(buffer, READ_CHUNK) != -1) {
            if (buffer.size() > cap) {
                throw tooLarge(cap);
            }
        }
        return buffer;
    }

    private byte[] decode(Buffer wire, String contentEncoding) throws IOException {
        long cap = limits.maxDecodedBytes();
        if (contentEncoding == null || contentEncoding.equalsIgnoreCase("identity")) {
            if (wire.size() > cap) {
                throw tooLarge(cap);
            }
            return wire.readByteArray();
        }
        if (!contentEncoding.trim().equalsIgnoreCase("gzip")) {
            throw SourceFetchFailedException.permanent("The response uses an unsupported encoding.");
        }
        try (InputStream gzip = new GZIPInputStream(new ByteArrayInputStream(wire.readByteArray()))) {
            ByteArrayOutputStream decoded = new ByteArrayOutputStream();
            byte[] chunk = new byte[(int) READ_CHUNK];
            int read;
            while ((read = gzip.read(chunk)) != -1) {
                decoded.write(chunk, 0, read);
                if (decoded.size() > cap) {
                    throw tooLarge(cap);
                }
            }
            return decoded.toByteArray();
        }
    }

    private static SourceFetchFailedException tooLarge(long cap) {
        return SourceFetchFailedException.permanent("The page is larger than " + cap / (1024 * 1024) + " MB.");
    }

    private static RuntimeException translate(IOException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof GuardedDns.BlockedAddressException) {
                return new SourceFetchBlockedException("This URL resolves to a disallowed destination.");
            }
            for (Throwable suppressed : cause.getSuppressed()) {
                if (suppressed instanceof GuardedDns.BlockedAddressException) {
                    return new SourceFetchBlockedException("This URL resolves to a disallowed destination.");
                }
            }
        }
        if (e instanceof InterruptedIOException) {
            return SourceFetchFailedException.retryable("The site did not answer in time.");
        }
        return SourceFetchFailedException.retryable("The site could not be reached.");
    }
}
