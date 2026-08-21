package tools.vitruv.methodologisttemplate.consistency.registry;

import java.util.Objects;

/**
 * Identifies exactly one OCL# pre-/postcondition constraint declared in a {@code .ocl} file,
 * as written under {@code context <namespace>::<Type> inv <constraintName>: ...}.
 *
 * <p>The pair (contextType, constraintName) is the key, not the constraint name alone: {@code
 * constraints.ocl} declares both {@code model::Link} and {@code model2::Link}, so contextType
 * must carry the namespace prefix to stay unambiguous.
 */
public final class ConstraintRef {

  private final String contextType;
  private final String constraintName;

  public ConstraintRef(String contextType, String constraintName) {
    Objects.requireNonNull(contextType, "contextType must not be null");
    Objects.requireNonNull(constraintName, "constraintName must not be null");
    if (contextType.isBlank()) {
      throw new IllegalArgumentException("contextType must not be blank");
    }
    if (constraintName.isBlank()) {
      throw new IllegalArgumentException("constraintName must not be blank");
    }
    this.contextType = contextType;
    this.constraintName = constraintName;
  }

  public String contextType() {
    return contextType;
  }

  public String constraintName() {
    return constraintName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ConstraintRef other)) return false;
    return contextType.equals(other.contextType) && constraintName.equals(other.constraintName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contextType, constraintName);
  }

  @Override
  public String toString() {
    return contextType + "::" + constraintName;
  }
}
