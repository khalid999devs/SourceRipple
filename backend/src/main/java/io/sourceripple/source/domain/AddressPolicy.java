package io.sourceripple.source.domain;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class AddressPolicy {

    private static final List<Range> FORBIDDEN = List.of(
            Range.of("0.0.0.0", 8),
            Range.of("10.0.0.0", 8),
            Range.of("100.64.0.0", 10),
            Range.of("127.0.0.0", 8),
            Range.of("169.254.0.0", 16),
            Range.of("172.16.0.0", 12),
            Range.of("192.0.0.0", 24),
            Range.of("192.0.2.0", 24),
            Range.of("192.168.0.0", 16),
            Range.of("198.18.0.0", 15),
            Range.of("198.51.100.0", 24),
            Range.of("203.0.113.0", 24),
            Range.of("224.0.0.0", 4),
            Range.of("240.0.0.0", 4),
            Range.of("::", 128),
            Range.of("::1", 128),
            Range.of("fc00::", 7),
            Range.of("fe80::", 10),
            Range.of("fec0::", 10),
            Range.of("ff00::", 8),
            Range.of("2001:db8::", 32));

    private final boolean allowPrivateTargets;

    public AddressPolicy(boolean allowPrivateTargets) {
        this.allowPrivateTargets = allowPrivateTargets;
    }

    public boolean isAllowed(InetAddress address) {
        return allowPrivateTargets || !isForbidden(address);
    }

    public static boolean isForbidden(InetAddress address) {
        InetAddress effective = embeddedIpv4(address);
        byte[] bytes = effective.getAddress();
        return FORBIDDEN.stream().anyMatch(range -> range.contains(bytes));
    }

    // Mapped (::ffff:0:0/96), NAT64 (64:ff9b::/96) and 6to4 (2002::/16) addresses reach an IPv4 host.
    private static InetAddress embeddedIpv4(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return address;
        }

        byte[] b = address.getAddress();
        boolean mapped = isZero(b, 0, 10) && (b[10] & 0xff) == 0xff && (b[11] & 0xff) == 0xff;
        boolean nat64 = (b[0] & 0xff) == 0x00 && (b[1] & 0xff) == 0x64 && (b[2] & 0xff) == 0xff
                && (b[3] & 0xff) == 0x9b && isZero(b, 4, 12);
        if (mapped || nat64) {
            return ipv4(b[12], b[13], b[14], b[15]);
        }

        if ((b[0] & 0xff) == 0x20 && (b[1] & 0xff) == 0x02) {
            return ipv4(b[2], b[3], b[4], b[5]);
        }
        return address;
    }

    private static boolean isZero(byte[] bytes, int from, int to) {
        for (int i = from; i < to; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return true;
    }

    private static InetAddress ipv4(byte a, byte b, byte c, byte d) {
        try {
            return Inet4Address.getByAddress(new byte[] {a, b, c, d});
        } catch (UnknownHostException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private record Range(byte[] network, int prefixLength) {

        static Range of(String literal, int prefixLength) {
            try {
                return new Range(InetAddress.getByName(literal).getAddress(), prefixLength);
            } catch (UnknownHostException impossible) {
                throw new IllegalStateException(impossible);
            }
        }

        boolean contains(byte[] address) {
            if (address.length != network.length) {
                return false;
            }
            int fullBytes = prefixLength / 8;
            for (int i = 0; i < fullBytes; i++) {
                if (address[i] != network[i]) {
                    return false;
                }
            }
            int remainingBits = prefixLength % 8;
            if (remainingBits == 0) {
                return true;
            }
            int mask = (0xff << (8 - remainingBits)) & 0xff;
            return (address[fullBytes] & mask) == (network[fullBytes] & mask);
        }
    }
}
