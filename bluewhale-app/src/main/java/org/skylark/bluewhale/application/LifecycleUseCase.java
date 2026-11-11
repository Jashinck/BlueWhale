package org.skylark.bluewhale.application;

import org.skylark.bluewhale.domain.context.Actor;
import org.skylark.bluewhale.domain.context.ContextUnit;
import org.skylark.bluewhale.domain.context.LifecycleEvent;
import org.skylark.bluewhale.domain.context.Transition;
import org.skylark.bluewhale.domain.context.TransitionResult;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.port.ClockPort;
import org.skylark.bluewhale.domain.port.ContextUnitRepository;
import org.skylark.bluewhale.domain.port.LifecycleEventRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class LifecycleUseCase {

    private final ContextUnitRepository unitRepository;
    private final LifecycleEventRepository eventRepository;
    private final ClockPort clock;

    public LifecycleUseCase(ContextUnitRepository unitRepository,
                            LifecycleEventRepository eventRepository, ClockPort clock) {
        this.unitRepository = unitRepository;
        this.eventRepository = eventRepository;
        this.clock = clock;
    }

    @Transactional
    public TransitionResult transition(UnitId id, Transition transition, Actor actor,
                                       String reason, UUID batchId) {
        ContextUnit current = unitRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("unit not found: " + id));
        TransitionResult result = current.transition(transition, actor, reason, clock.now(), batchId);
        persist(result);
        return result;
    }

    public List<LifecycleEvent> history(UnitId id) {
        return eventRepository.findByUnitId(id);
    }

    private void persist(TransitionResult result) {
        boolean updated = unitRepository.updateIfState(
                result.unit().id(), result.event().fromState(), result.unit());
        if (!updated) {
            throw new OptimisticLockingFailureException(
                    "state of " + result.unit().id() + " changed concurrently");
        }
        eventRepository.append(result.event());
    }
}
