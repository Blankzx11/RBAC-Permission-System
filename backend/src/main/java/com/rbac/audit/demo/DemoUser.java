package com.rbac.audit.demo;

import com.rbac.audit.port.AuditIdentity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import java.util.Set;

public class DemoUser extends User implements AuditIdentity {
    private final long userId;
    private final String displayName;
    private final Long sysId;
    private final boolean platformAdmin;
    private final Set<String> permissions;

    public DemoUser(String username, String encodedPassword, long userId, String displayName, Long sysId, boolean platformAdmin, Set<String> permissions) {
        super(username, encodedPassword, permissions.stream().map(SimpleGrantedAuthority::new).toList());
        this.userId = userId; this.displayName = displayName; this.sysId = sysId;
        this.platformAdmin = platformAdmin; this.permissions = Set.copyOf(permissions);
    }

    @Override public long userId() { return userId; }
    @Override public String displayName() { return displayName; }
    @Override public Long sysId() { return sysId; }
    @Override public boolean platformAdmin() { return platformAdmin; }
    @Override public Set<String> permissionCodes() { return permissions; }
}
