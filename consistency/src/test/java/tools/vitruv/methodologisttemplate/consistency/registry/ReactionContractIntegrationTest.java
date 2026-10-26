package tools.vitruv.methodologisttemplate.consistency.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.change.propagation.ChangePropagationMode;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.dsls.vitruvocl.pipeline.VitruvOCL;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.ViewSource;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;
import tools.vitruv.methodologisttemplate.model.model.ModelFactory;
import tools.vitruv.methodologisttemplate.model.model.System;
import tools.vitruv.methodologisttemplate.model.model2.Root;

/**
 * Checks the contracts of the template's Reactions against a real VSUM: every execution of a
 * Reaction is checked against its pre- and postconditions, and a violation is decided by the user.
 * Restoring the models after a rejected violation, and checking the invariants after the commit,
 * is {@code ContractGuardedVsum}'s job in the {@code vsum} module.
 */
class ReactionContractIntegrationTest {

  private static final Path CONSTRAINT_FILE =
      Path.of("src/main/constraints/tools/vitruv/methodologisttemplate/consistency/constraints.ocl");
  private static final Path PREPOST_CONSTRAINT_FILE =
      Path.of("src/main/constraints/tools/vitruv/methodologisttemplate/consistency/prepost-example.ocl");

  private TestUserInteraction interaction;
  private ContractLog log;
  private InternalVirtualModel vsum;

  @BeforeAll
  static void setupResourceFactory() {
    Resource.Factory.Registry.INSTANCE
        .getExtensionToFactoryMap()
        .put("*", new XMIResourceFactoryImpl());
  }

  @AfterEach
  void teardown() {
    VitruvOCL.clearVSUM();
  }

  private void createVsum(Path tempDir) throws IOException {
    interaction = new TestUserInteraction();
    log = new ContractLog();
    var specification = new HookedModel2Model2ChangePropagationSpecification();
    vsum =
        new VirtualModelBuilder()
            .withStorageFolder(tempDir)
            .withUserInteractorForResultProvider(new TestUserInteraction.ResultProvider(interaction))
            .withChangePropagationSpecifications(specification)
            .buildAndInitialize();
    vsum.setChangePropagationMode(ChangePropagationMode.TRANSITIVE_CYCLIC);
    VitruvOCL.registerVSUM(vsum);
    var coordinator =
        new ConstraintEvaluationCoordinator(
            ProjectReactionConstraints.buildRegistry(),
            new VitruvOCLGatewayImpl(CONSTRAINT_FILE, PREPOST_CONSTRAINT_FILE));
    InternalVirtualModel model = vsum;
    specification
        .getContractEnforcer()
        .bind(
            coordinator,
            log,
            () ->
                ((ViewSource) model)
                    .getViewSourceModels().stream()
                        .map(Resource::getResourceSet)
                        .filter(java.util.Objects::nonNull)
                        .findFirst()
                        .orElse(null));

    CommittableView view =
        vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"))
            .createView()
            .withChangeDerivingTrait();
    view.registerRoot(
        ModelFactory.eINSTANCE.createSystem(),
        URI.createFileURI(tempDir.toString() + "/example.model"));
    view.commitChanges();
  }

  private void modifySystem(Consumer<System> modification) {
    var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
    selector.getSelectableElements().stream()
        .filter(System.class::isInstance)
        .forEach(e -> selector.setSelected(e, true));
    CommittableView view = selector.createView().withChangeDerivingTrait();
    modification.accept(view.getRootObjects(System.class).iterator().next());
    view.commitChanges();
  }

  private int entityCount() {
    var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
    selector.getSelectableElements().stream()
        .filter(Root.class::isInstance)
        .forEach(e -> selector.setSelected(e, true));
    return selector.createView().getRootObjects(Root.class).iterator().next().getEntities().size();
  }

  private static void addComponent(System system, String name) {
    var component = ModelFactory.eINSTANCE.createComponent();
    component.setName(name);
    system.getComponents().add(component);
  }

  /**
   * The same Reaction is executed once per inserted Component. Its postconditions only speak about
   * what the respective execution created, so the second Component, not yet propagated when the
   * first execution is checked, causes no violation.
   */
  @Test
  void eachExecutionOfAReactionIsCheckedOnItsOwn(@TempDir Path tempDir) throws IOException {
    createVsum(tempDir);

    modifySystem(system -> {
      addComponent(system, "first");
      addComponent(system, "second");
    });

    assertEquals(2, entityCount());
    assertTrue(log.decisions().isEmpty(), () -> "no contract may be violated: " + log.decisions());
  }

  @Test
  void aPostconditionIsCheckedAgainstTheChangesOfTheExecution(@TempDir Path tempDir)
      throws IOException {
    createVsum(tempDir);
    modifySystem(system -> addComponent(system, "before"));

    modifySystem(system -> system.getComponents().get(0).setName("after"));
    modifySystem(system -> system.getComponents().remove(0));

    assertEquals(0, entityCount());
    assertTrue(log.decisions().isEmpty(), () -> "no contract may be violated: " + log.decisions());
  }

  @Test
  void aRejectedPreconditionViolationStopsTheCommitBeforeTheReactionRuns(@TempDir Path tempDir)
      throws IOException {
    createVsum(tempDir);
    interaction.addNextConfirmationInput(false);

    // Vitruvius wraps whatever propagates out of Reaction.execute() in a RuntimeException.
    var wrapper =
        assertThrows(RuntimeException.class, () -> modifySystem(system -> addComponent(system, "")));

    var rejection = assertInstanceOf(ConstraintViolationsDetectedException.class, wrapper.getCause());
    assertTrue(rejection.getMessage().contains("was not executed"), rejection.getMessage());
    assertEquals("ComponentHasNameBeforeSync", rejection.violations().get(0).constraintName());
    assertEquals(1, log.decisions().size());
    assertFalse(log.decisions().get(0).approved());
  }

  @Test
  void anApprovedPreconditionViolationLetsTheReactionRunAndIsLogged(@TempDir Path tempDir)
      throws IOException {
    createVsum(tempDir);
    // Setting the blank name also triggers ComponentRenamed, whose contract has the same
    // precondition, so the user is asked once per Reaction execution.
    interaction.addNextConfirmationInput(true);
    interaction.addNextConfirmationInput(true);

    modifySystem(system -> addComponent(system, ""));

    assertEquals(1, entityCount(), "the Reaction must have run after the approval");
    assertEquals(
        List.of(true, true), log.decisions().stream().map(ContractLog.Decision::approved).toList());
    assertTrue(
        log.decisions().stream()
            .allMatch(d -> d.violation().kind() == ConstraintKind.PRECONDITION
                && d.violation().constraintName().equals("ComponentHasNameBeforeSync")));
  }
}
