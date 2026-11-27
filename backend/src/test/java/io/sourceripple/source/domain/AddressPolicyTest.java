package io.sourceripple.source.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AddressPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "127.0.0.1", "10.1.2.3", "172.16.0.1", "172.31.255.255", "192.168.0.1", "169.254.169.254",
        "100.64.0.1", "100.127.255.255", "0.0.0.0", "224.0.0.1", "255.255.255.255", "192.0.2.10",
        "198.51.100.1", "203.0.113.5", "198.18.0.1", "::1", "::", "fe80::1", "fc00::1", "fd12:3456::1",
        "ff02::1", "2001:db8::1", "::ffff:127.0.0.1", "64:ff9b::a00:1", "2002:a00:1::1"
    })
    void forbidsPrivateReservedAndEmbeddedPrivateAddresses(String literal) throws UnknownHostException {
        assertThat(AddressPolicy.isForbidden(InetAddress.getByName(literal))).isTrue();
        assertThat(new AddressPolicy(false).isAllowed(InetAddress.getByName(literal))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"93.184.215.14", "8.8.8.8", "172.32.0.1", "100.128.0.1", "2606:4700::1111"})
    void allowsPublicAddresses(String literal) throws UnknownHostException {
        assertThat(AddressPolicy.isForbidden(InetAddress.getByName(literal))).isFalse();
    }

    @Test
    void theFlagAllowsEverything() throws UnknownHostException {
        assertThat(new AddressPolicy(true).isAllowed(InetAddress.getByName("169.254.169.254"))).isTrue();
    }
}
