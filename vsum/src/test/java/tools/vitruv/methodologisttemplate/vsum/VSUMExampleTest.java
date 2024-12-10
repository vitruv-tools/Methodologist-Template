package tools.vitruv.methodologisttemplate.vsum;

import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.methodologisttemplate.model.model.ModelFactory;

import java.nio.file.Path;
import java.util.function.Consumer;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import mir.reactions.model2Model2.Model2Model2ChangePropagationSpecification;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;

/**
 * This class provides an example how to define and use a VSUM.
 */
public class VSUMExampleTest {

  static final Path projectPath = Path.of("target/vsumexample");

  @BeforeAll
  static void setup() {
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("model", new XMIResourceFactoryImpl());
  }

  @Test
  void test() {
    VirtualModel vsum = createDefaultVirtualModel();
    CommittableView view = getDefaultView(vsum).withChangeDerivingTrait();
    modifyView(view, (CommittableView v) -> {
      v.registerRoot(
        ModelFactory.eINSTANCE.createSystem(),
        URI.createURI(projectPath.resolve("example.model").toString()));
    });
    Assertions.assertFalse(getDefaultView(vsum).getRootObjects().isEmpty(),"Modification of view failed");
  }

  private VirtualModel createDefaultVirtualModel() {
    return new VirtualModelBuilder()
        .withStorageFolder(projectPath)
        .withUserInteractorForResultProvider(new TestUserInteraction.ResultProvider(new TestUserInteraction()))
        .withChangePropagationSpecifications(new Model2Model2ChangePropagationSpecification())
        .buildAndInitialize();
  }

  private View getDefaultView(VirtualModel vsum) {
    var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
    selector.getSelectableElements().forEach(it -> selector.setSelected(it, true));
    return selector.createView();
  }

  private void modifyView(CommittableView view, Consumer<CommittableView> modificationFunction) {
    modificationFunction.accept(view);
    view.commitChanges();
  }

}
