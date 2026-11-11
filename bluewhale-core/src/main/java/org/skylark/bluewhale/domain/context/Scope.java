package org.skylark.bluewhale.domain.context;

import java.util.StringJoiner;

public record Scope(
        String tenant,
        String workspace,
        String project,
        String agentId,
        String userId,
        String sessionId
) {

    public Scope {
        if (tenant == null || tenant.isBlank()) {
            throw new IllegalArgumentException("tenant is required");
        }
    }

    public static Scope of(String tenant, String workspace, String project,
                           String agentId, String userId, String sessionId) {
        return new Scope(tenant, workspace, project, agentId, userId, sessionId);
    }

    public static Scope agent(String tenant, String agentId) {
        return new Scope(tenant, null, null, agentId, null, null);
    }

    public static Scope agentSession(String tenant, String agentId, String sessionId) {
        return new Scope(tenant, null, null, agentId, null, sessionId);
    }

    public String path() {
        StringJoiner joiner = new StringJoiner("/");
        append(joiner, "ws", workspace);
        append(joiner, "project", project);
        append(joiner, "agent", agentId);
        append(joiner, "user", userId);
        append(joiner, "session", sessionId);
        return joiner.toString();
    }

    /**
     * A null dimension is broader than a concrete value. This lets a workspace
     * unit remain visible to a query scoped to one of its agents or sessions.
     */
    public boolean canSee(Scope other) {
        if (other == null || !tenant.equals(other.tenant)) {
            return false;
        }
        return compatible(workspace, other.workspace)
                && compatible(project, other.project)
                && compatible(agentId, other.agentId)
                && compatible(userId, other.userId)
                && compatible(sessionId, other.sessionId);
    }

    private static boolean compatible(String left, String right) {
        return left == null || right == null || left.equals(right);
    }

    private static void append(StringJoiner joiner, String key, String value) {
        if (value != null && !value.isBlank()) {
            joiner.add(key + ":" + value);
        }
    }
}
