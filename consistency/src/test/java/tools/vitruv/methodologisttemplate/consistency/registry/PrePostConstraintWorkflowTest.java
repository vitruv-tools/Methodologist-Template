package tools.vitruv.methodologisttemplate.consistency.registry;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import mir.reactions.model2Model2.ComponentInsertedIntoSystemReaction;
import mir.reactions.model2Model2.Model2Model2ChangePropagationSpecification;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.description.TransactionalChange;
import tools.vitruv.change.composite.recording.ChangeRecorder;
import tools.vitruv.change.propagation.ChangePropagationMode;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.dsls.reactions.runtime.reactions.Reaction;
import tools.vitruv.dsls.vitruvocl.pipeline.VitruvOCL;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;
import tools.vitruv.methodologisttemplate.model.model.ModelFactory;
import tools.vitruv.methodologisttemplate.model.model.System;

/**
 * Exercises the full Reaction-Constraint Registry workflow — registry lookup, {@link
 * VitruvOCLGatewayImpl} parsing, {@link ConstraintEvaluationCoordinator} filtering — against
 * {@code prepost-example.ocl}, the file that uses OCL#'s {@code pre}/{@code post} syntax instead
 * of {@code inv}.
 *
 * <p>Unlike {@link ConstraintEvaluationIntegrationTest} (which uses the transaction-less {@link
 * ConstraintEvaluationCoordinator#evaluateFor(Set)} and therefore only ever exercises {@code
 * inv}-equivalent, always-checked behavior), the tests here record a real transaction via {@link
 * ChangeRecorder} and call {@link ConstraintEvaluationCoordinator#evaluateFor(Set, List)} — the
 * genuinely-evaluating path — specifically so {@code pre}/{@code post} get exercised for real.
 */
class PrePostConstraintWorkflowTest {

  private static final Path CONSTRAINT_FILE =
      Path.of("src/main/constraints/tools/vitruv/methodologisttemplate/consistency/constraints.ocl");
  private static final Path PREPOST_CONSTRAINT_FILE =
      Path.of("src/main/constraints/tools/vitruv/methodologisttemplate/consistency/prepost-example.ocl");

  @BeforeAll
  static void setup() {
    Resource.Factory.Registry.INSTANCE
        .getExtensionToFactoryMap()
        .put("*", new XMIResourceFactoryImpl());
  }

  private InternalVirtualModel createVsum(Path tempDir) throws IOException {
    InternalVirtualModel vsum =
        new VirtualModelBuilder()
            .withStorageFolder(tempDir)
            .withUserInteractorForResultProvider(
                new TestUserInteraction.ResultProvider(new TestUserInteraction()))
            .withChangePropagationSpecifications(new Model2Model2ChangePropagationSpecification())
            .buildAndInitialize();
    vsum.setChangePropagationMode(ChangePropagationMode.TRANSITIVE_CYCLIC);
    return vsum;
  }

  private void addSystem(InternalVirtualModel vsum, Path projectPath) {
    CommittableView view =
        vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"))
            .createView()
            .withChangeDerivingTrait();
    view.registerRoot(
        ModelFactory.eINSTANCE.createSystem(),
        URI.createFileURI(projectPath.toString() + "/example.model"));
    view.commitChanges();
  }

  private System getSystem(InternalVirtualModel vsum) {
    var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
    selector.getSelectableElements().stream()
        .filter(System.class::isInstance)
        .forEach(e -> selector.setSelected(e, true));
    return (System) selector.createView().getRootObjects(System.class).iterator().next();
  }

  /**
   * Adds a Component (which triggers {@code ComponentInsertedIntoSystemReaction}) while recording
   * every {@link EChange} touched by both the direct edit and the reaction's own consequential
   * changes, mirroring how {@link ReactionConstraintCheckingListener} assembles the transaction it
   * passes to {@link ConstraintEvaluationCoordinator#evaluateFor(Set, List)} in production.
   */
  private List<EChange<EObject>> addComponentRecordingTransaction(
      InternalVirtualModel vsum, String name) {
    System system = getSystem(vsum);
    ResourceSet resourceSet = system.eResource().getResourceSet();

    ChangeRecorder recorder = new ChangeRecorder(resourceSet);
    recorder.addToRecording(resourceSet);
    recorder.beginRecording();

    var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
    selector.getSelectableElements().stream()
        .filter(System.class::isInstance)
        .forEach(e -> selector.setSelected(e, true));
    CommittableView view = selector.createView().withChangeDerivingTrait();
    var component = ModelFactory.eINSTANCE.createComponent();
    component.setName(name);
    view.getRootObjects(System.class).iterator().next().getComponents().add(component);
    view.commitChanges();

    TransactionalChange<EObject> change = recorder.endRecording();
    return change.getEChanges();
  }

  @Test
  void wellFormedComponentReportsNoViolations(@TempDir Path tempDir) throws IOException {
    InternalVirtualModel vsum = createVsum(tempDir);
    addSystem(vsum, tempDir);
    List<EChange<EObject>> transaction = addComponentRecordingTransaction(vsum, "specialname");

    VitruvOCL.registerVSUM(vsum);
    var registry = ProjectReactionConstraints.buildRegistry();
    var gateway = new VitruvOCLGatewayImpl(CONSTRAINT_FILE, PREPOST_CONSTRAINT_FILE);
    var coordinator = new ConstraintEvaluationCoordinator(registry, gateway);

    Set<Class<? extends Reaction>> firedReactions =
        Set.of(ComponentInsertedIntoSystemReaction.class);

    List<ViolatedConstraint> violations = coordinator.evaluateFor(firedReactions, transaction);

    assertTrue(violations.isEmpty(), () -> "Expected no violations, got: " + violations);
  }

  @Test
  void componentWithBlankNameReportsPreconditionViolation(@TempDir Path tempDir)
      throws IOException {
    InternalVirtualModel vsum = createVsum(tempDir);
    addSystem(vsum, tempDir);
    // Violates ComponentHasNameBeforeSync (a "pre" constraint: self.name != "").
    List<EChange<EObject>> transaction = addComponentRecordingTransaction(vsum, "");

    VitruvOCL.registerVSUM(vsum);
    var registry = ProjectReactionConstraints.buildRegistry();
    var gateway = new VitruvOCLGatewayImpl(CONSTRAINT_FILE, PREPOST_CONSTRAINT_FILE);
    var coordinator = new ConstraintEvaluationCoordinator(registry, gateway);

    Set<Class<? extends Reaction>> firedReactions =
        Set.of(ComponentInsertedIntoSystemReaction.class);

    List<ViolatedConstraint> violations = coordinator.evaluateFor(firedReactions, transaction);

    assertTrue(
        violations.stream()
            .anyMatch(v -> v.ref().equals(new ConstraintRef("model::Component", "ComponentHasNameBeforeSync"))),
        () -> "Expected ComponentHasNameBeforeSync to be reported as violated, got: " + violations);
  }
}
