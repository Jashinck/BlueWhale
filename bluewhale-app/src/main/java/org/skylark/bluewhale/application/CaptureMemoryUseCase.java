package org.skylark.bluewhale.application;

import org.skylark.bluewhale.domain.context.Actor;
import org.skylark.bluewhale.domain.context.ContextUnit;
import org.skylark.bluewhale.domain.context.Origin;
import org.skylark.bluewhale.domain.context.Provenance;
import org.skylark.bluewhale.domain.context.RepresentationLine;
import org.skylark.bluewhale.domain.context.Transition;
import org.skylark.bluewhale.domain.context.TransitionResult;
import org.skylark.bluewhale.domain.context.UnitContent;
import org.skylark.bluewhale.domain.context.UnitKind;
import org.skylark.bluewhale.domain.context.UnitState;
import org.skylark.bluewhale.domain.memory.Episode;
import org.skylark.bluewhale.domain.port.ClockPort;
import org.skylark.bluewhale.domain.port.ContextUnitRepository;
import org.skylark.bluewhale.domain.port.EmbeddingPort;
import org.skylark.bluewhale.domain.port.EpisodeRepository;
import org.skylark.bluewhale.domain.port.IndexPort;
import org.skylark.bluewhale.domain.port.LifecycleEventRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class CaptureMemoryUseCase {

    private static final Actor EXTRACTOR = Actor.system("memory-extractor");

    private final EpisodeRepository episodeRepository;
    private final ContextUnitRepository unitRepository;
    private final LifecycleEventRepository eventRepository;
    private final EmbeddingPort embeddingPort;
    private final IndexPort indexPort;
    private final ClockPort clock;

    public CaptureMemoryUseCase(EpisodeRepository episodeRepository,
                                ContextUnitRepository unitRepository,
                                LifecycleEventRepository eventRepository,
                                EmbeddingPort embeddingPort, IndexPort indexPort, ClockPort clock) {
        this.episodeRepository = episodeRepository;
        this.unitRepository = unitRepository;
        this.eventRepository = eventRepository;
        this.embeddingPort = embeddingPort;
        this.indexPort = indexPort;
        this.clock = clock;
    }

    @Transactional
    public CaptureResult capture(CaptureSignal signal) {
        Instant now = clock.now();
        Episode episode = Episode.capture(signal.scope(), signal.source(), signal.text(), now);
        episodeRepository.append(episode);

        ContextUnit unit = ContextUnit.capture(signal.scope(), signal.kind(), Origin.OBSERVED,
                UnitContent.of(signal.text()), Provenance.ofEpisode(episode.id()),
                signal.confidence(), now, signal.tags());
        unitRepository.save(unit);
        unit = transition(unit, Transition.CAPTURE_TO_EXTRACTED, "capture extracted");
        if (signal.confidence() >= ContextUnit.AUTO_ACTIVE_CONFIDENCE) {
            unit = transition(unit, Transition.EXTRACTED_TO_ACTIVE, "observed confidence threshold met");
        }

        boolean indexed = index(unit);
        return new CaptureResult(signal.traceId(), episode.id().toString(), unit.id().toString(),
                unit.lifecycle().state(), indexed);
    }

    private ContextUnit transition(ContextUnit unit, Transition transition, String reason) {
        TransitionResult result = unit.transition(transition, EXTRACTOR, reason, clock.now(), null);
        boolean updated = unitRepository.updateIfState(
                result.unit().id(), result.event().fromState(), result.unit());
        if (!updated) {
            throw new OptimisticLockingFailureException(
                    "state of " + result.unit().id() + " changed concurrently");
        }
        eventRepository.append(result.event());
        return result.unit();
    }

    private boolean index(ContextUnit unit) {        if (unit.lifecycle().state() != UnitState.ACTIVE) {
            return false;
        }
        try {
            float[] embedding = embeddingPort.embed(unit.content().abstractText());
            indexPort.index(new IndexPort.IndexEntry(unit.id(), unit.scope(),
                    indexLine(unit.kind()), unit.content().abstractText(), embedding,
                    Map.of("kind", unit.kind().name(), "origin", unit.origin().name(),
                            "state", unit.lifecycle().state().name())));
            return true;
        } catch (RuntimeException unavailable) {
            indexPort.invalidate(unit.id());
            return false;
        }
    }

    private static RepresentationLine indexLine(UnitKind kind) {
        return kind == UnitKind.EPISODIC
                ? RepresentationLine.EPISODE_TIMELINE
                : RepresentationLine.CURATED_DOC;
    }
}
