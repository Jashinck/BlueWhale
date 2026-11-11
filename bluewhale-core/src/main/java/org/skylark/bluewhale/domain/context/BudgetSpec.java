package org.skylark.bluewhale.domain.context;

public record BudgetSpec(int maxTokens) {

    public BudgetSpec {
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("max tokens must be positive");
        }
    }

    public static BudgetSpec defaults() {
        return new BudgetSpec(2000);
    }
}
