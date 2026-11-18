package org.skylark.bluewhale.application;

import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.feedback.Attribution;
import org.skylark.bluewhale.domain.feedback.AttributionCode;
import org.skylark.bluewhale.domain.feedback.AttributionFacts;
import org.skylark.bluewhale.domain.feedback.AttributionService;
import org.skylark.bluewhale.domain.feedback.Signal;
import org.skylark.bluewhale.domain.port.AttributionRepository;
import org.skylark.bluewhale.domain.port.ClockPort;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AttributeSignalUseCase {

    private final AttributionService attributionService = new AttributionService();
    private final AttributionRepository attributionRepository;
    private final ClockPort clock;

    public AttributeSignalUseCase(AttributionRepository attributionRepository, ClockPort clock) {
        this.attributionRepository = attributionRepository;
        this.clock = clock;
    }

    public Optional<Attribution> attribute(Signal signal, AttributionFacts facts) {
        Optional<AttributionCode> code = attributionService.attribute(facts);
        if (code.isEmpty()) {
            return Optional.empty();
        }
        Attribution attribution = Attribution.derive(signal.id(), code.get(), signal.unitId(), clock.now());
        attributionRepository.save(attribution);
        return Optional.of(attribution);
    }
}