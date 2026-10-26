package tools.vitruv.methodologisttemplate.consistency.registry;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.dsls.reactions.runtime.reactions.Reaction;
import tools.vitruv.methodologisttemplate.consistency.registry.VitruvOCLGateway.DeclaredConstraint;

/**
 * Evaluates the contract of a single Reaction execution -- the pre- and postconditions that
 * {@link ReactionConstraintRegistry} assigns to the Reaction -- and, after a commit, all invariants.
 *
 * <p>Only the constraints assigned to the Reaction are evaluated, not every constraint in the
 * evaluated files. Contracts are evaluated exactly like invariants: {@code self} ranges over all
 * instances of the context type, and other elements are reached through fully qualified navigation
 * and the correspondence operator {@code ~}.
 */
public final class ConstraintEvaluationCoordinator {

  private final ReactionConstraintRegistry registry;
  private final VitruvOCLGateway ocl;
  private Map<ConstraintRef, DeclaredConstraint> declared;

  public ConstraintEvaluationCoordinator(ReactionConstraintRegistry registry, VitruvOCLGateway ocl) {
    this.registry = registry;
    this.ocl = ocl;
  }

  /** Whether {@code reaction} has a contract at all, i.e. whether its executions are checked. */
  public boolean hasContract(Class<? extends Reaction> reaction) {
    return !registry.getConstraintsFor(reaction).isEmpty();
  }

  /** Whether {@code reaction} has postconditions, i.e. whether its changes must be recorded. */
  public boolean hasPostconditions(Class<? extends Reaction> reaction) {
    return !ofKind(reaction, ConstraintKind.POSTCONDITION).isEmpty();
  }

  /**
   * Evaluates the preconditions of {@code reaction} against the current state, right before the
   * Reaction runs. They are evaluated with an empty transaction ({@code @pre} and the lifecycle
   * predicates are not allowed in a precondition anyway).
   *
   * @throws UnknownConstraintException if the registry references a constraint that no evaluated
   *     file declares -- fails loudly instead of silently never checking it
   */
  public List<ViolatedConstraint> evaluatePreconditionsFor(Class<? extends Reaction> reaction) {
    return evaluate(ofKind(reaction, ConstraintKind.PRECONDITION), List.of());
  }

  /**
   * Evaluates the postconditions of {@code reaction} right after one of its executions, against
   * exactly the changes this execution made: {@code @pre} reads the state right before the
   * Reaction, {@code OCLisNew}/{@code OCLisModified}/{@code OCLisDeleted} refer to what it created,
   * changed or deleted.
   *
   * @throws UnknownConstraintException see {@link #evaluatePreconditionsFor(Class)}
   */
  public List<ViolatedConstraint> evaluatePostconditionsFor(
      Class<? extends Reaction> reaction, List<EChange<EObject>> reactionChanges) {
    return evaluate(ofKind(reaction, ConstraintKind.POSTCONDITION), reactionChanges);
  }

  /** Evaluates every invariant of the evaluated files, after a commit. */
  public List<ViolatedConstraint> evaluateInvariants() {
    return ocl.evaluateInvariants().stream()
        .filter(c -> !c.satisfied())
        .map(ConstraintEvaluationCoordinator::toViolation)
        .toList();
  }

  private List<DeclaredConstraint> ofKind(Class<? extends Reaction> reaction, ConstraintKind kind) {
    return registry.getConstraintsFor(reaction).stream()
        .map(this::declaration)
        .filter(c -> c.kind() == kind)
        .toList();
  }

  private List<ViolatedConstraint> evaluate(
      List<DeclaredConstraint> constraints, List<EChange<EObject>> transaction) {
    return constraints.stream()
        .map(c -> ocl.evaluate(c, transaction))
        .filter(c -> !c.satisfied())
        .map(ConstraintEvaluationCoordinator::toViolation)
        .toList();
  }

  private static ViolatedConstraint toViolation(EvaluatedConstraint c) {
    return new ViolatedConstraint(c.contextType(), c.constraintName(), c.kind(), c.message());
  }

  private synchronized DeclaredConstraint declaration(ConstraintRef ref) {
    if (declared == null) {
      Map<ConstraintRef, DeclaredConstraint> byRef = new LinkedHashMap<>();
      ocl.declarations().forEach(c -> byRef.put(c.ref(), c));
      Set<ConstraintRef> unknown = new LinkedHashSet<>();
      registry.allConstraints().stream().filter(r -> !byRef.containsKey(r)).forEach(unknown::add);
      if (!unknown.isEmpty()) {
        throw new UnknownConstraintException(unknown, byRef.keySet());
      }
      declared = byRef;
    }
    return declared.get(ref);
  }
}
