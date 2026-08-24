package tools.vitruv.methodologisttemplate.consistency.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.dsls.reactions.runtime.reactions.Reaction;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;

class HookedReactionTest {

  /** Mimics the shape the Reactions compiler generates: a public match-check method. */
  static final class MatchingFakeReaction implements Reaction {
    boolean executed = false;

    @Override
    public void execute(EChange<EObject> change, ReactionExecutionState state) {
      executed = true;
    }

    public boolean isCurrentChangeMatchingTrigger(EChange<EObject> change) {
      return true;
    }
  }

  static final class NonMatchingFakeReaction implements Reaction {
    boolean executed = false;

    @Override
    public void execute(EChange<EObject> change, ReactionExecutionState state) {
      executed = true;
    }

    public boolean isCurrentChangeMatchingTrigger(EChange<EObject> change) {
      return false;
    }
  }

  static final class NoMatchMethodFakeReaction implements Reaction {
    @Override
    public void execute(EChange<EObject> change, ReactionExecutionState state) {}
  }

  @Test
  void recordsFiredAndDelegatesWhenTriggerMatches() {
    var collector = new FiredReactionsCollector();
    var fake = new MatchingFakeReaction();
    var hooked = new HookedReaction(fake, collector);

    hooked.execute(null, null);

    assertTrue(fake.executed, "delegate.execute must always run regardless of match");
    assertEquals(Set.of(MatchingFakeReaction.class), collector.drain());
  }

  @Test
  void delegatesButDoesNotRecordWhenTriggerDoesNotMatch() {
    var collector = new FiredReactionsCollector();
    var fake = new NonMatchingFakeReaction();
    var hooked = new HookedReaction(fake, collector);

    hooked.execute(null, null);

    assertTrue(fake.executed, "delegate.execute must always run regardless of match");
    assertEquals(Set.of(), collector.drain());
  }

  @Test
  void rejectsReactionWithoutTheGeneratedMatchMethod() {
    var collector = new FiredReactionsCollector();

    assertThrows(
        IllegalStateException.class, () -> new HookedReaction(new NoMatchMethodFakeReaction(), collector));
  }

  @Test
  void wrappedReactionClassReflectsTheDelegate() {
    var collector = new FiredReactionsCollector();
    var hooked = new HookedReaction(new MatchingFakeReaction(), collector);

    assertEquals(MatchingFakeReaction.class, hooked.wrappedReactionClass());
  }
}
