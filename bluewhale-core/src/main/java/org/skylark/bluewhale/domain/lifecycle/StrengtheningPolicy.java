package org.skylark.bluewhale.domain.lifecycle;

import org.skylark.bluewhale.domain.context.Economy;

//  provisional：采纳/纠正驱动重要度，阈值待历史数据标定后调整。
public record StrengtheningPolicy(double adoptionBoost, double correctionPenalty) {

    public StrengtheningPolicy {
        if (adoptionBoost < 0.0 || correctionPenalty < 0.0) {
            throw new IllegalArgumentException("policy weights must not be negative");
        }
    }

    public static StrengtheningPolicy defaultPolicy() {
        return new StrengtheningPolicy(0.1, 0.2);
    }

    public double recomputeImportance(Economy economy) {
        double value = 0.5 + adoptionBoost * economy.nAdopted() - correctionPenalty * economy.nCorrected();
        return Math.max(0.0, Math.min(1.0, value));
    }
}