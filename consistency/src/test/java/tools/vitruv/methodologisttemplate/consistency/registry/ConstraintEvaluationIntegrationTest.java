package tools.vitruv.methodologisttemplate.consistency.registry;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import mir.reactions.model2Model2.ComponentInsertedIntoSystemReaction;
import mir.reactions.model2Model2.Model2Model2ChangePropagationSpecification;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
 * Closes the loop end-to-end except for the two open runtime dependencies noted in the
 * implementation plan: obtaining {@code firedReactions} for real (here it is hand-supplied, as
 * a stand-in for what the Reactions runtime would eventually report), and acting on a revert
 * decision (out of scope until a TransactionManager with rollback capability exists).
 */
class ConstraintEvaluationIntegrationTest {

  private static final Path CONSTRAINT_FILE =
      Path.of("src/main/constraints/tools/vitruv/methodologisttemplate/consistency/constraints.ocl");

  @BeforeAll
  static void setup() {
    Resource.Factory.Registry.INSTANCE
        .getExtensionToFactoryMap()
        .put("*", new XMIResourceFactoryImpl());
  }

  @Test
  void filtersToOnlyTheConstraintsOfTheReactionThatActuallyRan(@TempDir Path tempDir)
      throws IOException {
    InternalVirtualModel vsum =
        new VirtualModelBuilder()
            .withStorageFolder(tempDir)
            .withUserInteractorForResultProvider(
                new TestUserInteraction.ResultProvider(new TestUserInteraction()))
            .withChangePropagationSpecifications(new Model2Model2ChangePropagationSpecification())
            .buildAndInitialize();
    vsum.setChangePropagationMode(ChangePropagationMode.TRANSITIVE_CYCLIC);

    addSystem(vsum, tempDir);
    addComponent(vsum); // triggers ComponentInsertedIntoSystemReaction

    VitruvOCL.registerVSUM(vsum);
    var registry = ProjectReactionConstraints.buildRegistry();
    var gateway = new VitruvOCLGatewayImpl(CONSTRAINT_FILE);
    var coordinator = new ConstraintEvaluationCoordinator(registry, gateway);

    // Stand-in for the not-yet-available real fired-Reaction set (see plan header).
    Set<Class<? extends Reaction>> firedReactions =
        Set.of(ComponentInsertedIntoSystemReaction.class);

    List<ViolatedConstraint> violations = coordinator.evaluateFor(firedReactions);

    assertTrue(
        violations.isEmpty(),
        () ->
            "Expected no violations among constraints registered for the fired Reactions: "
                + violations);
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

  private void addComponent(InternalVirtualModel vsum) {
    var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
    selector.getSelectableElements().stream()
        .filter(System.class::isInstance)
        .forEach(e -> selector.setSelected(e, true));
    CommittableView view = selector.createView().withChangeDerivingTrait();
    var component = ModelFactory.eINSTANCE.createComponent();
    component.setName("specialname");
    view.getRootObjects(System.class).iterator().next().getComponents().add(component);
    view.commitChanges();
  }
}
