package tools.vitruv.methodologisttemplate.consistency.registry;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.change.propagation.ChangePropagationMode;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.dsls.vitruvocl.pipeline.VitruvOCL;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;
import tools.vitruv.methodologisttemplate.model.model.ModelFactory;
import tools.vitruv.methodologisttemplate.model.model.System;

/**
 * Proves the automatic hook end-to-end: with a {@link HookedModel2Model2ChangePropagationSpecification}
 * and a {@link ReactionConstraintCheckingListener} wired onto a VSUM, committing a change that
 * makes a Reaction fire is enough on its own -- no manual {@link
 * ConstraintEvaluationCoordinator#evaluateFor} call anywhere in this test -- to have the relevant
 * constraints checked and violations reported.
 *
 * <p>Deliberately creates a {@code model::Link} with only one Component, which real (working)
 * {@code inv} constraint {@code LinkHasAtLeastTwoEntities} in {@code constraints.ocl} forbids,
 * so this test observes an actual, non-trivial violation surfacing automatically -- not just
 * the pre/post no-op case documented in {@link PrePostConstraintWorkflowTest}.
 */
class AutomaticConstraintCheckingIntegrationTest {

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

  @Test
  void committingAChangeAutomaticallyChecksTheReactionsConstraints(@TempDir Path tempDir)
      throws IOException {
    var hookedSpecification = new HookedModel2Model2ChangePropagationSpecification();

    InternalVirtualModel vsum =
        new VirtualModelBuilder()
            .withStorageFolder(tempDir)
            .withUserInteractorForResultProvider(
                new TestUserInteraction.ResultProvider(new TestUserInteraction()))
            .withChangePropagationSpecifications(hookedSpecification)
            .buildAndInitialize();
    vsum.setChangePropagationMode(ChangePropagationMode.TRANSITIVE_CYCLIC);

    VitruvOCL.registerVSUM(vsum);
    var registry = ProjectReactionConstraints.buildRegistry();
    var gateway = new VitruvOCLGatewayImpl(CONSTRAINT_FILE, PREPOST_CONSTRAINT_FILE);
    var coordinator = new ConstraintEvaluationCoordinator(registry, gateway);

    List<ViolatedConstraint> capturedViolations = new ArrayList<>();
    vsum.addChangePropagationListener(
        new ReactionConstraintCheckingListener(
            hookedSpecification.getCollector(), coordinator, capturedViolations::addAll));

    addSystem(vsum, tempDir);
    addLinkWithOnlyOneComponent(vsum);

    assertTrue(
        capturedViolations.stream()
            .anyMatch(v -> v.ref().equals(new ConstraintRef("model2::Link", "LinkHasAtLeastTwoEntities"))),
        () ->
            "Expected LinkHasAtLeastTwoEntities to be reported automatically after committing a "
                + "Link with only one Component. Captured violations: "
                + capturedViolations);
  }

  @Test
  void failFastMakesTheCommitThatViolatesAConstraintThrowWithAMeaningfulMessage(
      @TempDir Path tempDir) throws IOException {
    var hookedSpecification = new HookedModel2Model2ChangePropagationSpecification();

    InternalVirtualModel vsum =
        new VirtualModelBuilder()
            .withStorageFolder(tempDir)
            .withUserInteractorForResultProvider(
                new TestUserInteraction.ResultProvider(new TestUserInteraction()))
            .withChangePropagationSpecifications(hookedSpecification)
            .buildAndInitialize();
    vsum.setChangePropagationMode(ChangePropagationMode.TRANSITIVE_CYCLIC);

    VitruvOCL.registerVSUM(vsum);
    var registry = ProjectReactionConstraints.buildRegistry();
    var gateway = new VitruvOCLGatewayImpl(CONSTRAINT_FILE, PREPOST_CONSTRAINT_FILE);
    var coordinator = new ConstraintEvaluationCoordinator(registry, gateway);

    vsum.addChangePropagationListener(
        ReactionConstraintCheckingListener.failFast(hookedSpecification.getCollector(), coordinator));

    addSystem(vsum, tempDir);

    // Unlike committingAChangeAutomaticallyChecksTheReactionsConstraints (which merely captures
    // violations into a list), this asserts the offending commitChanges() call itself throws --
    // proving a test using failFast cannot stay green when a Reaction violates a constraint.
    var exception =
        assertThrows(
            ConstraintViolationsDetectedException.class, () -> addLinkWithOnlyOneComponent(vsum));

    assertTrue(
        exception.getMessage().contains("LinkHasAtLeastTwoEntities"),
        () -> "Expected the failure message to name the violated constraint. Got: " + exception.getMessage());
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

  private void addLinkWithOnlyOneComponent(InternalVirtualModel vsum) {
    var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
    selector.getSelectableElements().stream()
        .filter(System.class::isInstance)
        .forEach(e -> selector.setSelected(e, true));
    CommittableView view = selector.createView().withChangeDerivingTrait();

    var system = view.getRootObjects(System.class).iterator().next();
    var component = ModelFactory.eINSTANCE.createComponent();
    component.setName("onlyComponent");
    system.getComponents().add(component);

    var link = ModelFactory.eINSTANCE.createLink();
    system.getLinks().add(link);
    link.getComponents().add(component);

    view.commitChanges();
  }
}
