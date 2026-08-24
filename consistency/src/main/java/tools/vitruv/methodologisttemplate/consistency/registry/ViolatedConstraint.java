package tools.vitruv.methodologisttemplate.consistency.registry;

/** One violated or failed constraint, as reported by VitruvOCL's evaluation result. */
public record ViolatedConstraint(String contextType, String constraintName, ConstraintKind kind, String message) {
  ConstraintRef ref() {
    return new ConstraintRef(contextType, constraintName);
  }
}
