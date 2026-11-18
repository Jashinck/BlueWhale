package org.skylark.bluewhale.domain.port;

import org.skylark.bluewhale.domain.knowledge.Claim;

import java.util.List;

public interface ClaimExtractionPort {

    List<Claim> extractClaims(String sourceText);
}