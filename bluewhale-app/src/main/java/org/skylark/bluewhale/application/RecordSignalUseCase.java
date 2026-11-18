package org.skylark.bluewhale.application;

import org.skylark.bluewhale.domain.feedback.Signal;
import org.skylark.bluewhale.domain.port.SignalRepository;
import org.springframework.stereotype.Service;

@Service
public class RecordSignalUseCase {

    private final SignalRepository signalRepository;

    public RecordSignalUseCase(SignalRepository signalRepository) {
        this.signalRepository = signalRepository;
    }

    public void record(Signal signal) {
        signalRepository.append(signal);
    }
}