package org.skylark.bluewhale.domain.event;

public class MemoryEvolvedEvent extends DomainEvent {
    private final String agentId;
    private final String evolutionType;
    private final int affectedCount;

    public MemoryEvolvedEvent(String agentId, String evolutionType, int affectedCount) {
        super();
        this.agentId = agentId;
        this.evolutionType = evolutionType;
        this.affectedCount = affectedCount;
    }

    @Override
    public String getEventType() {
        return "MEMORY_EVOLVED";
    }

    public String getAgentId() {
        return agentId;
    }

    public String getEvolutionType() {
        return evolutionType;
    }

    public int getAffectedCount() {
        return affectedCount;
    }
}
