package org.skylark.bluewhale.domain.context;

public record Ownership(
        String ownerId,
        String ownerOrg,
        Integer reviewCycleDays,
        boolean legalHold,
        String holdReason
) {

    public Ownership {
        if (reviewCycleDays != null && reviewCycleDays <= 0) {
            throw new IllegalArgumentException("review cycle must be positive");
        }
        if (legalHold && (holdReason == null || holdReason.isBlank())) {
            throw new IllegalArgumentException("legal hold requires a reason");
        }
    }

    public static Ownership unowned() {
        return new Ownership(null, null, null, false, null);
    }

    public static Ownership owned(String ownerId, String ownerOrg, int reviewCycleDays) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("owner id is required");
        }
        return new Ownership(ownerId, ownerOrg, reviewCycleDays, false, null);
    }

    public boolean hasOwner() {
        return ownerId != null && !ownerId.isBlank();
    }
}
