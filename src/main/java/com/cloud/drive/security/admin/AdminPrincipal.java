package com.cloud.drive.security.admin;

import com.cloud.drive.model.AdminUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Authenticated administrator. Carries the admin id so mutating services can write
 * an accurate {@code admin_audit_log} row without another lookup.
 */
public class AdminPrincipal implements UserDetails {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final Long id;
    private final String email;
    private final String name;

    public AdminPrincipal(Long id, String email, String name) {
        this.id = id;
        this.email = email;
        this.name = name;
    }

    public static AdminPrincipal of(AdminUser admin) {
        return new AdminPrincipal(admin.getId(), admin.getEmail(), admin.getName());
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(ROLE_ADMIN));
    }

    @Override
    public String getPassword() { return ""; }

    @Override
    public String getUsername() { return email; }
}
