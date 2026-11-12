package org.skylark.bluewhale.domain.port;

import java.util.List;

public interface ExtractionPort {

    String distill(List<String> sources);
}