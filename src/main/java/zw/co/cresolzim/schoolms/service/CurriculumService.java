package zw.co.cresolzim.schoolms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.repo.*;

import java.util.*;

/**
 * The scheme of work. The head owns this; teachers read it.
 */
@Service
@RequiredArgsConstructor
public class CurriculumService {

    private final CurriculumTopicRepository topics;
    private final SubjectRepository subjects;

    @Transactional(readOnly = true)
    public List<CurriculumTopic> forSubjectAndGrade(Long subjectId, Integer gradeLevel) {
        return topics.findBySubjectIdAndGradeLevelAndActiveTrueOrderByTermNumberAscWeekNumberAsc(
                subjectId, gradeLevel);
    }

    /** Grouped by term, so the page can show three blocks rather than one long list. */
    @Transactional(readOnly = true)
    public Map<Integer, List<CurriculumTopic>> byTerm(Long subjectId, Integer gradeLevel) {
        Map<Integer, List<CurriculumTopic>> out = new LinkedHashMap<>();
        for (int t = 1; t <= 3; t++) out.put(t, new ArrayList<>());
        for (CurriculumTopic c : forSubjectAndGrade(subjectId, gradeLevel)) {
            out.computeIfAbsent(c.getTermNumber(), k -> new ArrayList<>()).add(c);
        }
        return out;
    }

    @Transactional
    public CurriculumTopic save(CurriculumTopic topic, String actor) {
        Subject s = subjects.findById(topic.getSubject().getId())
                .orElseThrow(() -> new NotFoundException("Subject", topic.getSubject().getId()));

        if (topic.getPhase() == null) topic.setPhase(s.getPhase());
        if (topic.getPhase() != s.getPhase()) {
            throw new RuleViolationException(
                    s.getName() + " is a " + s.getPhase() + " subject, so it cannot be scheduled "
                    + "against a " + topic.getPhase() + " grade.");
        }
        if (topic.getWeekNumber() == null || topic.getWeekNumber() < 1 || topic.getWeekNumber() > 15) {
            throw new RuleViolationException("Week number must be between 1 and 15.");
        }
        if (topic.getTermNumber() == null || topic.getTermNumber() < 1 || topic.getTermNumber() > 3) {
            throw new RuleViolationException("Term must be 1, 2 or 3.");
        }

        // One topic per subject, grade, term and week — otherwise two teachers of
        // parallel streams can end up following different schemes.
        topics.findBySubjectIdAndGradeLevelAndTermNumberAndWeekNumber(
                        s.getId(), topic.getGradeLevel(), topic.getTermNumber(), topic.getWeekNumber())
              .filter(existing -> !existing.getId().equals(topic.getId()))
              .ifPresent(existing -> {
                  throw new RuleViolationException(
                          "Week " + topic.getWeekNumber() + " of term " + topic.getTermNumber()
                          + " already has a topic for this subject and grade: " + existing.getTopic());
              });

        if (topic.getCreatedBy() == null) topic.setCreatedBy(actor);
        return topics.save(topic);
    }

    @Transactional
    public void retire(Long id) {
        CurriculumTopic c = topics.findById(id).orElseThrow(() -> new NotFoundException("Topic", id));
        c.setActive(false);
        topics.save(c);
    }

    /**
     * Weeks of the scheme with no lesson plan against them for a class.
     * This is the question a head actually asks: is the syllabus being covered?
     */
    @Transactional(readOnly = true)
    public List<CurriculumTopic> gaps(Long subjectId, Integer gradeLevel, Integer termNumber, Long classId) {
        return topics.uncoveredWeeks(subjectId, gradeLevel, termNumber, classId);
    }

    /** Copies a whole subject's scheme from one grade to another as a starting point. */
    @Transactional
    public int copyToGrade(Long subjectId, Integer fromGrade, Integer toGrade, String actor) {
        if (Objects.equals(fromGrade, toGrade)) {
            throw new RuleViolationException("Choose a different grade to copy into.");
        }
        if (topics.countBySubjectIdAndGradeLevelAndActiveTrue(subjectId, toGrade) > 0) {
            throw new RuleViolationException(
                    "That grade already has a scheme for this subject. Retire it first if you mean to replace it.");
        }

        List<CurriculumTopic> source = forSubjectAndGrade(subjectId, fromGrade);
        List<CurriculumTopic> copies = new ArrayList<>();
        for (CurriculumTopic src : source) {
            CurriculumTopic c = new CurriculumTopic();
            c.setSubject(src.getSubject());
            c.setPhase(src.getPhase());
            c.setGradeLevel(toGrade);
            c.setTermNumber(src.getTermNumber());
            c.setWeekNumber(src.getWeekNumber());
            c.setTopic(src.getTopic());
            c.setSubTopic(src.getSubTopic());
            c.setObjectives(src.getObjectives());
            c.setCompetencies(src.getCompetencies());
            c.setSuggestedActivities(src.getSuggestedActivities());
            c.setResources(src.getResources());
            c.setSuggestedPeriods(src.getSuggestedPeriods());
            c.setSyllabusReference(src.getSyllabusReference());
            c.setCreatedBy(actor);
            copies.add(c);
        }
        topics.saveAll(copies);
        return copies.size();
    }
}
