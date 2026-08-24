package tools.vitruv.methodologisttemplate.consistency.registry;

import java.util.List;
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
 * triggered the violation, with every violated constraint listed in the failure message.
 */
public final class ConstraintViolationsDetectedException extends AssertionError {

  public ConstraintViolationsDetectedException(List<ViolatedConstraint> violations) {
    super(buildMessage(violations));
  }

  private static String buildMessage(List<ViolatedConstraint> violations) {
    return violations.size()
        + " constraint(s) violated by the Reaction(s) that just fired:\n"
        + violations.stream().map(Object::toString).collect(Collectors.joining("\n"));
  }
}
