package org.skylark.bluewhale.domain.context;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ContextUnit {

    public static final double AUTO_ACTIVE_CONFIDENCE = 0.7;

    private final UnitId id;
    private final Scope scope;
    private final UnitKind kind;
    private final Origin origin;
    private final UnitContent content;
    private final Lifecycle lifecycle;
    private final Ownership ownership;
    private final Trust trust;
    private final Economy economy;
    private final Provenance provenance;
    private final Lineage lineage;
    private final UnitId supersededBy;
    private final List<String> tags;

    private ContextUnit(UnitId id, Scope scope, UnitKind kind, Origin origin, UnitContent content,
                        Lifecycle lifecycle, Ownership ownership, Trust trust, Economy economy,
                        Provenance provenance, Lineage lineage, UnitId supersededBy, List<String> tags) {
        this.id = Objects.requireNonNull(id, "unit id is required");
        this.scope = Objects.requireNonNull(scope, "scope is required");
        this.kind = Objects.requireNonNull(kind, "kind is required");
        this.origin = Objects.requireNonNull(origin, "origin is required");
        this.content = Objects.requireNonNull(content, "content is required");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle is required");
        this.ownership = ownership == null ? Ownership.unowned() : ownership;
        this.trust = Objects.requireNonNull(trust, "trust is required");
        this.economy = Objects.requireNonNull(economy, "economy is required");
        this.provenance = Objects.requireNonNull(provenance, "provenance is required");
        this.lineage = lineage == null ? Lineage.empty() : lineage;
        this.supersededBy = supersededBy;
        this.tags = tags == null ? List.of() : List.copyOf(tags);

        if (origin == Origin.DECLARED && !this.ownership.hasOwner()) {
            throw new IllegalArgumentException("declared knowledge requires an owner");
        }
        if (origin == Origin.DISTILLED && !this.lineage.hasEvidenceOfDerivation()) {
            throw new IllegalArgumentException("distilled units require at least one lineage parent");
        }
        if (this.lineage.derivedFrom().contains(id)) {
            throw new IllegalArgumentException("a unit cannot derive from itself");
        }
    }

    public static ContextUnit create(UnitId id, Scope scope, UnitKind kind, Origin origin,
                                     UnitContent content, Lifecycle lifecycle, Ownership ownership,
                                     Trust trust, Economy economy, Provenance provenance,
                                     Lineage lineage, List<String> tags) {
        return new ContextUnit(id, scope, kind, origin, content, lifecycle, ownership,
                trust, economy, provenance, lineage, null, tags);
    }

    public static ContextUnit restore(UnitId id, Scope scope, UnitKind kind, Origin origin,
                                      UnitContent content, Lifecycle lifecycle, Ownership ownership,
                                      Trust trust, Economy economy, Provenance provenance,
                                      Lineage lineage, UnitId supersededBy, List<String> tags) {
        return new ContextUnit(id, scope, kind, origin, content, lifecycle, ownership,
                trust, economy, provenance, lineage, supersededBy, tags);
    }

    public static ContextUnit capture(Scope scope, UnitKind kind, Origin origin, UnitContent content,
                                      Provenance provenance, double confidence, Instant now,
                                      List<String> tags) {
        return create(UnitId.generate(), scope, kind, origin, content, Lifecycle.captured(now),
                Ownership.unowned(), Trust.observed(confidence), Economy.initial(confidence),
                provenance, Lineage.empty(), tags);
    }

    public TransitionResult transition(Transition transition, Actor actor, String reason,
                                       Instant now, UUID batchId) {
        Objects.requireNonNull(transition, "transition is required");
        Objects.requireNonNull(actor, "actor is required");
        Objects.requireNonNull(now, "transition time is required");
        if (lifecycle.state() != transition.from()) {
            throw new IllegalStateException("transition " + transition + " requires state "
                    + transition.from() + " but was " + lifecycle.state());
        }
        enforceGuard(transition, now);
        Lifecycle nextLifecycle = lifecycle.withState(transition.to(), now);
        ContextUnit next = copy(nextLifecycle, supersededBy);
        LifecycleEvent event = new LifecycleEvent(UUID.randomUUID(), id, lifecycle.state(),
                transition.to(), transition.name(), actor.id(), actor.type(), now, reason, batchId);
        return new TransitionResult(next, event);
    }

    public TransitionResult markSuperseded(UnitId replacement, Instant realityEnd,
                                           Instant systemTime, Actor actor, String reason) {
        Objects.requireNonNull(replacement, "replacement unit is required");
        Objects.requireNonNull(realityEnd, "reality end is required");
        Objects.requireNonNull(systemTime, "system time is required");
        if (replacement.equals(id)) {
            throw new IllegalArgumentException("a unit cannot supersede itself");
        }
        if (lifecycle.state() != UnitState.ACTIVE && lifecycle.state() != UnitState.STALE_SUSPECT) {
            throw new IllegalStateException("only active or stale units can be superseded");
        }
        ContextUnit next = copy(lifecycle.superseded(realityEnd, systemTime), replacement);
        LifecycleEvent event = new LifecycleEvent(UUID.randomUUID(), id, lifecycle.state(),
                UnitState.SUPERSEDED, "SUPERSEDE", actor.id(), actor.type(), systemTime,
                reason, null);
        return new TransitionResult(next, event);
    }

    public ContextUnit recordApplied(Outcome outcome) {
        return copy(lifecycle, supersededBy, economy.record(outcome));
    }

    public ContextUnit withImportance(double importance) {
        return copy(lifecycle, supersededBy, economy.withImportance(importance));
    }

    public boolean isCurrentTruth(Instant now) {
        return lifecycle.state() == UnitState.ACTIVE
                && supersededBy == null
                && lifecycle.validTo() == null
                && lifecycle.isEffectiveAt(now);
    }

    public boolean isRetrievable(Instant now) {
        return lifecycle.state().isRetrievable()
                && supersededBy == null
                && lifecycle.validTo() == null
                && lifecycle.isEffectiveAt(now);
    }

    public boolean isVisibleTo(Scope queryScope, AccessPolicy policy) {
        return policy.allows(queryScope) && queryScope.canSee(scope);
    }

    public String contentHash() {
        String material = String.join("\u001f",
                nullToEmpty(content.abstractText()),
                nullToEmpty(content.overview()),
                nullToEmpty(content.inlineText()),
                nullToEmpty(content.ref()),
                kind.name(),
                origin.name(),
                provenance.ontologyVersion(),
                provenance.extractorVersion());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private void enforceGuard(Transition transition, Instant now) {
        switch (transition) {
            case EXTRACTED_TO_ACTIVE -> {
                if (origin != Origin.OBSERVED || trust.confidence() < AUTO_ACTIVE_CONFIDENCE) {
                    throw new IllegalStateException("automatic activation requires observed origin and sufficient confidence");
                }
            }
            case EXTRACTED_TO_PENDING_REVIEW -> {
                if (origin != Origin.DECLARED && origin != Origin.DISTILLED) {
                    throw new IllegalStateException("review is required only for declared or distilled knowledge");
                }
            }
            case PENDING_REVIEW_TO_SCHEDULED -> {
                if (lifecycle.validFrom() == null || !lifecycle.validFrom().isAfter(now)) {
                    throw new IllegalStateException("scheduling requires a future validFrom");
                }
            }
            case PENDING_REVIEW_TO_ACTIVE -> {
                if (lifecycle.validFrom() != null && lifecycle.validFrom().isAfter(now)) {
                    throw new IllegalStateException("future-effective knowledge must be scheduled");
                }
            }
            case SCHEDULED_TO_ACTIVE -> {
                if (lifecycle.validFrom() != null && lifecycle.validFrom().isAfter(now)) {
                    throw new IllegalStateException("validFrom has not been reached");
                }
            }
            case EXPIRED_TO_ERASED -> {
                if (ownership.legalHold()) {
                    throw new IllegalStateException("legal hold blocks erasure");
                }
            }
            default -> {
            }
        }
    }

    private ContextUnit copy(Lifecycle nextLifecycle, UnitId nextSupersededBy) {
        return copy(nextLifecycle, nextSupersededBy, economy);
    }

    private ContextUnit copy(Lifecycle nextLifecycle, UnitId nextSupersededBy, Economy nextEconomy) {
        return new ContextUnit(id, scope, kind, origin, content, nextLifecycle, ownership, trust,
                nextEconomy, provenance, lineage, nextSupersededBy, tags);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public UnitId id() {
        return id;
    }

    public Scope scope() {
        return scope;
    }

    public UnitKind kind() {
        return kind;
    }

    public Origin origin() {
        return origin;
    }

    public UnitContent content() {
        return content;
    }

    public Lifecycle lifecycle() {
        return lifecycle;
    }

    public Ownership ownership() {
        return ownership;
    }

    public Trust trust() {
        return trust;
    }

    public Economy economy() {
        return economy;
    }

    public Provenance provenance() {
        return provenance;
    }

    public Lineage lineage() {
        return lineage;
    }

    public UnitId supersededBy() {
        return supersededBy;
    }

    public List<String> tags() {
        return tags;
    }
}
