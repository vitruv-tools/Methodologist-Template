package tools.vitruv.methodologisttemplate.consistency.registry;

import java.util.LinkedHashSet;
import java.util.Set;
import tools.vitruv.dsls.reactions.runtime.reactions.Reaction;

/**
 * Accumulates the Reaction classes that actually fired during the current transaction, so a
 * {@link ReactionConstraintCheckingListener} can hand them to {@link
 * ConstraintEvaluationCoordinator#evaluateFor} once change propagation finishes.
 *
 * <p>Vitruvius change propagation for one VSUM commit runs synchronously on a single thread, so
 * this class is intentionally not designed for concurrent transactions on the same VSUM.
 */
public final class FiredReactionsCollector {

  private final Set<Class<? extends Reaction>> fired = new LinkedHashSet<>();

  void recordFired(Class<? extends Reaction> reactionClass) {
    fired.add(reactionClass);
  }

  /** Returns everything recorded so far and clears the collector for the next transaction. */
  public Set<Class<? extends Reaction>> drain() {
    var snapshot = Set.copyOf(fired);
    fired.clear();
    return snapshot;
  }
}
