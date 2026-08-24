package tools.vitruv.methodologisttemplate.consistency.registry;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Thrown by {@link ReactionConstraintCheckingListener#failFast} when a Reaction fires and one or
 * more of its registered constraints turn out to be violated.
 *
 * <p>Extends {@link AssertionError} (not a plain {@link RuntimeException}) so JUnit -- and any
 * other test runner -- reports it as a failed assertion, the same category as a hand-written
 * {@code assertTrue(...)}: this commit broke a documented invariant, not "something crashed
 * unexpectedly". It propagates straight out of {@code commitChanges()} uncaught (Vitruvius does
 * not wrap or swallow exceptions thrown from a {@code ChangePropagationListener}), so a test using
 * {@link ReactionConstraintCheckingListener#failFast} goes red exactly at the commit that
 * triggered the violation.
 *
 * <p>The message names each violated {@link ConstraintKind} explicitly (e.g. "1 postcondition(s)
 * violated", not a generic "1 constraint(s) violated") and, per violation, prints only {@link
 * ViolatedConstraint#message()} -- the diagnostic VitruvOCL itself already produced (severity,
 * the concrete object, the interpolated message) -- rather than a raw {@code
 * ViolatedConstraint[contextType=..., constraintName=..., ...]} field dump that would just repeat
 * what that diagnostic already states.
 */
public final class ConstraintViolationsDetectedException extends AssertionError {

  public ConstraintViolationsDetectedException(List<ViolatedConstraint> violations) {
    super(buildMessage(violations));
  }

  private static String buildMessage(List<ViolatedConstraint> violations) {
    return summarizeByKind(violations)
        + " violated by the Reaction(s) that just fired:\n"
        + violations.stream().map(ViolatedConstraint::message).collect(Collectors.joining("\n"));
  }

  private static String summarizeByKind(List<ViolatedConstraint> violations) {
    Map<ConstraintKind, Long> countByKind =
        violations.stream()
            .collect(
                Collectors.groupingBy(
                    ViolatedConstraint::kind,
                    () -> new EnumMap<>(ConstraintKind.class),
                    Collectors.counting()));
    return countByKind.entrySet().stream()
        .map(entry -> entry.getValue() + " " + entry.getKey().label() + "(s)")
        .collect(Collectors.joining(" and "));
  }
}
