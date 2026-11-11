package org.skylark.bluewhale.domain.governance;

import org.junit.jupiter.api.Test;
import org.skylark.bluewhale.domain.context.Actor;
import org.skylark.bluewhale.domain.context.ActorType;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChangeRequestTest {

    private static final Instant NOW = Instant.parse("2026-11-10T08:00:00Z");
    private static final Actor REVIEWER = new Actor("reviewer-1", ActorType.HUMAN);

    private final KnowledgePatch patch = new KnowledgePatch(
            Scope.of("acme", "ws-1", null, null, null, null),
            "owner-1", "legal", "a policy rule", "policy.md");

    @Test
    void shouldWalkHappyPathFromDraftToReleased() {
        ChangeRequest request = ChangeRequest.draft(ChangeRequestType.ADD, "author-1", patch, NOW);

        request = request.submitForReview(REVIEWER, NOW)
                .approve(REVIEWER, NOW)
                .startRegression(REVIEWER, NOW)
                .markReady(REVIEWER, NOW, null)
                .release(REVIEWER, NOW);

        assertEquals(ChangeRequestStatus.RELEASED, request.status());
    }

    @Test
    void shouldRequireRegressionEvidenceForRevise() {
        ChangeRequest request = ChangeRequest.draft(ChangeRequestType.REVISE, "author-1", patch, NOW)
                .submitForReview(REVIEWER, NOW)
                .approve(REVIEWER, NOW)
                .startRegression(REVIEWER, NOW);

        assertThrows(IllegalStateException.class,
                () -> request.markReady(REVIEWER, NOW, null));
    }

    @Test
    void systemAuthorCannotSelfApprove() {
        ChangeRequest request = ChangeRequest.draft(ChangeRequestType.ADD, "SYSTEM", patch, NOW)
                .submitForReview(REVIEWER, NOW);

        assertThrows(IllegalStateException.class,
                () -> request.approve(new Actor("SYSTEM", ActorType.SYSTEM), NOW));
    }

    @Test
    void rejectedRequestStopsTheFlow() {
        ChangeRequest request = ChangeRequest.draft(ChangeRequestType.ADD, "author-1", patch, NOW)
                .submitForReview(REVIEWER, NOW)
                .reject(REVIEWER, NOW);

        assertEquals(ChangeRequestStatus.REJECTED, request.status());
    }
}
