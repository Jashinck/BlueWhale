package org.skylark.bluewhale.domain.context;

public record Economy(
        int costTokens,
        int nSeen,
        int nCited,
        int nAdopted,
        int nCorrected,
        double importance
) {

    public Economy {
        if (costTokens < 0 || nSeen < 0 || nCited < 0 || nAdopted < 0 || nCorrected < 0) {
            throw new IllegalArgumentException("economy counters must not be negative");
        }
        if (importance < 0.0 || importance > 1.0) {
            throw new IllegalArgumentException("importance must be between 0 and 1");
        }
    }

    public static Economy initial(double importance) {
        return new Economy(0, 0, 0, 0, 0, importance);
    }

    public Economy record(Outcome outcome) {
        return switch (outcome) {
            case SEEN -> new Economy(costTokens, nSeen + 1, nCited, nAdopted, nCorrected, importance);
            case CITED -> new Economy(costTokens, nSeen, nCited + 1, nAdopted, nCorrected, importance);
            case ADOPTED -> new Economy(costTokens, nSeen, nCited, nAdopted + 1, nCorrected, importance);
            case CORRECTED -> new Economy(costTokens, nSeen, nCited, nAdopted, nCorrected + 1, importance);
        };
    }

    public Economy withCostTokens(int tokens) {
        return new Economy(Math.max(0, tokens), nSeen, nCited, nAdopted, nCorrected, importance);
    }
}
