package io.sourceripple.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GlobalFetchLimitsTest {

    @Test
    void initializeReturnsTheSameInstanceForTheSameLimits() {
        GlobalFetchLimits first = GlobalFetchLimits.initialize(GlobalFetchLimits.builder());
        GlobalFetchLimits second = GlobalFetchLimits.initialize(GlobalFetchLimits.builder());

        assertThat(second).isSameAs(first).isSameAs(GlobalFetchLimits.getInstance());
    }

    @Test
    void initializeRefusesDifferentLimitsOnceBuilt() {
        GlobalFetchLimits.initialize(GlobalFetchLimits.builder());

        assertThatThrownBy(() -> GlobalFetchLimits.initialize(GlobalFetchLimits.builder().maxRedirects(9)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void contentTypesMatchCaseInsensitively() {
        GlobalFetchLimits limits = TestFetchLimits.defaults();

        assertThat(limits.allowsContentType("Text/HTML")).isTrue();
        assertThat(limits.allowsContentType("image/png")).isFalse();
        assertThat(limits.allowsContentType(null)).isFalse();
    }
}
