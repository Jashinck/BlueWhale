package org.skylark.bluewhale.domain.context;

import java.util.Set;

public record AccessPolicy(Set<String> tenants, Set<String> workspaces, Set<String> agents) {

    public AccessPolicy {
        tenants = tenants == null ? Set.of() : Set.copyOf(tenants);
        workspaces = workspaces == null ? Set.of() : Set.copyOf(workspaces);
        agents = agents == null ? Set.of() : Set.copyOf(agents);
    }

    public static AccessPolicy open() {
        return new AccessPolicy(Set.of(), Set.of(), Set.of());
    }

    public boolean allows(Scope scope) {
        return scope != null
                && allowed(tenants, scope.tenant())
                && allowed(workspaces, scope.workspace())
                && allowed(agents, scope.agentId());
    }

    private static boolean allowed(Set<String> allowed, String value) {
        return allowed.isEmpty() || (value != null && allowed.contains(value));
    }
}