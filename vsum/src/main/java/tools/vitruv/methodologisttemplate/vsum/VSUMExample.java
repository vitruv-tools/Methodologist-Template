package tools.vitruv.methodologisttemplate.vsum;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;
import mir.reactions.model2Model2.Model2Model2ChangePropagationSpecification;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import tools.vitruv.change.atomic.uuid.Uuid;
import tools.vitruv.change.composite.description.PropagatedChange;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.composite.propagation.ChangePropagationListener;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.dsls.vitruvocl.pipeline.VitruvOCL;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.methodologisttemplate.model.model.ModelFactory;

/**
 * This class provides an example how to define and use a VSUM.
 *
 * <p>It also demonstrates how to integrate VitruviusOCL for automatic constraint evaluation.
 * Constraints are defined in {@code constraints.ocl} and evaluated after every change propagation
 * via a {@link ChangePropagationListener}.
 */
public class VSUMExample {

  /** Path to the OCL constraint file, located alongside the Reactions in the consistency module. */
  private static final Path CONSTRAINT_FILE =
      Path.of(
          "consistency/src/main/constraints/tools/vitruv/methodologisttemplate/consistency/constraints.ocl");

  /** Storage folder for the VSUM; must be the exact same {@link Path} used for registerRoot. */
  private static final Path STORAGE_FOLDER = Path.of("vsumexample").toAbsolutePath();

  public static void main(String[] args) throws IOException {
    // Required so EMF knows how to create/load a Resource for the .model file registered below;
    // without it, ResourceSet#createResource() returns null for unrecognized file extensions.
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());

    // Forces the reactions-runtime correspondence metamodel's EPackage to register itself in
    // EPackage.Registry before we try to load an existing VSUM. On a fresh VSUM this happens
    // implicitly (creating a ReactionsCorrespondence touches the class anyway); when reloading a
    // VSUM that already has a correspondences.correspondence file on disk, buildAndInitialize()
    // below is the very first thing to touch that metamodel, and XMI parsing needs the
    // registration to already be there. Note: this is a *different* CorrespondencePackage class
    // than tools.vitruv.change.correspondence.CorrespondencePackage (same simple name).
    tools.vitruv.dsls.reactions.runtime.correspondence.CorrespondencePackage.eINSTANCE.eClass();

    VirtualModel vsum = createDefaultVirtualModel();

    // Register the VSUM with VitruviusOCL so that evaluateConstraints() can access it.
    // This only needs to be done once per VSUM instance.
    VitruvOCL.registerVSUM(vsum);

    // Register a ChangePropagationListener to automatically evaluate constraints
    // after every commitChanges() call. Vitruvius calls finishedChangePropagation()
    // internally once all Reactions have been executed.
    vsum.addChangePropagationListener(
        new ChangePropagationListener() {
          @Override
          public void startedChangePropagation(VitruviusChange<Uuid> changeToPropagate) {
            // nothing to do before propagation
          }

          @Override
          public void finishedChangePropagation(Iterable<PropagatedChange> propagatedChanges) {
            // Evaluate all constraints in the .ocl file against the current VSUM state.
            // The result indicates per constraint whether it is satisfied or violated.
            var result = VitruvOCL.evaluateConstraints(CONSTRAINT_FILE);
            result
                .getResults()
                .forEach(
                    entry -> {
                      if (!entry.isSatisfied()) {
                        java.lang.System.err.println(
                            "[OCL VIOLATION] "
                                + entry.getConstraint()
                                + ": "
                                + entry.getWarningsSummary());
                      }
                    });
          }
        });

    CommittableView view = getDefaultView(vsum).withChangeDerivingTrait();
    modifyView(
        view,
        (CommittableView v) -> {
          // After this commit, Reactions fire and then the ChangePropagationListener
          // automatically evaluates the constraints defined in constraints.ocl.
          // registerRoot (rather than getRootObjects().add()) is required here because this
          // System is a brand-new root: Vitruvius needs an explicit URI to know which resource
          // to persist it into.
          v.registerRoot(
              ModelFactory.eINSTANCE.createSystem(),
              URI.createFileURI(STORAGE_FOLDER.resolve("example.model").toString()));
        });
  }

  private static VirtualModel createDefaultVirtualModel() throws IOException {
    return new VirtualModelBuilder()
        .withStorageFolder(STORAGE_FOLDER)
        .withUserInteractorForResultProvider(
            new TestUserInteraction.ResultProvider(new TestUserInteraction()))
        .withChangePropagationSpecifications(new Model2Model2ChangePropagationSpecification())
        .buildAndInitialize();
  }

  private static View getDefaultView(VirtualModel vsum) {
    var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
    selector.getSelectableElements().forEach(it -> selector.setSelected(it, true));
    return selector.createView();
  }

  private static void modifyView(
      CommittableView view, Consumer<CommittableView> modificationFunction) {
    modificationFunction.accept(view);
    view.commitChanges();
  }
}
