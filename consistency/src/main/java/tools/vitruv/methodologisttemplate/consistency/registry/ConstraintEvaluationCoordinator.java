package tools.vitruv.methodologisttemplate.consistency.registry;

import java.util.LinkedHashSet;
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

  /**
   * @throws UnknownConstraintException if the registry references a {@link ConstraintRef} that
   *     is not declared in any file {@link #ocl} evaluates -- fails loudly instead of silently
   *     never checking it, which is what would otherwise happen with e.g. a typo'd constraint
   *     name in {@code ProjectReactionConstraints.buildRegistry()}.
   */
  public List<ViolatedConstraint> evaluateFor(Set<Class<? extends Reaction>> firedReactions) {
    Set<ConstraintRef> relevant = registry.getConstraintsForAll(firedReactions);
    if (relevant.isEmpty()) {
      return List.of();
    }

    List<EvaluatedConstraint> all = ocl.evaluateAll();
    Set<ConstraintRef> declared = all.stream().map(EvaluatedConstraint::ref).collect(Collectors.toSet());
    Set<ConstraintRef> unknown =
        relevant.stream()
            .filter(ref -> !declared.contains(ref))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (!unknown.isEmpty()) {
      throw new UnknownConstraintException(unknown, declared);
    }

    return all.stream()
        .filter(c -> relevant.contains(c.ref()) && !c.satisfied())
        .map(c -> new ViolatedConstraint(c.contextType(), c.constraintName(), c.message()))
        .collect(Collectors.toList());
  }
}
