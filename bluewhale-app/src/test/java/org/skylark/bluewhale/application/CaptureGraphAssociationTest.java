package org.skylark.bluewhale.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.context.UnitKind;
import org.skylark.bluewhale.domain.graph.EdgeType;
import org.skylark.bluewhale.domain.graph.UnitEdge;
import org.skylark.bluewhale.domain.memory.CaptureSignal;
import org.skylark.bluewhale.domain.port.EmbeddingPort;
import org.skylark.bluewhale.infrastructure.index.InMemoryIndexAdapter;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryContextUnitRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryEpisodeRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryLifecycleEventRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryUnitEdgeRepository;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaptureGraphAssociationTest {

    private static final Instant NOW = Instant.parse("2026-11-11T08:00:00Z");
    private static final Scope SCOPE = Scope.agent("acme", "agent-1");

    private InMemoryUnitEdgeRepository edgeRepository;
    private CaptureMemoryUseCase capture;

    @BeforeEach
    void setUp() {
        InMemoryContextUnitRepository unitRepository = new InMemoryContextUnitRepository();
        edgeRepository = new InMemoryUnitEdgeRepository();
        InMemoryEpisodeRepository episodes = new InMemoryEpisodeRepository();
        InMemoryLifecycleEventRepository events = new InMemoryLifecycleEventRepository();
        InMemoryIndexAdapter index = new InMemoryIndexAdapter();
        capture = new CaptureMemoryUseCase(episodes, unitRepository, events,
                new FixedEmbedding(), index, edgeRepository, () -> NOW);
    }

    @Test
    void capturingSimilarMemoryCreatesSimilarToEdge() {
        CaptureResult first = capture.capture(signal("first fact"));
        CaptureResult second = capture.capture(signal("second fact"));

        List<UnitEdge> edges = edgeRepository.findByFrom(UnitId.of(second.unitId()));

        assertEquals(1, edges.size());
        assertEquals(EdgeType.SIMILAR_TO, edges.get(0).type());
        assertEquals(UnitId.of(first.unitId()), edges.get(0).to());
        assertEquals(0.5, edges.get(0).weight(), 1e-9);
    }

    private static CaptureSignal signal(String text) {
        return new CaptureSignal("trace", SCOPE, "conversation", text, UnitKind.EPISODIC, 0.9, List.of());
    }

    private static final class FixedEmbedding implements EmbeddingPort {
        @Override
        public float[] embed(String text) {
            return new float[]{1, 0, 0};
        }

        @Override
        public List<float[]> embedBatch(List<String> texts) {
            return texts.stream().map(this::embed).toList();
        }

        @Override
        public int getDimension() {
            return 3;
        }
    }
}