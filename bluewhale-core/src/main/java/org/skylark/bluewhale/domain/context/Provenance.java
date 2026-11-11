package org.skylark.bluewhale.domain.context;

public record Provenance(
        UnitId episodeId,
        String sourceRef,
        Integer spanStart,
        Integer spanEnd,
        String ontologyVersion,
        String extractorVersion
) {

    public Provenance {
        if (episodeId == null && (sourceRef == null || sourceRef.isBlank())) {
            throw new IllegalArgumentException("provenance requires an episode or source reference");
        }
        if ((spanStart == null) != (spanEnd == null)) {
            throw new IllegalArgumentException("span start and end must be provided together");
        }
        if (spanStart != null && (spanStart < 0 || spanEnd < spanStart)) {
            throw new IllegalArgumentException("invalid evidence span");
        }
    }

    public static Provenance ofSource(String sourceRef) {
        return new Provenance(null, sourceRef, null, null, null, null);
    }

    public static Provenance ofEpisode(UnitId episodeId) {
        return new Provenance(episodeId, null, null, null, null, null);
    }
}
