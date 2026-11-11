package org.skylark.bluewhale.domain.model.graph;

public class MemoryNode {
    private final String memoryId;
    private final String label;
    /** 节点权重，反映该记忆在图中的中心性 */
    private final double weight;

    public MemoryNode(String memoryId, String label, double weight) {
        this.memoryId = memoryId;
        this.label = label;
        this.weight = weight;
    }

    public static MemoryNode create(String memoryId, String label, double weight) {
        return new MemoryNode(memoryId, label, weight);
    }

    public MemoryNode withWeight(double newWeight) {
        return new MemoryNode(memoryId, label, newWeight);
    }

    public String getMemoryId() {
        return memoryId;
    }

    public String getLabel() {
        return label;
    }

    public double getWeight() {
        return weight;
    }
}
