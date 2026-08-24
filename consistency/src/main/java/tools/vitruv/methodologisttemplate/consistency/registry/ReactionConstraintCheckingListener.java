package tools.vitruv.methodologisttemplate.consistency.registry;

import java.util.List;
import java.util.function.Consumer;
import tools.vitruv.change.atomic.uuid.Uuid;
import tools.vitruv.change.composite.description.PropagatedChange;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.composite.propagation.ChangePropagationListener;

/**
 * Automatically evaluates the constraints relevant to whichever Reactions fired during a
 * transaction, right after change propagation for that transaction finishes.
 *
 * <p>Register this on the same VSUM that uses a {@link
 * HookedModel2Model2ChangePropagationSpecification}, sharing that instance's {@link
 * FiredReactionsCollector} (via {@link HookedModel2Model2ChangePropagationSpecification#getCollector()}).
 * Without a matching Hooked specification feeding it, the collector stays empty and this listener
 * never evaluates anything -- see {@code AutomaticConstraintCheckingIntegrationTest} for the full
 * wiring.
 *
 * <p><b>The {@code onViolations} callback you pass to the constructor does not by itself make
 * anything fail.</b> A callback that only logs (e.g. {@code v -> System.err.println(v)}) leaves a
 * test green even though a Reaction just violated a registered constraint -- the callback runs,
 * nothing more. Use {@link #failFast} instead wherever a violation should actually stop the test
 * (or the calling program) with a clear message.
 */
public final class ReactionConstraintCheckingListener implements ChangePropagationListener {

  private final FiredReactionsCollector collector;
  private final ConstraintEvaluationCoordinator coordinator;
  private final Consumer<List<ViolatedConstraint>> onViolations;

  public ReactionConstraintCheckingListener(
      FiredReactionsCollector collector,
      ConstraintEvaluationCoordinator coordinator,
      Consumer<List<ViolatedConstraint>> onViolations) {
    this.collector = collector;
    this.coordinator = coordinator;
    this.onViolations = onViolations;
  }

  /**
   * A listener that throws {@link ConstraintViolationsDetectedException} -- uncaught, right out
   * of {@code commitChanges()} -- the moment any fired Reaction's constraints are violated. This
   * is what makes a test actually go red with a message naming every violated constraint, instead
   * of merely calling a callback that a passing test could ignore.
   */
  public static ReactionConstraintCheckingListener failFast(
      FiredReactionsCollector collector, ConstraintEvaluationCoordinator coordinator) {
    return new ReactionConstraintCheckingListener(
        collector,
        coordinator,
        violations -> {
          throw new ConstraintViolationsDetectedException(violations);
        });
  }

  @Override
  public void startedChangePropagation(VitruviusChange<Uuid> changeToPropagate) {
    // Constraints are only meaningful to check once propagation (and therefore all Reactions
    // for this transaction) has finished.
  }

  @Override
  public void finishedChangePropagation(Iterable<PropagatedChange> propagatedChanges) {
    var firedReactions = collector.drain();
    if (firedReactions.isEmpty()) {
      return;
    }
    var violations = coordinator.evaluateFor(firedReactions);
    if (!violations.isEmpty()) {
      onViolations.accept(violations);
    }
  }
}
