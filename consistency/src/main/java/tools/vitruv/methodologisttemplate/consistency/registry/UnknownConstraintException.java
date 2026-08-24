package tools.vitruv.methodologisttemplate.consistency.registry;

import java.util.Set;

/**
 * Thrown when {@link ReactionConstraintRegistry} (via {@code ProjectReactionConstraints}) points
 * at a {@link ConstraintRef} that does not correspond to any constraint actually declared in the
 * evaluated {@code .ocl} file(s) -- almost always a typo in the {@code (contextType,
 * constraintName)} pair, or a constraint that was renamed/removed from the {@code .ocl} file
 * without updating the registry. Registering such a Reference would otherwise silently never be
 * checked, since it can never appear in {@link VitruvOCLGateway#evaluateAll()}'s result.
 */
public final class UnknownConstraintException extends IllegalStateException {

  public UnknownConstraintException(Set<ConstraintRef> unknownRefs, Set<ConstraintRef> declaredRefs) {
    super(buildMessage(unknownRefs, declaredRefs));
  }

  private static String buildMessage(Set<ConstraintRef> unknownRefs, Set<ConstraintRef> declaredRefs) {
    return "ReactionConstraintRegistry references constraint(s) that do not exist in the "
        + "evaluated .ocl file(s): "
        + unknownRefs
        + ". Check for typos in ProjectReactionConstraints.buildRegistry(), or verify the "
        + "constraint is declared with this exact (contextType, constraintName) -- contextType "
        + "must include the namespace prefix (e.g. \"model2::Link\", not \"Link\"). Constraints "
        + "actually declared in the evaluated file(s): "
        + declaredRefs;
  }
}
