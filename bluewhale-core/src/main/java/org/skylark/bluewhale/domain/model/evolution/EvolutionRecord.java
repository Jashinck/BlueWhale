package org.skylark.bluewhale.domain.model.evolution;

import java.time.Instant;
import java.util.UUID;

/**
 * EvolutionRecord实体 - 记录每次记忆进化操作的历史
 */
public class EvolutionRecord {
    private final String id;
    private final String agentId;
    private final EvolutionType type;
    private final String targetMemoryId;
    private final String description;
    private final double scoreBefore;
    private final double scoreAfter;
    private final Instant occurredAt;

    public EvolutionRecord(String id, String agentId, EvolutionType type,
                           String targetMemoryId, String description,
                           double scoreBefore, double scoreAfter, Instant occurredAt) {
        this.id = id;
        this.agentId = agentId;
        this.type = type;
        this.targetMemoryId = targetMemoryId;
        this.description = description;
        this.scoreBefore = scoreBefore;
        this.scoreAfter = scoreAfter;
        this.occurredAt = occurredAt;
    }

    public static EvolutionRecord create(String agentId, EvolutionType type,
                                         String targetMemoryId, String description,
                                         double scoreBefore, double scoreAfter) {
        return new EvolutionRecord(UUID.randomUUID().toString(), agentId, type,
                targetMemoryId, description, scoreBefore, scoreAfter, Instant.now());
    }

    public enum EvolutionType {
        CONSOLIDATION,
        FORGETTING,
        STRENGTHENING,
        ABSTRACTION,
        ASSOCIATION
    }

    public String getId() {
        return id;
    }

    public String getAgentId() {
        return agentId;
    }

    public EvolutionType getType() {
        return type;
    }

    public String getTargetMemoryId() {
        return targetMemoryId;
    }

    public String getDescription() {
        return description;
    }

    public double getScoreBefore() {
        return scoreBefore;
    }

    public double getScoreAfter() {
        return scoreAfter;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
