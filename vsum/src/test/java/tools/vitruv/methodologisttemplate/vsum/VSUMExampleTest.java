package tools.vitruv.methodologisttemplate.vsum;

import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;
import tools.vitruv.methodologisttemplate.model.model.ModelFactory;
import tools.vitruv.methodologisttemplate.model.model.ModelPackage;
import tools.vitruv.methodologisttemplate.model.model2.Model2Package;
import tools.vitruv.methodologisttemplate.model.model2.Root;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import mir.reactions.model2Model2.Model2Model2ChangePropagationSpecification;
import tools.vitruv.change.propagation.ChangePropagationMode;
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

  // static final Path projectPath = Path.of("target/vsumexample");

  @BeforeAll
  static void setup() {
    // ------------------------------------- IMPORTANT ----------------------------//
    // Register the appropriate ResourceFactory which is used by the ChangeDerivingViewType to create an initial copy of the
    // Resources that are part of the View. Uncomment the line if you want to set a specific ResourceFactory for your specific
    // Metamodel.
    // Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("model", new XMIResourceFactoryImpl());
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
    EPackage.Registry.INSTANCE.put(ModelPackage.eNS_URI, ModelPackage.eINSTANCE);
    EPackage.Registry.INSTANCE.put(Model2Package.eNS_URI, Model2Package.eINSTANCE);
    EcorePlugin.ExtensionProcessor.process(null);
  }

  // @AfterEach
  // void clean() {
  //   final Path folder = Path.of(new File("").getAbsolutePath() + "/" + projectPath.toString());
  //   try (Stream<Path> walk = Files.walk(folder)) {
  //     walk.sorted(Comparator.reverseOrder())
  //       .filter(path -> !path.equals(folder))
  //         .forEach(path -> {
  //           try {
  //             Files.delete(path);
  //           } catch (IOException e) {
  //             e.printStackTrace();
  //           }
  //         });
  //   } catch (IOException e) {
  //     e.printStackTrace();
  //   }
  // }

  @Test
  void systemInsertionAndPropagationTest(@TempDir Path tempDir) {
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addSystem(vsum, tempDir);
    Assertions.assertEquals(2, getDefaultView(vsum, List.of(System.class, Root.class)).getRootObjects().size(),
        "Modification of view and propagation of changes failed");
    Assertions.assertTrue(assertView(getDefaultView(vsum, List.of(System.class, Root.class)), (View v) -> {
      return v.getRootObjects(System.class).size() == 1 && v.getRootObjects(Root.class).size() == 1;
    }));
  }

  @Test
  void insertComponent(@TempDir Path tempDir) {
    InternalVirtualModel vsum = createDefaultVirtualModel(tempDir);
    addSystem(vsum, tempDir);
    addComponent(vsum);
    Assertions.assertTrue(assertView(getDefaultView(vsum, List.of(System.class, Root.class)), (View v) -> {
      // assert that a component has been inserted, a entity has been created and that both have the same name
      return v.getRootObjects(System.class).iterator().next().getComponents().get(0).getName().equals(v.getRootObjects(Root.class).iterator().next().getEntities().get(0).getName());
    }));
  }

  @Test
  void renameComponent(@TempDir Path tempDir) {
    final String newName = "newName";
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addSystem(vsum, tempDir);
    addComponent(vsum);
    modifyView(getDefaultView(vsum, List.of(System.class)).withChangeDerivingTrait(), (CommittableView v) -> {
      v.getRootObjects(System.class).iterator().next().getComponents().get(0).setName(newName);
    });
    Assertions.assertTrue(assertView(getDefaultView(vsum, List.of(System.class, Root.class)), (View v) -> {
      // assert that the renaming worked on the component as well as the corresponding entity
      return v.getRootObjects(System.class).iterator().next().getComponents().get(0).getName().equals(newName) && v.getRootObjects(Root.class).iterator().next().getEntities().get(0).getName().equals(newName);
    }));
  }

  @Test
  void deleteComponent(@TempDir Path tempDir) {
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addSystem(vsum, tempDir);
    addComponent(vsum);
    modifyView(getDefaultView(vsum, List.of(System.class)).withChangeDerivingTrait(), (CommittableView v) -> {
      v.getRootObjects(System.class).iterator().next().getComponents().remove(0);
    });
    Assertions.assertTrue(assertView(getDefaultView(vsum, List.of(System.class, Root.class)), (View v) -> {
      // assert that the deletion of the component worked and that the corresponding entity also got deleted
      return v.getRootObjects(System.class).iterator().next().getComponents().isEmpty() && v.getRootObjects(Root.class).iterator().next().getEntities().isEmpty();
    }));
  }

  private boolean assertView(View view, Function<View, Boolean> viewAssertionFunction) {
    return viewAssertionFunction.apply(view);
  }

  private void addSystem(VirtualModel vsum, Path projectPath) {
    CommittableView view = getDefaultView(vsum, List.of(System.class)).withChangeDerivingTrait();
    modifyView(view, (CommittableView v) -> {
      v.registerRoot(
          ModelFactory.eINSTANCE.createSystem(),
          URI.createFileURI(projectPath.toString() + "/example.model"));
    });

  }

  private void addComponent(VirtualModel vsum) {
    CommittableView view = getDefaultView(vsum, List.of(System.class)).withChangeDerivingTrait();
    modifyView(view, (CommittableView v) -> {
      var component = ModelFactory.eINSTANCE.createComponent();
      component.setName("specialname");
      v.getRootObjects(System.class).iterator().next().getComponents().add(component);
    });
  }

  private InternalVirtualModel createDefaultVirtualModel(Path projectPath) {
    InternalVirtualModel model = new VirtualModelBuilder()
        .withStorageFolder(projectPath)
        .withUserInteractorForResultProvider(new TestUserInteraction.ResultProvider(new TestUserInteraction()))
        .withChangePropagationSpecifications(new Model2Model2ChangePropagationSpecification())
        .buildAndInitialize();
    model.setChangePropagationMode(ChangePropagationMode.TRANSITIVE_CYCLIC);
    return model;
  }

  private View getDefaultView(VirtualModel vsum, Collection<Class<?>> rootTypes) {
    var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
    selector.getSelectableElements().stream()
      .filter(element -> rootTypes.stream().anyMatch(it -> it.isInstance(element)))
      .forEach(it -> selector.setSelected(it, true));
    return selector.createView();
  }

  private void modifyView(CommittableView view, Consumer<CommittableView> modificationFunction) {
    modificationFunction.accept(view);
    view.commitChanges();
  }

}
