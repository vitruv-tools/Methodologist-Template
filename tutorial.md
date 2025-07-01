# Tutorial

For the following tutorial a basic understanding of the V-SUM is recommended.
We recommend reading our [wiki](https://github.com/vitruv-tools/.github/wiki) for more information.
First we will briefly explain how to edit the meta-models stored in `.ecore` files in the `model` folder.
For a deeper understanding on EMF and ecore we recommend the [Eclipse EMF Tutorial](https://www.vogella.com/tutorials/EclipseEMF/article.html) by Lars Vogel and the [model section](./README.md#model) in the README.

## Editing the Meta-Model

We recommend using the Eclipse Modeling Framework (EMF) to edit the meta-models.
Please make sure to have the EMF plugin installed in your Eclipse IDE.
The meta-models are located in the `model` folder of the project.
You can open the `.ecore` files in Eclipse and edit them using the EMF editor
or any other editor that supports ecore files.

When you're done editing the meta-model, you need to update the genmodel.
To do this, right-click on the `.genmodel` file and select `Reload...`.
Once the ecore and genmodel files are synchronized again, make sure their updated version are placed into the model folder.

Please open the existing `model.ecore` and `model2.ecore` files in the `model` folder and make yourself familiar with the meta-models.
As a simple first task, we want to expand the meta-model and add a new consistency rule that keeps the models consistent.

## Example Task 1

Our tasks will follow three simple steps:

1. **Editing the Meta-Model**: We will edit/expand meta-model.
2. **Keeping the Models Consistent**: We will ensure that the models are kept consistent by introducing consistency preservation rules written in the reactions language.
3. **Testing the Reactions**: We will write test cases that ensure that the reactions are working as expected and that the models are kept consistent.

The current code reflects the state of the project after the tutorial has been completed.
We encourage you to follow the steps and implement the tasks yourself or at least read through the code to understand how the V-SUM works.

In this simple first task we will add a new `Router` component to the meta-model and ensure that the reactions are working correctly.

### 1. Editing the Model

The `model.ecore` file defines a `component` and a `device` and `server` which extend the `component` .
Consider you're now a methodologist and you want to expand the meta-model to also support `Routers` which are also a `component` .
Add a new EClass called `Router` which also extends the `component`.

### 2. Keeping the Models Consistent

Once you have added the `Router` to the meta-model we now want to ensure that consistency is kept.
Have a look at the reactions located in the `consistency` folder.
Reactions are used to keep the models, meaning the concrete instances of the meta-model, consistent.
The `ComponentInsertedIntoSystem` reaction is responsible for creating a corresponding `Entity` for each `Component` that is inserted into the system.
When a `Component` is inserted into the instantiated system, the reaction is triggered in order to keep the model up to date.
Since the `Router` is a `Component` it will be matched by the reaction and a corresponding `Entity` will be created. Therefore we don't need to add a new reaction for the `Router`.
The reaction then calls a routine which restores the consistency.
Here the routine `createAndInsertEntity` is called.

Within the match block all corresponding elements are selected.
Then a new `Entity` is created. Afterwards all properties from the `Component` are set to the `Entity`.
The `Entity` is then inserted into the `Root`.
With the `addCorrespondenceBetween` method the `Component` and the `Entity` are linked.

### 3. Adding a Test Case

Now we want to make sure that the reaction is working correctly for or newly added `Router` component.

For this we can use the inspiration of the existing test case `insertComponent` and add a new test case for the `Router`.

The test case could look like this:

```java
@Test
   void insertRouter(@TempDir Path tempDir) {
       InternalVirtualModel vsum = createDefaultVirtualModel(tempDir);
       addSystem(vsum, tempDir);
       addRouter(vsum);
       Assertions.assertTrue(assertView(getDefaultView(vsum, List.of(System.class, Root.class)), (View v) -> {
       // assert that a component has been inserted, a entity has been created and that
       // both have the same name
       return v.getRootObjects(System.class).iterator().next()
           .getComponents().get(0).getName()
           .equals(v.getRootObjects(Root.class).iterator().next()
               .getEntities().get(0).getName());
       }));
   }

```

A `addRouter` helper method needs to be added that creates a `Router` and adds it to the system.
The method as well as the test case can be found in the `VSUMExampleTest.java` file.

## Example Task 2

Both ecore files specify a `Link` class.
We now want to keep these links consistent.

### 1. Editing the Model

In order to reflect the protocols and links in the model2 we add a `CommunicationsStandard` class to the second ecore file.
The `CommunicationsStandard` should have a property name of type `EString`.
Add a reference to the `Link` class within the model2.ecore of the newly created `CommunicationsStandard` class.
**Make sure to set the `Containment` property of the relation to `true` in the ecore file.**
This is important since all instances of classes of a meta-model must be contained.
Once you have saved these changes to the model, don't forget to update the genmodel.

### 2. Keeping the Models Consistent

We need to add four reactions (`LinkInsertedIntoSystem`, `ProtocolInsertedIntoSystem`, `ComponentInsertedIntoLink` and `ProtocolInsertedIntoSystem`) to the `templateReactions.reactions` file in order to keep the models consistent.

`LinkInsertedIntoSystem` and `ComponentInsertedIntoLink` work similarly to the `ComponentInsertedIntoSystem` reaction.
A corresponding Element is created in the second model and a correspondence is established between the two models.

`ComponentInsertedIntoLink` and `ProtocolInsertedIntoLink` are responsible for keeping the then already created corresponding `Link` in model2 consistent with the `Link` in model.
For this the corresponding elements are retrieved and the properties are set accordingly.

Having a look at the `ComponentInsertedIntoLink` reaction, we can see that it is triggered after a `Component` is inserted into a `Link`.

```java
   reaction ComponentInsertedIntoLink {
      after element model::Component inserted in model::Link[components]
      call updateLinkEntities(affectedEObject, newValue)
   }

   routine updateLinkEntities(model::Link link, model::Component component) {
      match {
         val mEntity = retrieve model2::Entity corresponding to component
         val mLink = retrieve model2::Link corresponding to link
      }
      update {
         mLink.entities.add(mEntity)
      }
   }
```

Then the corresponding `Entity` is retrieved from the second model and added to the `Link` in the second model.
The `ProtocolInsertedIntoSystem` reaction works similarly.

### 3. Adding a Test Case

Now we want to test if the reactions that we have created are working as expected.
We add two components, a`Protocol` and a `Link` to the first model and check that the corresponding `Entities`, `CommunicationsStandard` and `Link` are created as well as added to the `Link` in model2.

The test case which also can be found in the `VSUMExampleTest.java` file, looks like this:

```java
  @Test
  void testLink(@TempDir Path tempDir) {
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addSystem(vsum, tempDir);

    // add two components, protocol and add a link between them
    modifyView(getDefaultView(vsum, List.of(System.class)).withChangeDerivingTrait(), (CommittableView v) -> {

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
    Assertions.assertTrue(assertView(getDefaultView(vsum, List.of(Root.class)), (View v) -> {
      var root = v.getRootObjects(Root.class).iterator().next();
      return root.getLinks().size() == 1
          && root.getLinks().get(0).getEntities().size() == 2
          && root.getLinks().get(0).getEntities().stream()
              .allMatch(c -> c.getName().startsWith("component"));
    }));
  }
```

Once the test cases are added we can run the tests again and check that all tests are passing.
