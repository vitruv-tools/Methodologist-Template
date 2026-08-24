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
 * Exercises the full Reaction-Constraint Registry workflow — registry lookup, {@link
 * VitruvOCLGatewayImpl} parsing, {@link ConstraintEvaluationCoordinator} filtering — against
 * {@code prepost-example.ocl}, the file that uses OCL#'s {@code pre}/{@code post} syntax instead
 * of {@code inv}.
 *
 * <p><b>Read this before "fixing" this test:</b> as documented in {@code prepost-example.ocl} and
 * in {@link VitruvOCLGatewayImpl}, the vitruvocl-language snapshot currently in use does not
 * evaluate {@code pre}/{@code post} bodies for truthiness (no {@code visitPreCS}/{@code
 * visitPostCS} in its {@code EvaluationVisitor}) — every pre/post constraint is reported as
 * satisfied regardless of its actual body. That is why {@link
 * #componentWithBlankNameStillReportsNoViolations} deliberately creates a Component that
 * violates {@code ComponentHasNameBeforeSync} (blank name) and still asserts an *empty* violation
 * list: today, that is the only correct assertion. Once {@code visitPreCS}/{@code visitPostCS}
 * are implemented upstream, this test's second case should start failing — at that point, invert
 * the assertion to expect the violation, as proof the fix landed and the registry wiring picks it
 * up with no changes needed on this side.
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

  private void addComponent(InternalVirtualModel vsum, String name) {
    var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
    selector.getSelectableElements().stream()
        .filter(System.class::isInstance)
        .forEach(e -> selector.setSelected(e, true));
    CommittableView view = selector.createView().withChangeDerivingTrait();
    var component = ModelFactory.eINSTANCE.createComponent();
    component.setName(name);
    view.getRootObjects(System.class).iterator().next().getComponents().add(component);
    view.commitChanges();
  }

  @Test
  void wellFormedComponentReportsNoViolations(@TempDir Path tempDir) throws IOException {
    InternalVirtualModel vsum = createVsum(tempDir);
    addSystem(vsum, tempDir);
    addComponent(vsum, "specialname");

    VitruvOCL.registerVSUM(vsum);
    var registry = ProjectReactionConstraints.buildRegistry();
    var gateway = new VitruvOCLGatewayImpl(CONSTRAINT_FILE, PREPOST_CONSTRAINT_FILE);
    var coordinator = new ConstraintEvaluationCoordinator(registry, gateway);

    Set<Class<? extends Reaction>> firedReactions =
        Set.of(ComponentInsertedIntoSystemReaction.class);

    List<ViolatedConstraint> violations = coordinator.evaluateFor(firedReactions);

    assertTrue(violations.isEmpty(), () -> "Expected no violations, got: " + violations);
  }

  @Test
  void componentWithBlankNameStillReportsNoViolations(@TempDir Path tempDir) throws IOException {
    InternalVirtualModel vsum = createVsum(tempDir);
    addSystem(vsum, tempDir);
    // Deliberately violates ComponentHasNameBeforeSync (a "pre" constraint). See the class
    // Javadoc: this must currently still report zero violations.
    addComponent(vsum, "");

    VitruvOCL.registerVSUM(vsum);
    var registry = ProjectReactionConstraints.buildRegistry();
    var gateway = new VitruvOCLGatewayImpl(CONSTRAINT_FILE, PREPOST_CONSTRAINT_FILE);
    var coordinator = new ConstraintEvaluationCoordinator(registry, gateway);

    Set<Class<? extends Reaction>> firedReactions =
        Set.of(ComponentInsertedIntoSystemReaction.class);

    List<ViolatedConstraint> violations = coordinator.evaluateFor(firedReactions);

    assertTrue(
        violations.isEmpty(),
        () ->
            "pre/post are not yet evaluated by vitruvocl-language, so this must stay empty until"
                + " visitPreCS/visitPostCS are implemented upstream — see class Javadoc. Got: "
                + violations);
  }
}
