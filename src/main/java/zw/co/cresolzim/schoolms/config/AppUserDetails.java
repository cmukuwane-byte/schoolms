package zw.co.cresolzim.schoolms.config;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import zw.co.cresolzim.schoolms.domain.AppUser;

import java.util.Collection;
import java.util.stream.Collectors;

@Getter
public class AppUserDetails implements UserDetails {

    private final Long userId;
    private final Long staffId;
    private final Long guardianId;
    private final String displayName;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final boolean mustChangePassword;
    private final Collection<GrantedAuthority> authorities;

    public AppUserDetails(AppUser u) {
        this.userId = u.getId();
        this.staffId = u.getStaff() == null ? null : u.getStaff().getId();
        this.guardianId = u.getGuardian() == null ? null : u.getGuardian().getId();
        this.displayName = u.getDisplayName();
        this.username = u.getUsername();
        this.password = u.getPassword();
        this.enabled = u.isEnabled();
        this.mustChangePassword = u.isMustChangePassword();
        this.authorities = u.getRoles().stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority(r.getName()))
                .collect(Collectors.toSet());
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}
