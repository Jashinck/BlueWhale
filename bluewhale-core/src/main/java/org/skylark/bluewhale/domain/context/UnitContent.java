package org.skylark.bluewhale.domain.context;

public record UnitContent(
        String abstractText,
        String overview,
        String inlineText,
        String ref
) {

    public UnitContent {
        if (abstractText == null || abstractText.isBlank()) {
            throw new IllegalArgumentException("abstract content is required");
        }
    }

    public static UnitContent of(String text) {
        return new UnitContent(text, text, text, null);
    }

    public String textAt(RepresentationDepth depth) {
        return switch (depth) {
            case ABSTRACT -> abstractText;
            case OVERVIEW -> hasText(overview) ? overview : abstractText;
            case DETAIL -> hasText(inlineText) ? inlineText : hasText(ref) ? ref : textAt(RepresentationDepth.OVERVIEW);
        };
    }

    public int estimatedTokensAt(RepresentationDepth depth) {
        return Math.max(1, (textAt(depth).length() + 3) / 4);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
