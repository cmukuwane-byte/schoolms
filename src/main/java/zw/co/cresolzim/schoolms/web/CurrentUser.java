package zw.co.cresolzim.schoolms.web;

import org.springframework.security.core.context.SecurityContextHolder;
import zw.co.cresolzim.schoolms.config.AppUserDetails;

/** Small helper so controllers do not repeat the same principal plumbing. */
public final class CurrentUser {

    private CurrentUser() {}

    public static AppUserDetails details() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails d)) return null;
        return d;
    }

    public static String name() {
        AppUserDetails d = details();
        return d == null ? "system" : d.getDisplayName();
    }

    public static Long staffId() {
        AppUserDetails d = details();
        return d == null ? null : d.getStaffId();
    }

    /** True if the signed-in user holds any of the given authorities. */
    public static boolean hasAnyRole(String... roles) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        for (var granted : auth.getAuthorities()) {
            for (String r : roles) {
                if (granted.getAuthority().equals(r)) return true;
            }
        }
        return false;
    }

    public static Long guardianId() {
        AppUserDetails d = details();
        return d == null ? null : d.getGuardianId();
    }
}
