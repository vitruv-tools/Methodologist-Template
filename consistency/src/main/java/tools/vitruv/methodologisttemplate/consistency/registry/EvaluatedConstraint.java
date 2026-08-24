package tools.vitruv.methodologisttemplate.consistency.registry;

/**
 * One constraint declaration as reported by VitruvOCL's evaluation result, regardless of whether
 * it was satisfied. Unlike {@link ViolatedConstraint} (which only ever represents a violation),
 * this also carries the constraints that passed -- needed so {@link ConstraintEvaluationCoordinator}
 * can tell "registered but satisfied" apart from "registered but never declared in any evaluated
 * .ocl file at all", the latter being a {@link UnknownConstraintException}-worthy configuration
 * error rather than a real violation.
 */
public record EvaluatedConstraint(String contextType, String constraintName, boolean satisfied, String message) {
  ConstraintRef ref() {
    return new ConstraintRef(contextType, constraintName);
  }
}
