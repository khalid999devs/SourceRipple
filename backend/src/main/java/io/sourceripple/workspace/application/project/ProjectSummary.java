package io.sourceripple.workspace.application.project;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

public class ProjectSummary {

    private long watchCount;
    private long failingSources;
    private Instant lastCheckAt;
    private final Map<String, Long> impactsByStatus = new TreeMap<>();

    public void watchCount(long count) {
        watchCount = count;
    }

    public void failingSources(long count) {
        failingSources = count;
    }

    public void lastCheckAt(Instant at) {
        lastCheckAt = at;
    }

    public void impactsByStatus(Map<String, Long> counts) {
        impactsByStatus.putAll(counts);
    }

    public long getWatchCount() {
        return watchCount;
    }

    public long getFailingSources() {
        return failingSources;
    }

    public Instant getLastCheckAt() {
        return lastCheckAt;
    }

    public Map<String, Long> getImpactsByStatus() {
        return Map.copyOf(impactsByStatus);
    }
}
