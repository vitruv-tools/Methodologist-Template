package tools.vitruv.methodologisttemplate.consistency.registry;

import java.util.List;
import tools.vitruv.dsls.reactions.runtime.reactions.Reaction;

/**
 * Lets {@link HookedReaction} check a Reaction's registered preconditions immediately before
 * executing it, and abort the Reaction entirely -- its generated {@code execute} body is never
 * called, so the model is never mutated -- if any precondition is violated.
 *
 * <p>Bound after construction (see {@link #bind}) rather than passed in directly, because {@link
 * ConstraintEvaluationCoordinator} cannot exist yet when {@link
 * HookedModel2Model2ChangePropagationSpecification#setup()} wraps every Reaction in a {@link
 * HookedReaction} -- the coordinator needs the VSUM to already be built. By the time any Reaction
 * genuinely fires (a real commit, as opposed to VSUM construction itself), {@link #bind} has
 * always already been called -- mirroring how {@link FiredReactionsCollector} is exposed via
 * {@code getCollector()} for the same reason.
 *
 * <p><b>Callers inspecting the thrown exception, note:</b> unlike a postcondition/invariant
 * violation thrown from {@link ReactionConstraintCheckingListener#failFast} (a {@code
 * ChangePropagationListener}, whose exceptions Vitruvius propagates unwrapped out of {@code
 * commitChanges()}), {@link #checkBeforeExecuting} throws from inside {@code Reaction.execute()}
 * itself -- Vitruvius's {@code ChangePropagator} wraps whatever propagates out of there in a
 * plain {@link RuntimeException}. A {@link ConstraintViolationsDetectedException} thrown here
 * therefore arrives at the caller as that wrapper's {@link Throwable#getCause() cause}, not as
 * the exception thrown directly -- confirmed in {@code
 * VSUMExampleTest#componentWithBlankNameNeverTriggersTheReactionAtAll}.
 */
public final class PreconditionGuard {

  private ConstraintEvaluationCoordinator coordinator;

  /** Wires the coordinator in once it exists -- call this once, right after building the VSUM. */
  public void bind(ConstraintEvaluationCoordinator coordinator) {
    this.coordinator = coordinator;
  }

  void checkBeforeExecuting(Class<? extends Reaction> reactionClass) {
    if (coordinator == null) {
      return;
    }
    List<ViolatedConstraint> violations = coordinator.evaluatePreconditionsFor(reactionClass);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationsDetectedException(violations);
    }
  }
}
