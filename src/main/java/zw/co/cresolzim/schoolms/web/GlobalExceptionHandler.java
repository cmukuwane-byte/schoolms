package zw.co.cresolzim.schoolms.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import jakarta.servlet.http.HttpServletRequest;
import zw.co.cresolzim.schoolms.service.NotFoundException;
import zw.co.cresolzim.schoolms.service.RuleViolationException;

/** Turns rule failures into a readable message on the page the user came from. */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuleViolationException.class)
    public RedirectView onRuleViolation(RuleViolationException ex, HttpServletRequest req,
                                        RedirectAttributes flash) {
        flash.addFlashAttribute("error", ex.getMessage());
        String back = req.getHeader("Referer");
        return new RedirectView(back == null ? "/dashboard" : back);
    }

    @ExceptionHandler(NotFoundException.class)
    public RedirectView onNotFound(NotFoundException ex, RedirectAttributes flash) {
        flash.addFlashAttribute("error", ex.getMessage());
        return new RedirectView("/dashboard");
    }
}
