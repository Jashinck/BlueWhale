package org.skylark.bluewhale.domain.port;

import java.util.List;

public interface EmbeddingPort {
    float[] embed(String text);

    List<float[]> embedBatch(List<String> texts);

    int getDimension();
}
