package org.skylark.bluewhale.domain.model.graph;

public class MemoryEdge {
    private final String sourceMemoryId;
    private final String targetMemoryId;
    /** 关系类型，例如：RELATED_TO, CAUSED_BY, PART_OF, CONTRADICTS */
    private final String relation;
    /** 关系强度 [0.0, 1.0] */
    private final double weight;

    public MemoryEdge(String sourceMemoryId, String targetMemoryId, String relation, double weight) {
        this.sourceMemoryId = sourceMemoryId;
        this.targetMemoryId = targetMemoryId;
        this.relation = relation;
        this.weight = weight;
    }

    public static MemoryEdge create(String sourceMemoryId, String targetMemoryId,
                                    String relation, double weight) {
        return new MemoryEdge(sourceMemoryId, targetMemoryId, relation, weight);
    }

    public MemoryEdge strengthen(double delta) {
        return new MemoryEdge(sourceMemoryId, targetMemoryId, relation,
                Math.min(1.0, weight + delta));
    }

    public String getSourceMemoryId() {
        return sourceMemoryId;
    }

    public String getTargetMemoryId() {
        return targetMemoryId;
    }

    public String getRelation() {
        return relation;
    }

    public double getWeight() {
        return weight;
    }
}
