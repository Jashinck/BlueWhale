package org.skylark.bluewhale.domain.context;

import org.junit.jupiter.api.Test;
import org.skylark.bluewhale.domain.model.memory.Memory;
import org.skylark.bluewhale.domain.model.memory.MemoryContent;
import org.skylark.bluewhale.domain.model.memory.MemoryMetadata;
import org.skylark.bluewhale.domain.model.memory.MemoryType;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemoryContextMapperTest {

    @Test
    void shouldMapLegacyMemoryWithoutLosingIdentityOrContext() {
        Memory memory = Memory.create(MemoryType.SEMANTIC,
                MemoryContent.ofText("Agent prefers concise answers"),
                MemoryMetadata.builder()
                        .agentId("agent-7")
                        .sessionId("session-2")
                        .source("conversation")
                        .tags(List.of("preference"))
                        .createdAt(Instant.parse("2026-11-01T00:00:00Z"))
                        .lastAccessedAt(Instant.parse("2026-11-02T00:00:00Z"))
                        .accessCount(4)
                        .importanceScore(0.8)
                        .decayFactor(0.1)
                        .build());

        ContextUnit unit = MemoryContextMapper.toContextUnit(memory);

        assertEquals(memory.getId().getValue(), unit.id().toString());
        assertEquals("agent-7", unit.scope().agentId());
        assertEquals("session-2", unit.scope().sessionId());
        assertEquals(UnitKind.SEMANTIC, unit.kind());
        assertEquals("Agent prefers concise answers", unit.content().textAt(RepresentationDepth.DETAIL));
        assertEquals(4, unit.economy().nSeen());
        assertEquals(0.8, unit.trust().confidence());
        assertEquals(List.of("preference"), unit.tags());
    }
}
