package tools.vitruv.methodologisttemplate.consistency.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.dsls.reactions.runtime.reactions.Reaction;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.methodologisttemplate.consistency.registry.VitruvOCLGateway.DeclaredConstraint;

class ConstraintEvaluationCoordinatorTest {

  interface FakeReaction extends Reaction {
    @Override
    default void execute(EChange<EObject> change, ReactionExecutionState state) {}
  }

  interface OtherFakeReaction extends Reaction {
    @Override
    default void execute(EChange<EObject> change, ReactionExecutionState state) {}
  }

  private static final DeclaredConstraint PRE =
      declared("model::Component", "SomePre", ConstraintKind.PRECONDITION);
  private static final DeclaredConstraint POST =
      declared("model2::Entity", "SomePost", ConstraintKind.POSTCONDITION);
  private static final DeclaredConstraint UNASSIGNED_POST =
      declared("model2::Entity", "OtherPost", ConstraintKind.POSTCONDITION);

  private static DeclaredConstraint declared(String type, String name, ConstraintKind kind) {
    return new DeclaredConstraint(new ConstraintRef(type, name), kind, "context " + type + " ...");
  }

  private static EvaluatedConstraint result(DeclaredConstraint c, boolean satisfied) {
    return new EvaluatedConstraint(
        c.ref().contextType(), c.ref().constraintName(), c.kind(), satisfied, "diagnostic");
  }

  private static VitruvOCLGateway gatewayDeclaring(DeclaredConstraint... constraints) {
    var ocl = mock(VitruvOCLGateway.class);
    when(ocl.declarations()).thenReturn(List.of(constraints));
    return ocl;
  }

  @Test
  void aReactionWithoutRegisteredConstraintsHasNoContract() {
    var coordinator =
        new ConstraintEvaluationCoordinator(new ReactionConstraintRegistry(), gatewayDeclaring());

    assertFalse(coordinator.hasContract(FakeReaction.class));
  }

  @Test
  void evaluatesOnlyThePreconditionsOfTheReactionWithAnEmptyTransaction() {
    var registry = new ReactionConstraintRegistry();
    registry.register(FakeReaction.class, PRE.ref(), POST.ref());
    var ocl = gatewayDeclaring(PRE, POST, UNASSIGNED_POST);
    when(ocl.evaluate(PRE, List.of())).thenReturn(result(PRE, false));
    var coordinator = new ConstraintEvaluationCoordinator(registry, ocl);

    var violations = coordinator.evaluatePreconditionsFor(FakeReaction.class);

    assertEquals(1, violations.size());
    assertEquals("SomePre", violations.get(0).constraintName());
    assertEquals(ConstraintKind.PRECONDITION, violations.get(0).kind());
    verify(ocl).evaluate(PRE, List.of());
    verify(ocl, never()).evaluate(org.mockito.ArgumentMatchers.eq(POST), any());
    verify(ocl, never()).evaluate(org.mockito.ArgumentMatchers.eq(UNASSIGNED_POST), any());
  }

  @Test
  void evaluatesThePostconditionsAgainstTheChangesOfTheReactionExecution() {
    var registry = new ReactionConstraintRegistry();
    registry.register(FakeReaction.class, PRE.ref(), POST.ref());
    var ocl = gatewayDeclaring(PRE, POST, UNASSIGNED_POST);
    @SuppressWarnings("unchecked")
    List<EChange<EObject>> reactionChanges = List.of(mock(EChange.class));
    when(ocl.evaluate(POST, reactionChanges)).thenReturn(result(POST, true));
    var coordinator = new ConstraintEvaluationCoordinator(registry, ocl);

    assertTrue(coordinator.hasPostconditions(FakeReaction.class));
    assertTrue(coordinator.evaluatePostconditionsFor(FakeReaction.class, reactionChanges).isEmpty());
    verify(ocl).evaluate(POST, reactionChanges);
    verify(ocl, never()).evaluate(UNASSIGNED_POST, reactionChanges);
  }

  @Test
  void reportsOnlyViolatedInvariants() {
    var ocl = gatewayDeclaring();
    var satisfied = declared("model::Component", "Holds", ConstraintKind.INVARIANT);
    var violated = declared("model2::Link", "Broken", ConstraintKind.INVARIANT);
    when(ocl.evaluateInvariants()).thenReturn(List.of(result(satisfied, true), result(violated, false)));
    var coordinator = new ConstraintEvaluationCoordinator(new ReactionConstraintRegistry(), ocl);

    var violations = coordinator.evaluateInvariants();

    assertEquals(1, violations.size());
    assertEquals("Broken", violations.get(0).constraintName());
    assertEquals(ConstraintKind.INVARIANT, violations.get(0).kind());
  }

  @Test
  void throwsForARegisteredConstraintThatNoFileDeclares() {
    var registry = new ReactionConstraintRegistry();
    registry.register(FakeReaction.class, new ConstraintRef("model::Component", "Typo"));
    var coordinator = new ConstraintEvaluationCoordinator(registry, gatewayDeclaring(PRE));

    var exception =
        assertThrows(
            UnknownConstraintException.class,
            () -> coordinator.evaluatePreconditionsFor(FakeReaction.class));
    assertTrue(exception.getMessage().contains("Typo"));
  }

  @Test
  void contractsOfOtherReactionsAreNotEvaluated() {
    var registry = new ReactionConstraintRegistry();
    registry.register(OtherFakeReaction.class, PRE.ref());
    var ocl = gatewayDeclaring(PRE);
    var coordinator = new ConstraintEvaluationCoordinator(registry, ocl);

    assertTrue(coordinator.evaluatePreconditionsFor(FakeReaction.class).isEmpty());
    verify(ocl, never()).evaluate(any(), any());
  }
}
