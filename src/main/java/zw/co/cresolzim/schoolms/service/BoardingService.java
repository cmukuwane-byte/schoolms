package zw.co.cresolzim.schoolms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.Residency;
import zw.co.cresolzim.schoolms.repo.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardingService {

    private final BoardingAllocationRepository allocations;
    private final BoardingExeatRepository exeats;
    private final DormitoryRepository dormitories;
    private final StudentRepository students;
    private final TermRepository terms;

    /**
     * Puts a boarder in a bed. Checks the learner is actually a boarder, that the
     * hostel matches their sex, and that the dormitory has room.
     */
    @Transactional
    public BoardingAllocation allocate(Long studentId, Long dormitoryId, String bedNumber, String actor) {
        Student s = students.findById(studentId).orElseThrow(() -> new NotFoundException("Student", studentId));
        Dormitory d = dormitories.findById(dormitoryId)
                .orElseThrow(() -> new NotFoundException("Dormitory", dormitoryId));
        Term term = terms.findByCurrentTrue()
                .orElseThrow(() -> new RuleViolationException("No term is marked current."));

        if (s.getResidency() != Residency.BOARDER) {
            throw new RuleViolationException(
                    s.getFullName() + " is registered as a day scholar. Change their residency first.");
        }
        if (d.getHostel().getGender() != s.getGender()) {
            throw new RuleViolationException(
                    d.getHostel().getName() + " is a " + d.getHostel().getGender() + " hostel.");
        }
        long occupied = allocations.countByDormitoryIdAndActiveTrue(dormitoryId);
        if (occupied >= d.getBedCapacity()) {
            throw new RuleViolationException(
                    d.getName() + " is full (" + occupied + " of " + d.getBedCapacity() + " beds).");
        }

        allocations.findByStudentIdAndActiveTrue(studentId).ifPresent(existing -> {
            existing.setActive(false);
            existing.setVacatedOn(LocalDate.now());
            allocations.save(existing);
        });

        BoardingAllocation a = new BoardingAllocation();
        a.setStudent(s);
        a.setDormitory(d);
        a.setTerm(term);
        a.setBedNumber(bedNumber);
        a.setAllocatedOn(LocalDate.now());
        a.setCreatedBy(actor);
        return allocations.save(a);
    }

    @Transactional
    public void vacate(Long studentId) {
        allocations.findByStudentIdAndActiveTrue(studentId).ifPresent(a -> {
            a.setActive(false);
            a.setVacatedOn(LocalDate.now());
            allocations.save(a);
        });
    }

    /** Signs a boarder out of the hostel. */
    @Transactional
    public BoardingExeat grantExeat(Long studentId, BoardingExeat e, String actor) {
        Student s = students.findById(studentId).orElseThrow(() -> new NotFoundException("Student", studentId));
        if (e.getExpectedReturn().isBefore(e.getDepartureTime())) {
            throw new RuleViolationException("The expected return cannot be before the departure time.");
        }
        e.setStudent(s);
        e.setAuthorisedBy(actor);
        e.setCreatedBy(actor);
        return exeats.save(e);
    }

    @Transactional
    public BoardingExeat recordReturn(Long exeatId) {
        BoardingExeat e = exeats.findById(exeatId).orElseThrow(() -> new NotFoundException("Exeat", exeatId));
        e.setActualReturn(LocalDateTime.now());
        return exeats.save(e);
    }

    /** Boarders who are signed out and past their return time. */
    @Transactional(readOnly = true)
    public List<BoardingExeat> outstanding() {
        return exeats.findByActualReturnIsNullOrderByExpectedReturnAsc();
    }

    @Transactional(readOnly = true)
    public List<Object[]> occupancy() { return allocations.occupancyByDormitory(); }
}
