package zw.co.cresolzim.schoolms.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import zw.co.cresolzim.schoolms.repo.AppUserRepository;
import zw.co.cresolzim.schoolms.service.RuleViolationException;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    @GetMapping("/password")
    public String form(@RequestParam(required = false) Boolean first, Model model) {
        model.addAttribute("first", Boolean.TRUE.equals(first));
        return "account/password";
    }

    @PostMapping("/password")
    public String change(@RequestParam String currentPassword,
                         @RequestParam String newPassword,
                         @RequestParam String confirmPassword,
                         RedirectAttributes flash) {

        var user = users.findById(CurrentUser.details().getUserId()).orElseThrow();

        if (!encoder.matches(currentPassword, user.getPassword())) {
            throw new RuleViolationException("The current password is not correct.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new RuleViolationException("The two new passwords do not match.");
        }
        if (newPassword.length() < 8) {
            throw new RuleViolationException("Use at least eight characters.");
        }

        user.setPassword(encoder.encode(newPassword));
        user.setMustChangePassword(false);
        users.save(user);

        flash.addFlashAttribute("message", "Password changed.");
        return "redirect:/dashboard";
    }
}
