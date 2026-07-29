package zw.co.cresolzim.schoolms.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import zw.co.cresolzim.schoolms.repo.*;
import zw.co.cresolzim.schoolms.service.BoardingService;
import zw.co.cresolzim.schoolms.service.RuleViolationException;

@Controller
@RequestMapping("/boarding")
@RequiredArgsConstructor
public class BoardingController {

    private final BoardingService service;
    private final HostelRepository hostels;
    private final DormitoryRepository dormitories;
    private final StaffRepository staffRepo;
    private final BoardingAllocationRepository allocations;
    private final StudentRepository students;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("hostels", hostels.findByActiveTrueOrderByNameAsc());
        model.addAttribute("occupancy", service.occupancy());
        model.addAttribute("outstandingExeats", service.outstanding());
        model.addAttribute("boarderCount",
                students.countByResidencyAndStatus(Residency.BOARDER, StudentStatus.ENROLLED));
        return "boarding/index";
    }

    @GetMapping("/dormitory/{id}")
    public String dormitory(@PathVariable Long id, Model model) {
        model.addAttribute("dormitory", dormitories.findById(id).orElseThrow());
        model.addAttribute("residents", allocations.findByDormitoryIdAndActiveTrue(id));
        return "boarding/dormitory";
    }

    @PostMapping("/allocate")
    public String allocate(@RequestParam Long studentId, @RequestParam Long dormitoryId,
                           @RequestParam(required = false) String bedNumber, RedirectAttributes flash) {
        service.allocate(studentId, dormitoryId, bedNumber, CurrentUser.name());
        flash.addFlashAttribute("message", "Bed allocated.");
        return "redirect:/boarding/dormitory/" + dormitoryId;
    }

    @PostMapping("/vacate")
    public String vacate(@RequestParam Long studentId, @RequestParam Long dormitoryId,
                         RedirectAttributes flash) {
        service.vacate(studentId);
        flash.addFlashAttribute("message", "Bed released.");
        return "redirect:/boarding/dormitory/" + dormitoryId;
    }

    @PostMapping("/exeat")
    public String exeat(@RequestParam Long studentId, @ModelAttribute BoardingExeat exeat,
                        RedirectAttributes flash) {
        service.grantExeat(studentId, exeat, CurrentUser.name());
        flash.addFlashAttribute("message", "Exeat granted.");
        return "redirect:/boarding";
    }

    @PostMapping("/exeat/{id}/return")
    public String exeatReturn(@PathVariable Long id, RedirectAttributes flash) {
        service.recordReturn(id);
        flash.addFlashAttribute("message", "Return logged.");
        return "redirect:/boarding";
    }

    /* --------------------------------------------------- hostels and dorms */

    /** Setting up the estate: hostels first, then dormitories inside them. */
    @GetMapping("/setup")
    public String setup(Model model) {
        model.addAttribute("hostels", hostels.findAllByOrderByNameAsc());
        model.addAttribute("dormitories", dormitories.findAllByOrderByHostelNameAscNameAsc());
        model.addAttribute("hostel", new Hostel());
        model.addAttribute("dormitory", new Dormitory());
        model.addAttribute("staffList", staffRepo.findByEmploymentStatusOrderBySurnameAsc(EmploymentStatus.ACTIVE));
        model.addAttribute("genders", Gender.values());
        model.addAttribute("phases", Phase.values());
        return "boarding/setup";
    }

    @PostMapping("/hostels")
    public String saveHostel(@ModelAttribute Hostel hostel, RedirectAttributes flash) {
        hostels.save(hostel);
        flash.addFlashAttribute("message", hostel.getName() + " saved.");
        return "redirect:/boarding/setup";
    }

    @PostMapping("/dormitories")
    public String saveDormitory(@ModelAttribute Dormitory dormitory, RedirectAttributes flash) {
        if (dormitory.getHostel() == null) {
            throw new RuleViolationException("Choose which hostel the dormitory belongs to.");
        }
        dormitories.save(dormitory);
        flash.addFlashAttribute("message", dormitory.getName() + " saved.");
        return "redirect:/boarding/setup";
    }

    @PostMapping("/dormitories/{id}/retire")
    public String retireDormitory(@PathVariable Long id, RedirectAttributes flash) {
        dormitories.findById(id).ifPresent(d -> {
            if (allocations.countByDormitoryIdAndActiveTrue(id) > 0) {
                throw new RuleViolationException(
                        "Move the boarders out of " + d.getName() + " before retiring it.");
            }
            d.setActive(false);
            dormitories.save(d);
        });
        flash.addFlashAttribute("message", "Dormitory retired.");
        return "redirect:/boarding/setup";
    }
}
