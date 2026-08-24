package tools.vitruv.methodologisttemplate.consistency.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.dsls.reactions.runtime.reactions.Reaction;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;

class FiredReactionsCollectorTest {

  interface FakeReactionA extends Reaction {
    @Override
    default void execute(EChange<EObject> change, ReactionExecutionState state) {}
  }

  interface FakeReactionB extends Reaction {
    @Override
    default void execute(EChange<EObject> change, ReactionExecutionState state) {}
  }

  @Test
  void drainReturnsRecordedClassesAndClears() {
    var collector = new FiredReactionsCollector();
    collector.recordFired(FakeReactionA.class);
    collector.recordFired(FakeReactionB.class);

    assertEquals(Set.of(FakeReactionA.class, FakeReactionB.class), collector.drain());
    assertEquals(Set.of(), collector.drain());
  }

  @Test
  void recordingTheSameClassTwiceDoesNotDuplicate() {
    var collector = new FiredReactionsCollector();
    collector.recordFired(FakeReactionA.class);
    collector.recordFired(FakeReactionA.class);

    assertEquals(Set.of(FakeReactionA.class), collector.drain());
  }
}
