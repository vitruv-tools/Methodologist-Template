package tools.vitruv.methodologisttemplate.consistency.registry;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import tools.vitruv.dsls.reactions.runtime.reactions.Reaction;

/**
 * Given the set of Reactions that fired in a transaction, looks up the relevant constraint set
 * via {@link ReactionConstraintRegistry} and filters VitruvOCL's (whole-file) evaluation result
 * down to just those constraints.
 */
public final class ConstraintEvaluationCoordinator {

  private final ReactionConstraintRegistry registry;
  private final VitruvOCLGateway ocl;

  public ConstraintEvaluationCoordinator(ReactionConstraintRegistry registry, VitruvOCLGateway ocl) {
    this.registry = registry;
    this.ocl = ocl;
  }

  public List<ViolatedConstraint> evaluateFor(Set<Class<? extends Reaction>> firedReactions) {
    Set<ConstraintRef> relevant = registry.getConstraintsForAll(firedReactions);
    if (relevant.isEmpty()) {
      return List.of();
    }
    return ocl.evaluateAll().stream()
        .filter(v -> relevant.contains(v.ref()))
        .collect(Collectors.toList());
  }
}
