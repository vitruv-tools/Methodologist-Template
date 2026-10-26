package tools.vitruv.methodologisttemplate.consistency.registry;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Thrown when the user rejects a violated constraint: a precondition or postcondition of a
 * Reaction (thrown by {@link ContractEnforcer} from inside the Reaction execution, which stops the
 * whole commit), or an invariant checked after the commit (by {@code ContractGuardedVsum}). In
 * both cases the commit is not kept.
 *
 * <p>Extends {@link AssertionError} (not a plain {@link RuntimeException}) so JUnit -- and any
 * other test runner -- reports it as a failed assertion, the same category as a hand-written
 * {@code assertTrue(...)}: this commit broke a documented contract or invariant, not "something
 * crashed unexpectedly".
 *
 * <p>The message names each violated {@link ConstraintKind} explicitly (e.g. "1 postcondition(s)
 * violated", not a generic "1 constraint(s) violated") and, per violation, prints only {@link
 * ViolatedConstraint#message()} -- the diagnostic VitruvOCL itself already produced (severity,
 * the concrete object, the interpolated message) -- rather than a raw {@code
 * ViolatedConstraint[contextType=..., constraintName=..., ...]} field dump that would just repeat
 * what that diagnostic already states.
 */
public final class ConstraintViolationsDetectedException extends AssertionError {

  private final transient List<ViolatedConstraint> violations;

  public ConstraintViolationsDetectedException(List<ViolatedConstraint> violations) {
    super(buildMessage(violations));
    this.violations = List.copyOf(violations);
  }

  /** The violations the user rejected. */
  public List<ViolatedConstraint> violations() {
    return violations;
  }

  private static String buildMessage(List<ViolatedConstraint> violations) {
    String verb;
    if (allOfKind(violations, ConstraintKind.PRECONDITION)) {
      // Thrown before the Reaction's own execute body has run.
      verb = " violated -- the Reaction was not executed and the commit was stopped:\n";
    } else if (allOfKind(violations, ConstraintKind.POSTCONDITION)) {
      verb = " violated by the Reaction that just ran -- its result was rejected and the commit"
          + " was stopped:\n";
    } else if (allOfKind(violations, ConstraintKind.INVARIANT)) {
      verb = " violated after the commit -- the commit was rolled back:\n";
    } else {
      verb = " violated -- the commit was stopped:\n";
    }
    return summarizeByKind(violations)
        + verb
        + violations.stream().map(ViolatedConstraint::message).collect(Collectors.joining("\n"));
  }

  private static boolean allOfKind(List<ViolatedConstraint> violations, ConstraintKind kind) {
    return violations.stream().allMatch(v -> v.kind() == kind);
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
