package tools.vitruv.methodologisttemplate.consistency.registry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.dsls.reactions.runtime.reactions.Reaction;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;

/**
 * Wraps a generated Reaction so {@link FiredReactionsCollector#recordFired} is called whenever
 * the wrapped Reaction actually matches and fires for a given change -- not merely whenever
 * {@link Reaction#execute} is invoked, which the Reactions runtime does unconditionally for
 * every registered Reaction on every incoming change, regardless of relevance (see {@code
 * AbstractReactionsChangePropagationSpecification.propagateChange} and {@code
 * AbstractReaction.execute}, which always delegate to the generated {@code executeReaction},
 * which itself no-ops if the change doesn't match).
 *
 * <p>The Reactions compiler generates a public {@code isCurrentChangeMatchingTrigger(EChange)}
 * method on every concrete Reaction class -- confirmed for all Reactions generated from {@code
 * templateReactions.reactions} -- but does not expose it through {@link Reaction} or any shared
 * supertype, so it is resolved reflectively once per wrapped instance instead.
 */
final class HookedReaction implements Reaction {

  private static final String MATCH_METHOD_NAME = "isCurrentChangeMatchingTrigger";

  private final Reaction delegate;
  private final Class<? extends Reaction> delegateClass;
  private final Method matchMethod;
  private final FiredReactionsCollector collector;

  @SuppressWarnings("unchecked")
  HookedReaction(Reaction delegate, FiredReactionsCollector collector) {
    this.delegate = delegate;
    this.delegateClass = (Class<? extends Reaction>) delegate.getClass();
    this.collector = collector;
    this.matchMethod = resolveMatchMethod(delegateClass);
  }

  private static Method resolveMatchMethod(Class<? extends Reaction> reactionClass) {
    try {
      return reactionClass.getMethod(MATCH_METHOD_NAME, EChange.class);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(
          "Generated Reaction "
              + reactionClass.getName()
              + " does not expose "
              + MATCH_METHOD_NAME
              + "(EChange) -- HookedReaction relies on this reactions-compiler convention to know"
              + " whether a Reaction actually fired for a given change.",
          e);
    }
  }

  @Override
  public void execute(EChange<EObject> change, ReactionExecutionState state) {
    boolean matches = invokeMatch(change);
    delegate.execute(change, state);
    if (matches) {
      collector.recordFired(delegateClass);
    }
  }

  private boolean invokeMatch(EChange<EObject> change) {
    try {
      return (boolean) matchMethod.invoke(delegate, change);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException(
          "Failed to invoke " + MATCH_METHOD_NAME + " reflectively on " + delegateClass.getName(),
          e);
    }
  }

  /** Visible for {@code HookedModel2Model2ChangePropagationSpecificationTest}. */
  Class<? extends Reaction> wrappedReactionClass() {
    return delegateClass;
  }
}
