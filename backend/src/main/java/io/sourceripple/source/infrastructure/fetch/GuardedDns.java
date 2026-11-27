package io.sourceripple.source.infrastructure.fetch;

import io.sourceripple.source.domain.AddressPolicy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import okhttp3.Dns;

public class GuardedDns implements Dns {

    private final AddressPolicy policy;
    private final Dns delegate;

    public GuardedDns(AddressPolicy policy, Dns delegate) {
        this.policy = policy;
        this.delegate = delegate;
    }

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        List<InetAddress> allowed =
                delegate.lookup(hostname).stream().filter(policy::isAllowed).toList();
        if (allowed.isEmpty()) {
            throw new BlockedAddressException(hostname + " resolves only to disallowed addresses");
        }
        return allowed;
    }

    public static class BlockedAddressException extends UnknownHostException {

        public BlockedAddressException(String message) {
            super(message);
        }
    }
}
