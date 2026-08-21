package tools.vitruv.methodologisttemplate.consistency.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.dsls.reactions.runtime.reactions.Reaction;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;

class ConstraintEvaluationCoordinatorTest {

  interface FakeReaction extends Reaction {
    @Override
    default void execute(EChange<EObject> change, ReactionExecutionState state) {}
  }

  @Test
  void returnsEmptyWhenNoConstraintsRegisteredForFiredReactions() {
    var registry = new ReactionConstraintRegistry();
    var ocl = mock(VitruvOCLGateway.class);
    var coordinator = new ConstraintEvaluationCoordinator(registry, ocl);

    var relevantViolations = coordinator.evaluateFor(Set.of(FakeReaction.class));

    assertTrue(relevantViolations.isEmpty());
    verifyNoInteractions(ocl);
  }

  @Test
  void filtersWholeFileResultDownToRegisteredConstraintsOnly() {
    var registry = new ReactionConstraintRegistry();
    var relevantRef = new ConstraintRef("model::System", "SystemInsertedAsRoot");
    registry.register(FakeReaction.class, relevantRef);

    var ocl = mock(VitruvOCLGateway.class);
    when(ocl.evaluateAll())
        .thenReturn(
            List.of(
                new ViolatedConstraint("model::System", "SystemInsertedAsRoot", "Root missing"),
                new ViolatedConstraint("model::Component", "UnrelatedConstraint", "Irrelevant here")));

    var coordinator = new ConstraintEvaluationCoordinator(registry, ocl);
    var relevantViolations = coordinator.evaluateFor(Set.of(FakeReaction.class));

    assertEquals(1, relevantViolations.size());
    assertEquals(relevantRef, relevantViolations.get(0).ref());
  }
}
