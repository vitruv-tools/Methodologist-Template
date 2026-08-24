package tools.vitruv.methodologisttemplate.consistency.registry;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
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
   * Evaluates with no transaction context at all -- {@code pre}/{@code post} constraints among
   * {@code firedReactions}' registered set are skipped outright (see {@link
   * VitruvOCLGateway#evaluateAll()}), not genuinely checked. Prefer {@link #evaluateFor(Set,
   * List)} whenever a real transaction is available.
   *
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
    return filterViolations(relevant, ocl.evaluateAll());
  }

  /**
   * Evaluates the constraints registered for {@code firedReactions} against a real transaction --
   * unlike {@link #evaluateFor(Set)}, {@code pre}/{@code post} constraints are genuinely checked,
   * not skipped. This is what {@link ReactionConstraintCheckingListener} calls after every commit,
   * passing the transaction it observed via {@code finishedChangePropagation}.
   *
   * @param transaction the ordered list of atomic changes between pre-state and the current
   *     post-state, evaluated by {@code @pre}/{@code OCLisNew}/{@code OCLisModified}/{@code
   *     OCLisDeleted} (may be empty)
   * @throws UnknownConstraintException see {@link #evaluateFor(Set)}
   */
  public List<ViolatedConstraint> evaluateFor(
      Set<Class<? extends Reaction>> firedReactions, List<EChange<EObject>> transaction) {
    Set<ConstraintRef> relevant = registry.getConstraintsForAll(firedReactions);
    if (relevant.isEmpty()) {
      return List.of();
    }
    return filterViolations(relevant, ocl.evaluateAll(transaction));
  }

  private List<ViolatedConstraint> filterViolations(
      Set<ConstraintRef> relevant, List<EvaluatedConstraint> all) {
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
        .map(c -> new ViolatedConstraint(c.contextType(), c.constraintName(), c.kind(), c.message()))
        .collect(Collectors.toList());
  }
}
