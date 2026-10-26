package tools.vitruv.methodologisttemplate.consistency.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.dsls.reactions.runtime.reactions.Reaction;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;

/**
 * Unit tests of {@link HookedReaction} without a VSUM. Checking a contract against real models,
 * including asking the user and stopping the commit, is covered by {@link
 * ReactionContractIntegrationTest}.
 */
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
  void delegatesUncheckedAsLongAsNoCoordinatorIsBound() {
    var fake = new MatchingFakeReaction();
    var hooked = new HookedReaction(fake, new ContractEnforcer());

    hooked.execute(null, null);

    assertTrue(fake.executed);
  }

  @Test
  void delegatesUncheckedWhenTheReactionHasNoContract() {
    var coordinator = mock(ConstraintEvaluationCoordinator.class);
    when(coordinator.hasContract(MatchingFakeReaction.class)).thenReturn(false);
    var enforcer = new ContractEnforcer();
    enforcer.bind(coordinator, new ContractLog(), () -> null);
    var fake = new MatchingFakeReaction();

    new HookedReaction(fake, enforcer).execute(null, null);

    assertTrue(fake.executed);
  }

  @Test
  void delegatesUncheckedWhenTheTriggerDoesNotMatch() {
    var coordinator = mock(ConstraintEvaluationCoordinator.class);
    when(coordinator.hasContract(NonMatchingFakeReaction.class)).thenReturn(true);
    var enforcer = new ContractEnforcer();
    enforcer.bind(coordinator, new ContractLog(), () -> null);
    var fake = new NonMatchingFakeReaction();

    new HookedReaction(fake, enforcer).execute(null, null);

    assertTrue(fake.executed, "delegate.execute must always run; it no-ops itself on a mismatch");
  }

  @Test
  void rejectsReactionWithoutTheGeneratedMatchMethod() {
    assertThrows(
        IllegalStateException.class,
        () -> new HookedReaction(new NoMatchMethodFakeReaction(), new ContractEnforcer()));
  }

  @Test
  void wrappedReactionClassReflectsTheDelegate() {
    var hooked = new HookedReaction(new MatchingFakeReaction(), new ContractEnforcer());

    assertEquals(MatchingFakeReaction.class, hooked.wrappedReactionClass());
  }
}
