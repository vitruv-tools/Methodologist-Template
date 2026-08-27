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
    var hooked = new HookedReaction(fake, collector, new PreconditionGuard());

    hooked.execute(null, null);

    assertTrue(fake.executed, "delegate.execute must always run regardless of match");
    assertEquals(Set.of(MatchingFakeReaction.class), collector.drain());
  }

  @Test
  void delegatesButDoesNotRecordWhenTriggerDoesNotMatch() {
    var collector = new FiredReactionsCollector();
    var fake = new NonMatchingFakeReaction();
    var hooked = new HookedReaction(fake, collector, new PreconditionGuard());

    hooked.execute(null, null);

    assertTrue(fake.executed, "delegate.execute must always run regardless of match");
    assertEquals(Set.of(), collector.drain());
  }

  @Test
  void rejectsReactionWithoutTheGeneratedMatchMethod() {
    var collector = new FiredReactionsCollector();

    assertThrows(
        IllegalStateException.class,
        () -> new HookedReaction(new NoMatchMethodFakeReaction(), collector, new PreconditionGuard()));
  }

  @Test
  void wrappedReactionClassReflectsTheDelegate() {
    var collector = new FiredReactionsCollector();
    var hooked = new HookedReaction(new MatchingFakeReaction(), collector, new PreconditionGuard());

    assertEquals(MatchingFakeReaction.class, hooked.wrappedReactionClass());
  }

  @Test
  void doesNotExecuteWhenTheBoundPreconditionGuardRejectsIt() {
    var collector = new FiredReactionsCollector();
    var registry = new ReactionConstraintRegistry();
    var ref = new ConstraintRef("model::Component", "SomePrecondition");
    registry.register(MatchingFakeReaction.class, ref);

    var ocl = org.mockito.Mockito.mock(VitruvOCLGateway.class);
    org.mockito.Mockito.when(ocl.evaluateAll(java.util.List.of()))
        .thenReturn(
            java.util.List.of(
                new EvaluatedConstraint(
                    "model::Component",
                    "SomePrecondition",
                    ConstraintKind.PRECONDITION,
                    false,
                    "precondition not met")));

    var coordinator = new ConstraintEvaluationCoordinator(registry, ocl);
    var guard = new PreconditionGuard();
    guard.bind(coordinator);

    var fake = new MatchingFakeReaction();
    var hooked = new HookedReaction(fake, collector, guard);

    assertThrows(ConstraintViolationsDetectedException.class, () -> hooked.execute(null, null));

    assertTrue(
        !fake.executed, "delegate.execute must not run when a registered precondition is violated");
    assertEquals(Set.of(), collector.drain());
  }

  @Test
  void executesNormallyWhenTheBoundPreconditionGuardIsSatisfied() {
    var collector = new FiredReactionsCollector();
    var registry = new ReactionConstraintRegistry();
    var ref = new ConstraintRef("model::Component", "SomePrecondition");
    registry.register(MatchingFakeReaction.class, ref);

    var ocl = org.mockito.Mockito.mock(VitruvOCLGateway.class);
    org.mockito.Mockito.when(ocl.evaluateAll(java.util.List.of()))
        .thenReturn(
            java.util.List.of(
                new EvaluatedConstraint(
                    "model::Component", "SomePrecondition", ConstraintKind.PRECONDITION, true, "OK")));

    var coordinator = new ConstraintEvaluationCoordinator(registry, ocl);
    var guard = new PreconditionGuard();
    guard.bind(coordinator);

    var fake = new MatchingFakeReaction();
    var hooked = new HookedReaction(fake, collector, guard);

    hooked.execute(null, null);

    assertTrue(fake.executed, "delegate.execute must run when the precondition is satisfied");
    assertEquals(Set.of(MatchingFakeReaction.class), collector.drain());
  }
}
