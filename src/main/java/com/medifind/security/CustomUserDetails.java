package com.medifind.security;

import com.medifind.entity.User;
import com.medifind.enums.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * SLP: Platform Security & Compliance → "Role-based access control (RBAC)
 * framework"
 *
 * Spring Security does not know about our {@link User} entity directly —
 * it works against its own {@link UserDetails} interface. This class is

 */
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    /** Escape hatch back to the real entity for controllers/services that need more than Spring Security does. */
    public User getUser() {
        return user;
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * SLP: Patient Authentication & Profile → "Rate limiting on patient
     * login attempts"
     * Backs the account lockout: once AuthService sets lockedUntil to a
     * future timestamp, Spring Security itself refuses to authenticate
     * this account until that moment passes.
     */
    @Override
    public boolean isAccountNonLocked() {
        return user.getLockedUntil() == null || user.getLockedUntil().isBefore(java.time.LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** SLP: Admin Account & Verification Mgmt → "View and suspend patient accounts" */
    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE;
    }
}
