package zw.co.cresolzim.schoolms.service;

/** Thrown when an action is blocked by a school rule rather than a technical fault. */
public class RuleViolationException extends RuntimeException {
    public RuleViolationException(String message) { super(message); }
}
