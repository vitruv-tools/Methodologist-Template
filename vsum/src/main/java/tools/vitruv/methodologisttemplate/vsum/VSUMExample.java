package tools.vitruv.methodologisttemplate.vsum;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import mir.reactions.model2Model2.ComponentInsertedIntoSystemReaction;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.methodologisttemplate.consistency.registry.ContractLog;
import tools.vitruv.methodologisttemplate.consistency.registry.HookedModel2Model2ChangePropagationSpecification;
import tools.vitruv.methodologisttemplate.consistency.registry.ProjectReactionConstraints;
import tools.vitruv.methodologisttemplate.model.model.ModelFactory;
import tools.vitruv.methodologisttemplate.model.model.System;

/**
 * This class provides an example how to define and use a VSUM.
 *
 * <p>It also demonstrates the Reaction contracts (see the {@code consistency} module's {@code
 * registry} package): {@link #createDefaultVirtualModel} builds a {@link ContractGuardedVsum} with
 * {@link HookedModel2Model2ChangePropagationSpecification} -- the exact same mechanism {@code
 * VSUMExampleTest} uses. Every Reaction execution below is checked against its pre- and
 * postconditions, and every commit against all invariants. A violation is rejected (this example
 * has no user to ask), the commit is rolled back, and {@link
 * tools.vitruv.methodologisttemplate.consistency.registry.ConstraintViolationsDetectedException}
 * is thrown straight out of {@code main()}.
 */
public class VSUMExample {

  /**
   * The repository root, resolved independently of the caller's working directory.
   *
   * <p>{@code ./mvnw -pl vsum exec:java} run from the repo root leaves {@code user.dir} at the
   * root, but an IDE's "Run" code lens on {@code main()} (e.g. VS Code's Java extension) instead
   * {@code cd}s into this module's own directory ({@code vsum/}) before invoking Maven. Every path
   * below used to be built directly from {@code user.dir}, so it silently pointed at nonexistent
   * locations under {@code vsum/} whenever launched that second way.
   */
  private static final Path PROJECT_ROOT = resolveProjectRoot();

  /**
   * Paths to the OCL constraint files, located alongside the Reactions in the consistency module.
   */
  private static final Path CONSTRAINT_FILE =
      PROJECT_ROOT.resolve(
          "consistency/src/main/constraints/tools/vitruv/methodologisttemplate/consistency/constraints.ocl");

  private static final Path PREPOST_CONSTRAINT_FILE =
      PROJECT_ROOT.resolve(
          "consistency/src/main/constraints/tools/vitruv/methodologisttemplate/consistency/prepost-example.ocl");

  /** Storage folder for the VSUM; must be the exact same {@link Path} used for registerRoot. */
  private static final Path STORAGE_FOLDER = PROJECT_ROOT.resolve("vsumexample").toAbsolutePath();

  private static Path resolveProjectRoot() {
    // Deliberately checks for 'consistency' and 'model' -- committed module directories that
    // always exist -- rather than 'vsumexample', which is generated output this very class
    // creates on first run (gitignored, absent on a fresh clone), and so cannot double as a marker
    // for finding the root.
    Path candidate = Path.of("").toAbsolutePath();
    Path original = candidate;
    for (int depth = 0; depth < 3; depth++) {
      if (Files.isDirectory(candidate.resolve("consistency"))
          && Files.isDirectory(candidate.resolve("model"))) {
        return candidate;
      }
      Path parent = candidate.getParent();
      if (parent == null) {
        break;
      }
      candidate = parent;
    }
    throw new IllegalStateException(
        "Could not locate the project root (expected 'consistency' and 'model' as sibling "
            + "directories) starting from working directory: "
            + original);
  }

  public static void main(String[] args) throws IOException {
    // Required so EMF knows how to create/load a Resource for the .model file registered below;
    // without it, ResourceSet#createResource() returns null for unrecognized file extensions.
    Resource.Factory.Registry.INSTANCE
        .getExtensionToFactoryMap()
        .put("*", new XMIResourceFactoryImpl());

    // Forces the reactions-runtime correspondence metamodel's EPackage to register itself in
    // EPackage.Registry before we try to load an existing VSUM. On a fresh VSUM this happens
    // implicitly (creating a ReactionsCorrespondence touches the class anyway); when reloading a
    // VSUM that already has a correspondences.correspondence file on disk, buildAndInitialize()
    // below is the very first thing to touch that metamodel, and XMI parsing needs the
    // registration to already be there. Note: this is a *different* CorrespondencePackage class
    // than tools.vitruv.change.correspondence.CorrespondencePackage (same simple name).
    tools.vitruv.dsls.reactions.runtime.correspondence.CorrespondencePackage.eINSTANCE.eClass();

    ContractGuardedVsum vsum = createDefaultVirtualModel();

    CommittableView view = getDefaultView(vsum.vsum()).withChangeDerivingTrait();
    modifyView(
        vsum,
        view,
        (CommittableView v) -> {
          // SystemInsertedAsRootReaction fires here, but has no contract registered in
          // ProjectReactionConstraints, so there is nothing to check for it.
          // registerRoot (rather than getRootObjects().add()) is required here because this
          // System is a brand-new root: Vitruvius needs an explicit URI to know which resource
          // to persist it into.
          v.registerRoot(
              ModelFactory.eINSTANCE.createSystem(),
              URI.createFileURI(STORAGE_FOLDER.resolve("example.model").toString()));
        });
    java.lang.System.out.println(
        "System added. SystemInsertedAsRootReaction fired; it has no contract to check.");

    addComponent(vsum);
  }

  /**
   * Triggers {@code ComponentInsertedIntoSystemReaction}, which -- unlike {@code
   * SystemInsertedAsRootReaction} above -- does have a contract registered in {@code
   * ProjectReactionConstraints}, so its execution is actually checked here.
   *
   * <p>Reaching the {@code println} below at all already proves the contract and every invariant
   * held: otherwise the commit would have been rolled back and {@code
   * ConstraintViolationsDetectedException} thrown a few lines above. Rather than leave that as an
   * inference from the absence of an exception, this explicitly lists which {@link
   * tools.vitruv.methodologisttemplate.consistency.registry.ConstraintRef}s form the contract of the
   * Reaction that just ran -- i.e. exactly what got checked and passed.
   */
  private static void addComponent(ContractGuardedVsum guarded) {
    VirtualModel vsum = guarded.vsum();
    var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
    selector.getSelectableElements().stream()
        .filter(System.class::isInstance)
        .forEach(it -> selector.setSelected(it, true));
    CommittableView view = selector.createView().withChangeDerivingTrait();
    var component = ModelFactory.eINSTANCE.createComponent();
    component.setName("exampleComponent");
    view.getRootObjects(System.class).iterator().next().getComponents().add(component);
    modifyView(guarded, view, v -> {});

    var checkedConstraints =
        ProjectReactionConstraints.buildRegistry()
            .getConstraintsFor(ComponentInsertedIntoSystemReaction.class);
    java.lang.System.out.println(
        "Component added. ComponentInsertedIntoSystemReaction ran and was checked against its "
            + "contract of "
            + checkedConstraints.size()
            + " pre-/postcondition(s) -- all satisfied, and all invariants hold:");
    checkedConstraints.forEach(ref -> java.lang.System.out.println("  [OK] " + ref));
  }

  private static ContractGuardedVsum createDefaultVirtualModel() throws IOException {
    // Without a user to ask, a violated contract or invariant is rejected and rolled back.
    return ContractGuardedVsum.build(
        STORAGE_FOLDER,
        new TestUserInteraction.ResultProvider(new TestUserInteraction()),
        inconsistencies -> false,
        new ContractLog(),
        CONSTRAINT_FILE,
        PREPOST_CONSTRAINT_FILE);
  }

  private static View getDefaultView(VirtualModel vsum) {
    var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
    selector.getSelectableElements().forEach(it -> selector.setSelected(it, true));
    return selector.createView();
  }

  private static void modifyView(
      ContractGuardedVsum vsum,
      CommittableView view,
      Consumer<CommittableView> modificationFunction) {
    modificationFunction.accept(view);
    var outcome = vsum.commit(view);
    if (!outcome.applied()) {
      throw outcome.rejection();
    }
  }
}
