package org.skylark.bluewhale.domain.context;

import org.skylark.bluewhale.domain.model.memory.Memory;
import org.skylark.bluewhale.domain.model.memory.MemoryMetadata;

import java.time.Instant;

public final class MemoryContextMapper {

    private MemoryContextMapper() {
    }

    public static ContextUnit toContextUnit(Memory memory) {
        MemoryMetadata metadata = memory.getMetadata();
        Instant createdAt = metadata.getCreatedAt() == null ? Instant.now() : metadata.getCreatedAt();
        String text = memory.getContent().getText();
        Lifecycle lifecycle = new Lifecycle(UnitState.ACTIVE, createdAt, null, createdAt,
                null, null, createdAt, null, null, null);
        Provenance provenance = Provenance.ofSource(
                metadata.getSource() == null ? "legacy-memory" : metadata.getSource());
        Economy economy = new Economy(0, metadata.getAccessCount(), 0, 0, 0,
                clamp(metadata.getImportanceScore()));
        Trust trust = new Trust(clamp(metadata.getImportanceScore()), 3);

        return ContextUnit.create(
                UnitId.of(memory.getId().getValue()),
                Scope.agentSession("default", metadata.getAgentId(), metadata.getSessionId()),
                UnitKind.valueOf(memory.getType().name()),
                Origin.OBSERVED,
                UnitContent.of(text),
                lifecycle,
                Ownership.unowned(),
                trust,
                economy,
                provenance,
                Lineage.empty(),
                metadata.getTags()
        );
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
