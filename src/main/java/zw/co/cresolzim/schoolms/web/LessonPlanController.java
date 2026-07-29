package zw.co.cresolzim.schoolms.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.repo.*;
import zw.co.cresolzim.schoolms.service.LessonPlanService;
import zw.co.cresolzim.schoolms.service.RuleViolationException;

@Controller
@RequestMapping("/lessons")
@RequiredArgsConstructor
public class LessonPlanController {

    private final LessonPlanService service;
    private final TermRepository terms;
    private final CurriculumTopicRepository curriculum;

    /** A teacher sees their own plans; a head sees the review queue. */
    @GetMapping
    public String index(Model model) {
        Term term = terms.findByCurrentTrue().orElse(null);
        model.addAttribute("term", term);

        boolean isReviewer = CurrentUser.hasAnyRole(Role.ADMIN, Role.HEAD);
        model.addAttribute("isReviewer", isReviewer);

        if (isReviewer) {
            model.addAttribute("queue", service.awaitingReview());
            model.addAttribute("board", term == null
                    ? java.util.List.of() : service.submissionBoard(term.getId()));
        }

        Long staffId = CurrentUser.staffId();
        if (staffId != null && term != null) {
            model.addAttribute("mine", service.mine(staffId, term.getId()));
            model.addAttribute("allocations", service.allocationsOf(staffId));
        }
        return "lessons/index";
    }

    @GetMapping("/new")
    public String blank(@RequestParam(required = false) Long allocationId, Model model) {
        requireTeacher();
        LessonPlan plan = new LessonPlan();
        model.addAttribute("plan", plan);
        model.addAttribute("allocations", service.allocationsOf(CurrentUser.staffId()));
        model.addAttribute("term", terms.findByCurrentTrue().orElse(null));
        return "lessons/form";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        LessonPlan plan = service.get(id);
        requireOwnerOrReviewer(plan);
        model.addAttribute("plan", plan);
        model.addAttribute("isReviewer", CurrentUser.hasAnyRole(Role.ADMIN, Role.HEAD));
        model.addAttribute("isOwner", isOwner(plan));
        return "lessons/view";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        LessonPlan plan = service.get(id);
        if (!isOwner(plan)) throw new RuleViolationException("You may only edit your own plans.");
        model.addAttribute("plan", plan);
        model.addAttribute("allocations", service.allocationsOf(CurrentUser.staffId()));
        model.addAttribute("term", plan.getTerm());
        return "lessons/form";
    }

    /** Pulls the scheme-of-work row for a week, so the teacher starts from the syllabus. */
    @GetMapping("/suggest")
    @ResponseBody
    public Object suggest(@RequestParam Long subjectId, @RequestParam Integer gradeLevel,
                          @RequestParam Integer termNumber, @RequestParam Integer weekNumber) {
        return curriculum
                .findBySubjectIdAndGradeLevelAndTermNumberAndWeekNumber(
                        subjectId, gradeLevel, termNumber, weekNumber)
                .map(c -> java.util.Map.of(
                        "topic", nz(c.getTopic()),
                        "subTopic", nz(c.getSubTopic()),
                        "objectives", nz(c.getObjectives()),
                        "activities", nz(c.getSuggestedActivities()),
                        "resources", nz(c.getResources())))
                .orElse(java.util.Map.of());
    }

    @PostMapping
    public String save(@ModelAttribute("plan") LessonPlan plan,
                       @RequestParam(defaultValue = "false") boolean submit,
                       RedirectAttributes flash) {
        requireTeacher();
        LessonPlan saved = service.saveDraft(plan, CurrentUser.staffId(), CurrentUser.name());

        if (submit) {
            service.submit(saved.getId(), CurrentUser.staffId());
            flash.addFlashAttribute("message", "Plan submitted for review.");
        } else {
            flash.addFlashAttribute("message", "Draft saved.");
        }
        return "redirect:/lessons/" + saved.getId();
    }

    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Long id, RedirectAttributes flash) {
        service.submit(id, CurrentUser.staffId());
        flash.addFlashAttribute("message", "Plan submitted for review.");
        return "redirect:/lessons/" + id;
    }

    /* ---------------------------------------------------------- head only */

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority(T(zw.co.cresolzim.schoolms.domain.Role).ADMIN, " +
                  "T(zw.co.cresolzim.schoolms.domain.Role).HEAD)")
    public String approve(@PathVariable Long id,
                          @RequestParam(required = false) String comment,
                          RedirectAttributes flash) {
        service.approve(id, CurrentUser.staffId(), comment);
        flash.addFlashAttribute("message", "Plan approved.");
        return "redirect:/lessons";
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyAuthority(T(zw.co.cresolzim.schoolms.domain.Role).ADMIN, " +
                  "T(zw.co.cresolzim.schoolms.domain.Role).HEAD)")
    public String sendBack(@PathVariable Long id, @RequestParam String comment,
                           RedirectAttributes flash) {
        service.returnForRevision(id, CurrentUser.staffId(), comment);
        flash.addFlashAttribute("message", "Plan returned to the teacher.");
        return "redirect:/lessons";
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAnyAuthority(T(zw.co.cresolzim.schoolms.domain.Role).ADMIN, " +
                  "T(zw.co.cresolzim.schoolms.domain.Role).HEAD)")
    public String reopen(@PathVariable Long id, @RequestParam String reason,
                         RedirectAttributes flash) {
        service.reopen(id, CurrentUser.staffId(), reason);
        flash.addFlashAttribute("message", "Plan reopened for revision.");
        return "redirect:/lessons/" + id;
    }

    /* ------------------------------------------------------------ helpers */

    private void requireTeacher() {
        if (CurrentUser.staffId() == null) {
            throw new RuleViolationException(
                    "Only a member of staff can write lesson plans. This account is not linked to a staff record.");
        }
    }

    private boolean isOwner(LessonPlan plan) {
        Long me = CurrentUser.staffId();
        return me != null && plan.getTeacher().getId().equals(me);
    }

    private void requireOwnerOrReviewer(LessonPlan plan) {
        if (!isOwner(plan) && !CurrentUser.hasAnyRole(Role.ADMIN, Role.HEAD)) {
            throw new RuleViolationException("You may only open your own lesson plans.");
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
