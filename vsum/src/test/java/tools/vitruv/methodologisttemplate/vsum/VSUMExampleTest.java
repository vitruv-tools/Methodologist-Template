package tools.vitruv.methodologisttemplate.vsum;

import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.methodologisttemplate.model.model.ModelFactory;
import tools.vitruv.methodologisttemplate.model.model2.Root;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import mir.reactions.model2Model2.Model2Model2ChangePropagationSpecification;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.methodologisttemplate.model.model.System;

/**
 * This class provides an example how to define and use a VSUM.
 */
public class VSUMExampleTest {

  static final Path projectPath = Path.of("target/vsumexample");

  @BeforeAll
  static void setup() {
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("model", new XMIResourceFactoryImpl());
  }

  @AfterEach
  void clean() {
    try (Stream<Path> walk = Files.walk(Path.of(new File("").getAbsolutePath() + "/target/vsumexample"))) {
      walk.sorted(Comparator.reverseOrder())
          .forEach(path -> {
            try {
              Files.delete(path);
            } catch (IOException e) {
              e.printStackTrace();
            }
          });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  void systemInsertionAndPropagationTest() {
    VirtualModel vsum = createDefaultVirtualModel();
    addSystem(vsum);
    Assertions.assertEquals(2, getDefaultView(vsum).getRootObjects().size(),
        "Modification of view and propagation of changes failed");
    Assertions.assertTrue(assertView(getDefaultView(vsum), (View v) -> {
      return v.getRootObjects(System.class).size() == 1 && v.getRootObjects(Root.class).size() == 1;
    }));
  }

  void insertComponent() {
    VirtualModel vsum = createDefaultVirtualModel();
    addSystem(vsum);
    addComponent(vsum);
    Assertions.assertTrue(assertView(getDefaultView(vsum), (View v) -> {
      // assert that a component has been inserted, a entity has been created and that both have the same name
      return v.getRootObjects(System.class).iterator().next().getComponents().get(0).getName().equals(v.getRootObjects(Root.class).iterator().next().getEntities().get(0).getName());
    }));
  }

  void renameComponent() {
    final String newName = "newName";
    VirtualModel vsum = createDefaultVirtualModel();
    addSystem(vsum);
    addComponent(vsum);
    modifyView(getDefaultView(vsum).withChangeDerivingTrait(), (CommittableView v) -> {
      v.getRootObjects(System.class).iterator().next().getComponents().get(0).setName(newName);
    });
    Assertions.assertTrue(assertView(getDefaultView(vsum), (View v) -> {
      // assert that the renaming worked on the component as well as the corresponding entity
      return v.getRootObjects(System.class).iterator().next().getComponents().get(0).getName().equals(newName) && v.getRootObjects(Root.class).iterator().next().getEntities().get(0).getName().equals(newName);
    }));
  }

  void deleteComponent() {
    VirtualModel vsum = createDefaultVirtualModel();
    addSystem(vsum);
    addComponent(vsum);
    modifyView(getDefaultView(vsum).withChangeDerivingTrait(), (CommittableView v) -> {
      v.getRootObjects(System.class).iterator().next().getComponents().remove(0);
    });
    Assertions.assertTrue(assertView(getDefaultView(vsum), (View v) -> {
      // assert that the deletion of the component worked and that the corresponding entity also got deleted
      return v.getRootObjects(System.class).iterator().next().getComponents().isEmpty() && v.getRootObjects(Root.class).iterator().next().getEntities().isEmpty();
    }));
  }

  private boolean assertView(View view, Function<View, Boolean> viewAssertionFunction) {
    return viewAssertionFunction.apply(view);
  }

  private void addSystem(VirtualModel vsum) {
    CommittableView view = getDefaultView(vsum).withChangeDerivingTrait();
    modifyView(view, (CommittableView v) -> {
      v.registerRoot(
          ModelFactory.eINSTANCE.createSystem(),
          URI.createURI(projectPath.resolve("example.model").toString()));
    });
  }

  private void addComponent(VirtualModel vsum) {
    CommittableView view = getDefaultView(vsum).withChangeDerivingTrait();
    modifyView(view, (CommittableView v) -> {
      var component = ModelFactory.eINSTANCE.createComponent();
      component.setName("specialname");
      v.getRootObjects(System.class).iterator().next().getComponents().add(component);
    });
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
