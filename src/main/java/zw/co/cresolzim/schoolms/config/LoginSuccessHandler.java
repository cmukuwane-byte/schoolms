package zw.co.cresolzim.schoolms.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import zw.co.cresolzim.schoolms.domain.Role;
import zw.co.cresolzim.schoolms.repo.AppUserRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/** Sends each role to the screen it actually needs, and stamps the last login. */
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AppUserRepository users;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res,
                                        Authentication auth) throws IOException, ServletException {

        AppUserDetails principal = (AppUserDetails) auth.getPrincipal();

        users.findById(principal.getUserId()).ifPresent(u -> {
            u.setLastLogin(LocalDateTime.now());
            users.save(u);
        });

        if (principal.isMustChangePassword()) {
            getRedirectStrategy().sendRedirect(req, res, "/account/password?first=true");
            return;
        }

        Set<String> roles = auth.getAuthorities().stream()
                .map(a -> a.getAuthority()).collect(Collectors.toSet());

        String target = "/dashboard";
        if (roles.contains(Role.GUARDIAN) && !roles.contains(Role.ADMIN)) target = "/portal";
        else if (roles.contains(Role.LIBRARIAN) && roles.size() == 1) target = "/library";
        else if (roles.contains(Role.TEACHER) && !roles.contains(Role.ADMIN) && !roles.contains(Role.HEAD))
            target = "/dashboard/teacher";

        getRedirectStrategy().sendRedirect(req, res, target);
    }
}
