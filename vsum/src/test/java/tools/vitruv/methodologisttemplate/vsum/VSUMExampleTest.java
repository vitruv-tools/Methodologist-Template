package tools.vitruv.methodologisttemplate.vsum;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.dsls.vitruvocl.pipeline.VitruvOCL;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;
import tools.vitruv.methodologisttemplate.consistency.registry.ConstraintKind;
import tools.vitruv.methodologisttemplate.consistency.registry.ConstraintViolationsDetectedException;
import tools.vitruv.methodologisttemplate.consistency.registry.ContractLog;
import tools.vitruv.methodologisttemplate.consistency.registry.HookedModel2Model2ChangePropagationSpecification;
import tools.vitruv.methodologisttemplate.model.model.ModelFactory;
import tools.vitruv.methodologisttemplate.model.model.System;
import tools.vitruv.methodologisttemplate.model.model2.Root;

/**
 * This class provides an example how to define and use a VSUM.
 *
 * <p>It also demonstrates how to integrate VitruviusOCL for constraint-based consistency checking.
 * Constraints are defined in {@code constraints.ocl} and can be evaluated manually via
 * {@link VitruvOCL#evaluateConstraints(java.nio.file.Path)}. {@link #createDefaultVirtualModel}
 * additionally wires up the Reaction contracts (see the {@code consistency} module's {@code
 * registry} package): every VSUM created by every test in this class is a {@link
 * ContractGuardedVsum} with {@link HookedModel2Model2ChangePropagationSpecification}. Every Reaction
 * execution is checked against its pre- and postconditions in {@code prepost-example.ocl}, and
 * every commit against all invariants in {@code constraints.ocl}. A violation is rejected unless a
 * test answers otherwise, the commit is rolled back, and {@link #modifyView} throws a {@link
 * ConstraintViolationsDetectedException} naming the violated constraint -- in every test in this
 * file, not just the ones explicitly about contracts.
 */
public class VSUMExampleTest {

  /**
   * Path to the OCL constraint file, located alongside the Reactions in the consistency module.
   *
   * <p>Surefire forks the test JVM with the {@code vsum} module directory (not the repository
   * root) as the working directory, so this path must climb one level up first.
   */
  private static final java.nio.file.Path CONSTRAINT_FILE = java.nio.file.Path.of(
      "../consistency/src/main/constraints/tools/vitruv/methodologisttemplate/consistency/constraints.ocl");
  private static final java.nio.file.Path PREPOST_CONSTRAINT_FILE = java.nio.file.Path.of(
      "../consistency/src/main/constraints/tools/vitruv/methodologisttemplate/consistency/prepost-example.ocl");

  /** Answers the confirmation asked for a violated contract; empty means "unexpected". */
  private TestUserInteraction interaction;

  /** The VSUM of the current test; after a rollback, {@link ContractGuardedVsum#vsum()} changes. */
  private ContractGuardedVsum guarded;

  private ContractLog log;

  @BeforeAll
  static void setup() {
    Resource.Factory.Registry.INSTANCE
        .getExtensionToFactoryMap()
        .put("*", new XMIResourceFactoryImpl());
  }

  @Test
  void reloadEmptyVirtualModel(@TempDir Path tempDir) throws IOException {
    InternalVirtualModel vsum = createDefaultVirtualModel(tempDir);
    vsum.dispose();
    vsum = createDefaultVirtualModel(tempDir);
  }

  @Test
  void reloadFilledVirtualModel(@TempDir Path tempDir) throws IOException {
    InternalVirtualModel vsum = createDefaultVirtualModel(tempDir);
    addSystem(vsum, tempDir);
    vsum.dispose();
    vsum = createDefaultVirtualModel(tempDir);
    // Assert that the reloaded virtual model contains the changes we made before disposing it
    Assertions.assertEquals(1, getDefaultView(vsum, List.of(System.class)).getRootObjects().size());
    Assertions.assertEquals(1, getDefaultView(vsum, List.of(Root.class)).getRootObjects().size());
  }

  @Test
  void systemInsertionAndPropagationTest(@TempDir Path tempDir) throws IOException {
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addSystem(vsum, tempDir);
    // assert that the directly added System is present
    Assertions.assertEquals(1, getDefaultView(vsum, List.of(System.class)).getRootObjects().size());
    // as well as the Root that should be created by the Reactions, see
    // templateReactions.reactions#14
    Assertions.assertEquals(1, getDefaultView(vsum, List.of(Root.class)).getRootObjects().size());
  }

  @Test
  void insertComponent(@TempDir Path tempDir) throws IOException {
    InternalVirtualModel vsum = createDefaultVirtualModel(tempDir);
    addSystem(vsum, tempDir);
    addComponent(vsum);
    Assertions.assertTrue(
        assertView(
            getDefaultView(vsum, List.of(System.class, Root.class)),
            (View v) -> {
              // assert that a component has been inserted, a entity has been created and that
              // both have the same name
              // Note: to make the test result easier to understand, these different effects
              // should be tested one by one
              return v.getRootObjects(System.class)
                  .iterator()
                  .next()
                  .getComponents()
                  .get(0)
                  .getName()
                  .equals(
                      v.getRootObjects(Root.class)
                          .iterator()
                          .next()
                          .getEntities()
                          .get(0)
                          .getName());
            }));
  }

  @Test
  void insertRouter(@TempDir Path tempDir) throws IOException {
    InternalVirtualModel vsum = createDefaultVirtualModel(tempDir);
    addSystem(vsum, tempDir);
    addRouter(vsum);
    modifyView(
        getDefaultView(vsum, List.of(System.class)).withChangeDerivingTrait(),
        (CommittableView v) -> {
          // add a router to the system
          v.getRootObjects(System.class)
              .iterator()
              .next()
              .getComponents()
              .add(ModelFactory.eINSTANCE.createRouter());
        });
    Assertions.assertTrue(
        assertView(
            getDefaultView(vsum, List.of(System.class, Root.class)),
            (View v) -> {
              // assert that the router has been added and that the corresponding entity has
              // been created
              return v.getRootObjects(System.class)
                  .iterator()
                  .next()
                  .getComponents()
                  .get(0)
                  .getName()
                  .equals(
                      v.getRootObjects(Root.class)
                          .iterator()
                          .next()
                          .getEntities()
                          .get(0)
                          .getName());
            }));
  }

  @Test
  void renameComponent(@TempDir Path tempDir) throws IOException {
    final String newName = "newName";
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addSystem(vsum, tempDir);
    addComponent(vsum);
    modifyView(
        getDefaultView(vsum, List.of(System.class)).withChangeDerivingTrait(),
        (CommittableView v) -> {
          // change the name of the component
          v.getRootObjects(System.class).iterator().next().getComponents().get(0).setName(newName);
        });
    Assertions.assertTrue(
        assertView(
            getDefaultView(vsum, List.of(System.class, Root.class)),
            (View v) -> {
              // assert that the renaming worked on the component as well as the corresponding
              // entity
              return v.getRootObjects(System.class)
                      .iterator()
                      .next()
                      .getComponents()
                      .get(0)
                      .getName()
                      .equals(newName)
                  && v.getRootObjects(Root.class)
                      .iterator()
                      .next()
                      .getEntities()
                      .get(0)
                      .getName()
                      .equals(newName);
            }));
  }

  @Test
  void deleteComponent(@TempDir Path tempDir) throws IOException {
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addSystem(vsum, tempDir);
    addComponent(vsum);
    modifyView(
        getDefaultView(vsum, List.of(System.class)).withChangeDerivingTrait(),
        (CommittableView v) -> {
          v.getRootObjects(System.class).iterator().next().getComponents().remove(0);
        });
    Assertions.assertTrue(
        assertView(
            getDefaultView(vsum, List.of(System.class, Root.class)),
            (View v) -> {
              // assert that the deletion of the component worked and that the corresponding
              // entity also got deleted
              return v.getRootObjects(System.class).iterator().next().getComponents().isEmpty()
                  && v.getRootObjects(Root.class).iterator().next().getEntities().isEmpty();
            }));
  }

  @Test
  void testLink(@TempDir Path tempDir) throws IOException {
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addSystem(vsum, tempDir);

    // add two components, protocol and add a link between them
    modifyView(
        getDefaultView(vsum, List.of(System.class)).withChangeDerivingTrait(),
        (CommittableView v) -> {
          var system = v.getRootObjects(System.class).iterator().next();

          var component1 = ModelFactory.eINSTANCE.createComponent();
          component1.setName("component1");
          var component2 = ModelFactory.eINSTANCE.createComponent();
          component2.setName("component2");
          system.getComponents().addAll(List.of(component1, component2));

          var protocol = ModelFactory.eINSTANCE.createProtocol();
          protocol.setName("exampleProtocol");
          system.getProtocols().add(protocol);

          var link = ModelFactory.eINSTANCE.createLink();
          system.getLinks().add(link);
          link.setProtocol(protocol);
          link.getComponents().addAll(List.of(component1, component2));
        });

    // assert that the link has been created and that it is connected to the two
    // components
    Assertions.assertTrue(
        assertView(
            getDefaultView(vsum, List.of(Root.class)),
            (View v) -> {
              var root = v.getRootObjects(Root.class).iterator().next();
              return root.getLinks().size() == 1
                  && root.getLinks().get(0).getEntities().size() == 2
                  && root.getLinks().get(0).getEntities().stream()
                      .allMatch(c -> c.getName().startsWith("component"));
            }));
  }

  /**
   * Proves the invariant check after every commit, wired into {@link #createDefaultVirtualModel},
   * actually catches a real violation in this file. Deliberately commits a {@code model::Link} with
   * only one Component, which the invariant {@code LinkHasAtLeastTwoEntities} forbids -- unlike
   * {@link #testLink} above, which correctly adds two. Every Reaction involved satisfies its own
   * contract; only the system as a whole is inconsistent, so the commit is rolled back.
   */
  @Test
  void linkWithOnlyOneComponentFailsTheCommit(@TempDir Path tempDir) throws IOException {
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addSystem(vsum, tempDir);

    var exception =
        Assertions.assertThrows(
            ConstraintViolationsDetectedException.class,
            () ->
                modifyView(
                    getDefaultView(vsum, List.of(System.class)).withChangeDerivingTrait(),
                    (CommittableView v) -> {
                      var system = v.getRootObjects(System.class).iterator().next();

                      var component = ModelFactory.eINSTANCE.createComponent();
                      component.setName("onlyComponent");
                      system.getComponents().add(component);

                      var link = ModelFactory.eINSTANCE.createLink();
                      system.getLinks().add(link);
                      link.getComponents().add(component);
                    }));

    Assertions.assertTrue(
        exception.getMessage().contains("LinkHasAtLeastTwoEntities"),
        () -> "Expected the failure message to name the violated constraint. Got: " + exception.getMessage());
    Assertions.assertEquals(ConstraintKind.INVARIANT, exception.violations().get(0).kind());
    Assertions.assertTrue(
        assertView(
            getDefaultView(guarded.vsum(), List.of(System.class)),
            (View v) -> v.getRootObjects(System.class).iterator().next().getLinks().isEmpty()),
        "the commit must have been rolled back");
  }

  /**
   * Proves the precondition genuinely prevents {@code ComponentInsertedIntoSystemReaction} from
   * executing at all, rather than merely detecting the violation after the fact. Committing a
   * Component with a blank name violates {@code ComponentHasNameBeforeSync} (a {@code pre}
   * constraint in {@code prepost-example.ocl}); the user rejects it, so the Reaction does not run,
   * the commit is stopped and rolled back -- no Component in model and no Entity in model2.
   */
  @Test
  void componentWithBlankNameNeverTriggersTheReactionAtAll(@TempDir Path tempDir) throws IOException {
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addSystem(vsum, tempDir);
    interaction.addNextConfirmationInput(false); // "stop the commit"

    var exception =
        Assertions.assertThrows(
            ConstraintViolationsDetectedException.class,
            () ->
                modifyView(
                    getDefaultView(vsum, List.of(System.class)).withChangeDerivingTrait(),
                    (CommittableView v) -> {
                      var component = ModelFactory.eINSTANCE.createComponent();
                      component.setName("");
                      v.getRootObjects(System.class).iterator().next().getComponents().add(component);
                    }));

    String message = exception.getMessage();
    Assertions.assertTrue(
        message.contains("ComponentHasNameBeforeSync") && message.contains("was not executed"),
        () ->
            "Expected a precondition-guard failure naming ComponentHasNameBeforeSync, blocked "
                + "before execution. Got: "
                + message);

    Assertions.assertTrue(
        assertView(
            getDefaultView(guarded.vsum(), List.of(System.class, Root.class)),
            (View v) ->
                v.getRootObjects(Root.class).iterator().next().getEntities().isEmpty()
                    && v.getRootObjects(System.class).iterator().next().getComponents().isEmpty()),
        "ComponentInsertedIntoSystemReaction must never have executed and the commit must have "
            + "been rolled back -- no Entity in model2, no Component in model");
  }

  /**
   * If the user approves a violated precondition, the Reaction runs and the decision is logged.
   */
  @Test
  void componentWithBlankNameIsPropagatedWhenTheUserApproves(@TempDir Path tempDir)
      throws IOException {
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addSystem(vsum, tempDir);
    // Setting the blank name also triggers ComponentRenamed, whose contract has the same
    // precondition, so the user is asked once per Reaction execution.
    interaction.addNextConfirmationInput(true); // "execute anyway"
    interaction.addNextConfirmationInput(true);

    modifyView(
        getDefaultView(vsum, List.of(System.class)).withChangeDerivingTrait(),
        (CommittableView v) -> {
          var component = ModelFactory.eINSTANCE.createComponent();
          component.setName("");
          v.getRootObjects(System.class).iterator().next().getComponents().add(component);
        });

    Assertions.assertEquals(2, log.decisions().size());
    Assertions.assertTrue(log.decisions().stream().allMatch(d -> d.approved()));
    Assertions.assertTrue(
        log.decisions().stream()
            .allMatch(d -> d.violation().constraintName().equals("ComponentHasNameBeforeSync")));
    Assertions.assertTrue(
        assertView(
            getDefaultView(guarded.vsum(), List.of(Root.class)),
            (View v) -> v.getRootObjects(Root.class).iterator().next().getEntities().size() == 1));
  }

  /**
   * Demonstrates manual constraint evaluation using VitruviusOCL.
   *
   * <p>After inserting a Component and committing the change, the Reactions create a corresponding
   * Entity in model2. The OCL constraints in {@code constraints.ocl} are then evaluated manually
   * to verify that the VSUM is in a consistent state.
   */
  @Test
  void constraintsAreSatisfiedAfterComponentInsert(@TempDir Path tempDir) throws IOException {
    InternalVirtualModel vsum = createDefaultVirtualModel(tempDir);
    addSystem(vsum, tempDir);
    addComponent(vsum);

    // Register the VSUM so VitruviusOCL can access model instances and correspondences.
    VitruvOCL.registerVSUM(vsum);

    var result = VitruvOCL.evaluateConstraints(CONSTRAINT_FILE);
    Assertions.assertTrue(
        result.allSatisfied(),
        () -> {
          var problems = new java.util.ArrayList<>(result.getViolatedConstraints());
          problems.addAll(result.getFailedConstraints());
          return "Expected all OCL constraints to be satisfied after component insertion ("
              + result.getSummary()
              + "):\n"
              + problems.stream().map(Object::toString).collect(java.util.stream.Collectors.joining("\n"))
              + "\n";
        });
  }

  private void addSystem(VirtualModel vsum, Path projectPath) {
    CommittableView view = getDefaultView(vsum, List.of(System.class)).withChangeDerivingTrait();
    modifyView(
        view,
        (CommittableView v) -> {
          v.registerRoot(
              ModelFactory.eINSTANCE.createSystem(),
              URI.createFileURI(projectPath.toString() + "/example.model"));
        });
  }

  private void addComponent(VirtualModel vsum) {
    CommittableView view = getDefaultView(vsum, List.of(System.class)).withChangeDerivingTrait();
    modifyView(
        view,
        (CommittableView v) -> {
          var component = ModelFactory.eINSTANCE.createComponent();
          component.setName("specialname");
          v.getRootObjects(System.class).iterator().next().getComponents().add(component);
        });
  }

  private void addRouter(VirtualModel vsum) {
    CommittableView view = getDefaultView(vsum, List.of(System.class)).withChangeDerivingTrait();
    modifyView(
        view,
        (CommittableView v) -> {
          var component = ModelFactory.eINSTANCE.createRouter();
          component.setName("specialRouterName");
          v.getRootObjects(System.class).iterator().next().getComponents().add(component);
        });
  }

  private InternalVirtualModel createDefaultVirtualModel(Path projectPath) throws IOException {
    interaction = new TestUserInteraction();
    log = new ContractLog();
    // Without a user to ask, a violated invariant after a commit is rejected and rolled back.
    guarded =
        ContractGuardedVsum.build(
            projectPath,
            new TestUserInteraction.ResultProvider(interaction),
            inconsistencies -> false,
            log,
            CONSTRAINT_FILE,
            PREPOST_CONSTRAINT_FILE);
    return guarded.vsum();
  }

  // See https://github.com/vitruv-tools/Vitruv/issues/717 for more information
  // about the rootTypes
  private View getDefaultView(VirtualModel vsum, Collection<Class<?>> rootTypes) {
    var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
    selector.getSelectableElements().stream()
        .filter(element -> rootTypes.stream().anyMatch(it -> it.isInstance(element)))
        .forEach(it -> selector.setSelected(it, true));
    return selector.createView();
  }

  // These functions are only for convience, as they make the code a bit better
  // readable
  private void modifyView(CommittableView view, Consumer<CommittableView> modificationFunction) {
    modificationFunction.accept(view);
    var outcome = guarded.commit(view);
    if (!outcome.applied()) {
      throw outcome.rejection();
    }
  }

  private boolean assertView(View view, Function<View, Boolean> viewAssertionFunction) {
    return viewAssertionFunction.apply(view);
  }
}
