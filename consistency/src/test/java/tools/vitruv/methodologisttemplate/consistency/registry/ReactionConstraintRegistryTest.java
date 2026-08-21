package tools.vitruv.methodologisttemplate.consistency.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.dsls.reactions.runtime.reactions.Reaction;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;

class ReactionConstraintRegistryTest {

  // Stand-ins for real generated Reaction classes, used only to exercise the registry's
  // keying-by-class behavior in isolation.
  interface FakeReactionA extends Reaction {
    @Override
    default void execute(EChange<EObject> change, ReactionExecutionState state) {}
  }

  interface FakeReactionB extends Reaction {
    @Override
    default void execute(EChange<EObject> change, ReactionExecutionState state) {}
  }

  interface FakeReactionUnregistered extends Reaction {
    @Override
    default void execute(EChange<EObject> change, ReactionExecutionState state) {}
  }

  @Test
  void returnsEmptyListForUnregisteredReaction() {
    var registry = new ReactionConstraintRegistry();
    assertEquals(List.of(), registry.getConstraintsFor(FakeReactionUnregistered.class));
  }

  @Test
  void returnsRegisteredConstraintsForOneReaction() {
    var registry = new ReactionConstraintRegistry();
    var ref1 = new ConstraintRef("model::System", "SystemInsertedAsRoot");
    var ref2 = new ConstraintRef("model::System", "RootIsUnique");
    registry.register(FakeReactionA.class, ref1, ref2);

    assertEquals(List.of(ref1, ref2), registry.getConstraintsFor(FakeReactionA.class));
  }

  @Test
  void keepsRegistrationsForDifferentReactionsSeparate() {
    var registry = new ReactionConstraintRegistry();
    var refA = new ConstraintRef("model::System", "SystemInsertedAsRoot");
    var refB = new ConstraintRef("model::Component", "ComponentRenamed");
    registry.register(FakeReactionA.class, refA);
    registry.register(FakeReactionB.class, refB);

    assertEquals(List.of(refA), registry.getConstraintsFor(FakeReactionA.class));
    assertEquals(List.of(refB), registry.getConstraintsFor(FakeReactionB.class));
  }

  @Test
  void getConstraintsForAllUnionsAcrossFiredReactionsWithoutDuplicates() {
    var registry = new ReactionConstraintRegistry();
    var shared = new ConstraintRef("model::System", "RootIsUnique");
    var onlyA = new ConstraintRef("model::System", "SystemInsertedAsRoot");
    var onlyB = new ConstraintRef("model::Component", "ComponentRenamed");
    registry.register(FakeReactionA.class, onlyA, shared);
    registry.register(FakeReactionB.class, onlyB, shared);

    Set<ConstraintRef> result =
        registry.getConstraintsForAll(Set.of(FakeReactionA.class, FakeReactionB.class));

    assertEquals(Set.of(onlyA, onlyB, shared), result);
  }

  @Test
  void registeringSameReactionTwiceAppendsRatherThanOverwrites() {
    var registry = new ReactionConstraintRegistry();
    var ref1 = new ConstraintRef("model::System", "SystemInsertedAsRoot");
    var ref2 = new ConstraintRef("model::System", "RootIsUnique");
    registry.register(FakeReactionA.class, ref1);
    registry.register(FakeReactionA.class, ref2);

    assertEquals(List.of(ref1, ref2), registry.getConstraintsFor(FakeReactionA.class));
  }
}
