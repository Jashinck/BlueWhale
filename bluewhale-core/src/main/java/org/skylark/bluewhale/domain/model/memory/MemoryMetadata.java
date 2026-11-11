package org.skylark.bluewhale.domain.model.memory;

import java.time.Instant;
import java.util.List;

public class MemoryMetadata {
    private final String agentId;
    private final String sessionId;
    private final String source;
    private final List<String> tags;
    private final Instant createdAt;
    private final Instant lastAccessedAt;
    private final int accessCount;
    /** 重要性分数 [0.0, 1.0] */
    private final double importanceScore;
    /** 衰减因子：记忆随时间遗忘的速率 */
    private final double decayFactor;

    public MemoryMetadata(String agentId, String sessionId, String source, List<String> tags,
                          Instant createdAt, Instant lastAccessedAt, int accessCount,
                          double importanceScore, double decayFactor) {
        this.agentId = agentId;
        this.sessionId = sessionId;
        this.source = source;
        this.tags = tags;
        this.createdAt = createdAt;
        this.lastAccessedAt = lastAccessedAt;
        this.accessCount = accessCount;
        this.importanceScore = importanceScore;
        this.decayFactor = decayFactor;
    }

    public static Builder builder() {
        return new Builder();
    }

    public MemoryMetadata withAccess() {
        return new MemoryMetadata(agentId, sessionId, source, tags, createdAt,
                Instant.now(), accessCount + 1, importanceScore, decayFactor);
    }

    public MemoryMetadata withImportance(double newImportance) {
        return new MemoryMetadata(agentId, sessionId, source, tags, createdAt,
                lastAccessedAt, accessCount,
                Math.max(0.0, Math.min(1.0, newImportance)), decayFactor);
    }

    public double computeEffectiveImportance() {
        long secondsSinceAccess = Instant.now().getEpochSecond() -
                (lastAccessedAt != null ? lastAccessedAt.getEpochSecond() : createdAt.getEpochSecond());
        double decayedScore = importanceScore * Math.exp(-decayFactor * secondsSinceAccess / 86400.0);
        return Math.max(0.0, decayedScore);
    }

    public String getAgentId() {
        return agentId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getSource() {
        return source;
    }

    public List<String> getTags() {
        return tags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public int getAccessCount() {
        return accessCount;
    }

    public double getImportanceScore() {
        return importanceScore;
    }

    public double getDecayFactor() {
        return decayFactor;
    }

    public static final class Builder {
        private String agentId;
        private String sessionId;
        private String source;
        private List<String> tags;
        private Instant createdAt;
        private Instant lastAccessedAt;
        private int accessCount;
        private double importanceScore;
        private double decayFactor;

        public Builder agentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder lastAccessedAt(Instant lastAccessedAt) {
            this.lastAccessedAt = lastAccessedAt;
            return this;
        }

        public Builder accessCount(int accessCount) {
            this.accessCount = accessCount;
            return this;
        }

        public Builder importanceScore(double importanceScore) {
            this.importanceScore = importanceScore;
            return this;
        }

        public Builder decayFactor(double decayFactor) {
            this.decayFactor = decayFactor;
            return this;
        }

        public MemoryMetadata build() {
            return new MemoryMetadata(agentId, sessionId, source, tags, createdAt,
                    lastAccessedAt, accessCount, importanceScore, decayFactor);
        }
    }
}
