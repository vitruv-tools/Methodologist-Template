package tools.vitruv.methodologisttemplate.consistency.registry;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.interaction.UserInteractionOptions.WindowModality;
import tools.vitruv.change.interaction.UserInteractor;
import tools.vitruv.dsls.reactions.runtime.reactions.Reaction;

/**
 * Lets {@link HookedReaction} enforce a Reaction's contract on each of its executions.
 *
 * <p>If a precondition (before the Reaction) or a postcondition (after it) is violated, the user is
 * asked whether to continue anyway. On <em>yes</em>, the decision is logged and propagation goes
 * on. On <em>no</em>, it is logged and a {@link ConstraintViolationsDetectedException} is thrown
 * from inside the Reaction execution, which stops the whole commit. Vitruvius saves the models only
 * after change propagation has finished, so nothing of the stopped commit is persisted and the
 * state before the commit can be restored by reloading the VSUM (see {@code ContractGuardedVsum}
 * in the {@code vsum} module). Every violation is decided by the user, whatever its {@code
 * @severity}; the severity is part of the diagnostic shown.
 *
 * <p>Bound after construction (see {@link #bind}) rather than passed in directly, because {@link
 * ConstraintEvaluationCoordinator} cannot exist yet when {@link
 * HookedModel2Model2ChangePropagationSpecification#setup()} wraps every Reaction in a {@link
 * HookedReaction} -- the coordinator needs the VSUM to already be built. Until it is bound,
 * Reactions run unchecked.
 *
 * <p><b>Callers inspecting the thrown exception, note:</b> Vitruvius's {@code ChangePropagator}
 * wraps whatever propagates out of {@code Reaction.execute()} in a plain {@link RuntimeException},
 * so the {@link ConstraintViolationsDetectedException} arrives at the caller of {@code
 * commitChanges()} as that wrapper's {@link Throwable#getCause() cause}.
 */
public final class ContractEnforcer {

  private ConstraintEvaluationCoordinator coordinator;
  private ContractLog log;
  private Supplier<ResourceSet> modelResourceSet = () -> null;

  /**
   * Wires the coordinator and the log in -- call this once, right after building the VSUM.
   *
   * @param modelResourceSet the ResourceSet of the VSUM's models, in which the changes of a
   *     Reaction execution are recorded; needed when the triggering change itself is no longer
   *     attached to it, e.g. the deletion of an element
   */
  public void bind(
      ConstraintEvaluationCoordinator coordinator,
      ContractLog log,
      Supplier<ResourceSet> modelResourceSet) {
    this.coordinator = coordinator;
    this.log = log;
    this.modelResourceSet = modelResourceSet;
  }

  ResourceSet modelResourceSet() {
    return modelResourceSet.get();
  }

  boolean appliesTo(Class<? extends Reaction> reaction) {
    return coordinator != null && coordinator.hasContract(reaction);
  }

  boolean hasPostconditions(Class<? extends Reaction> reaction) {
    return coordinator.hasPostconditions(reaction);
  }

  void checkBeforeExecuting(
      Class<? extends Reaction> reaction, String reactionName, UserInteractor userInteractor) {
    enforce(
        reactionName,
        ConstraintKind.PRECONDITION,
        coordinator.evaluatePreconditionsFor(reaction),
        userInteractor);
  }

  void checkAfterExecuting(
      Class<? extends Reaction> reaction,
      String reactionName,
      List<EChange<EObject>> reactionChanges,
      UserInteractor userInteractor) {
    enforce(
        reactionName,
        ConstraintKind.POSTCONDITION,
        coordinator.evaluatePostconditionsFor(reaction, reactionChanges),
        userInteractor);
  }

  private void enforce(
      String reactionName,
      ConstraintKind kind,
      List<ViolatedConstraint> violations,
      UserInteractor userInteractor) {
    if (violations.isEmpty()) {
      return;
    }
    boolean approved = askUser(kind, reactionName, violations, userInteractor);
    violations.forEach(v -> log.record(v, approved));
    if (!approved) {
      throw new ConstraintViolationsDetectedException(violations);
    }
  }

  private static boolean askUser(
      ConstraintKind kind,
      String reactionName,
      List<ViolatedConstraint> violations,
      UserInteractor userInteractor) {
    String question =
        kind == ConstraintKind.PRECONDITION
            ? "The precondition of Reaction " + reactionName + " is violated. Execute it anyway?"
            : "The postcondition of Reaction " + reactionName + " is violated. Keep its result?";
    String details =
        violations.stream().map(ViolatedConstraint::message).collect(Collectors.joining("\n"));
    return userInteractor
        .getConfirmationDialogBuilder()
        .message(question + "\n" + details + "\nNo stops the commit and restores the models.")
        .windowModality(WindowModality.MODAL)
        .startInteraction();
  }
}
