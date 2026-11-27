package io.sourceripple.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GlobalFetchLimitsTest {

    @Test
    void initializeReturnsTheSameInstanceForTheSameLimits() {
        GlobalFetchLimits first = GlobalFetchLimits.initialize(TestFetchLimits.builder());
        GlobalFetchLimits second = GlobalFetchLimits.initialize(TestFetchLimits.builder());

        assertThat(second).isSameAs(first).isSameAs(GlobalFetchLimits.getInstance());
    }

    @Test
    void initializeRefusesDifferentLimitsOnceBuilt() {
        GlobalFetchLimits.initialize(TestFetchLimits.builder());

        assertThatThrownBy(() -> GlobalFetchLimits.initialize(TestFetchLimits.builder().maxRedirects(9)))
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
