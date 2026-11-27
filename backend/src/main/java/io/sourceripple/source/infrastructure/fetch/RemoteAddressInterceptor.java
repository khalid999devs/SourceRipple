package io.sourceripple.source.infrastructure.fetch;

import io.sourceripple.source.domain.AddressPolicy;
import java.io.IOException;
import java.net.InetAddress;
import okhttp3.Connection;
import okhttp3.Interceptor;
import okhttp3.Response;

/** Re-checks the connected peer before any request bytes are written, in case DNS was bypassed. */
public class RemoteAddressInterceptor implements Interceptor {

    private final AddressPolicy policy;

    public RemoteAddressInterceptor(AddressPolicy policy) {
        this.policy = policy;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Connection connection = chain.connection();
        InetAddress peer = connection == null ? null : connection.socket().getInetAddress();
        if (peer == null || !policy.isAllowed(peer)) {
            throw new GuardedDns.BlockedAddressException("Connected to a disallowed address");
        }
        return chain.proceed(chain.request());
    }
}
