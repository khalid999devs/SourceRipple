package io.sourceripple.shared.domain;

public final class TestFetchLimits {

    private TestFetchLimits() {}

    public static GlobalFetchLimits.Builder builder() {
        return GlobalFetchLimits.builder();
    }

    public static GlobalFetchLimits build(GlobalFetchLimits.Builder builder) {
        return GlobalFetchLimits.detached(builder);
    }

    public static GlobalFetchLimits defaults() {
        return GlobalFetchLimits.detached(GlobalFetchLimits.builder());
    }
}
