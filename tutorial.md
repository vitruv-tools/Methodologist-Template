## Tutorial

For the following example make yourself familiar with the meta-model. The meta-model is located in the `model` folder for more information read the [Model](#model) section.

### Editing the Model and Testing Reactions

1. **Editing the Meta-Model** \
   The `model.ecore` file defines a `component` and a `device` and `server` which extend the `component` .
   Consider you're now a methodologist and you want to expand the meta-model to also support `Routers` which are also a `component` .
   Add a new `component` called `Router` which also extends the `component` .

2. **Keeping the Models Consistent** \
   Once you have added the `Router` to the meta-model we now want to ensure that
   consistency is kept. Have a look at the reactions located in the `consistency` folder.
   Reactions are used to keep the models, meaning the concrete instances of the meta-model, consistent.
   The `ComponentInsertedIntoSystem` reaction is responsible for creating a corresponding `Entity` for each `Component` that is inserted into the system.

   When a `Component` is inserted into the instantiated system, the reaction is triggered in order to keep the model up to date.
   It then calles a routine which restores the consistency.
   Here the routine `createAndInsertEntity` is called. With the `match` block all `components` without a matching `Entity` are selected.
   Then a new `Entity` is created. Afterwards all properties from the `Component` are set to the `Entity` .
   The `Entity` is then inserted into the `Root` . With the `addCorrespondenceBetween` method the `Component` and the `Entity` are linked.

   Since the `Router` is a `Component` it will be matched by the reaction and a corresponding `Entity` will be created. Therefore we don't need to add a new reaction for the `Router` .

3. **Adding a Test Case** \
   Now we want to ensure that the `Router` is correctly inserted into the system and that the reaction is triggered.
   For this we can use the existing test case `insertComponent` and add a new test case for the `Router` .  
   We also need to add another helper method `addRouter` that creates a `Router` and adds it to the system. For reference have a look at the `addComponent` method.
   The test case with the `addRouter` method should look like this:

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

   This testcase asserts that a `Router` has been inserted into the system and an `Entity` has been created. It also checks that both have the same name.

### Writing a simple Reaction

In order to later be able to keep the links consistent we now want to add `Protocol` to the second ecore file.
This we will then keep consistent with the `Protocol` in the first ecore file using a reaction.

1. **Updating the Meta-Model** \
   Add a `Protocol` class to the second ecore file.
   The `Protocol` should have a property name of type `EString` .
   Note that all Instances of classes of a meta-model must be contained.
   Therefore update the `Root` class to contain a list of `Protocol` objects.
   Make sure to set the `Containment` property of the relation to `true` in the ecore file.
   Once you have saved these changes to the model, don't forget to update the genmodel.

2. **Creating a Reaction** \
   Strongly inspired by the already existing `ComponentInsertedIntoSystem` reaction,
   we now want to create a reaction that creates a `Protocol` and adds it to the `Root` .
   The reaction should be triggered when a `Protocol` is inserted into the system.
   The reaction should look like this:

   ```java
   reaction ProtocolInsertedIntoSystem {
       after element model::Protocol inserted in model::System[protocols]
       call createAndInsertProtocol(affectedEObject, newValue)
   }

   routine createAndInsertProtocol(model::System system, model::Protocol protocol) {
       match {
           require absence of model2::Protocol corresponding to protocol
           // retrieve the mRoot we added a correspondence in the createAndRegisterRoot routine in the update block (line 33 in this file)
           val mRoot = retrieve model2::Root corresponding to system
       }
       create {
           val mProtocol = new model2::Protocol
       }
       update {
       mProtocol.name = protocol.name
       mRoot.protocols.add(mProtocol)
           addCorrespondenceBetween(protocol, mProtocol)
       }
   }
   ```

3. **Adding a Test Case** \
   Now we want to ensure that the `Protocol` is correctly inserted into the system and that the reaction is triggered.
   For this we can use the existing test case `insertComponent` and add a new test case for the `Protocol` .  
   We also need to add another helper method `addProtocol` that creates a `Protocol` and adds it to the system. For reference have a look at the `addComponent` method.

Once you have done this, you can run the tests again and check that all tests are passing.

### Writing a more complex Reaction

In the previous section we have seen how to create a simple reaction that is triggered when a `Protocol` is inserted into the system.
Now we want to keep the links between the two meta-models consistent.

1. **Updating the models** \
   Both ecore files specify a `Link` class.
   The ecore files need to be adjusted. Add a property called `name` of type `EString` to both `Link` classes (in each ecore file).
   Again all Links need to be contained in the model.
   Therefore a property with `Containment` enabled called `links` of type `Link` needs to be added to the `Root` class in `model.ecore`.
   The `Link` class within the model.ecore specifies that each `Link` has a also a `Protocol` .
   Since we have already added a `Protocol` to the second ecore file, we just need to also add this reference to the `Link` class in the second ecore file.
   Note that the multiplicity of the `Protocol` reference is `1..1`.
   After updating the ecore files, don't forget to update the genmodel.

2. **Creating a Reaction** \
   Now we want to create the reactions that keep the links consistent.
   Yes there are multiple reactions needed.
   As a first thought one might think that we can create a link object (belonging to the first ecore file) add a protocol and components and then write just one reaction that keeps this link consistent.
   The problem that we run into is that the entities corresponding to the components are not necessarily yet created when the link is created.
   The reason for this is that there is no set order in which the reactions are executed.
   Therefore we create multiple separate reactions.
   One will be responsible for creating the link and add it to the root object whenever a link is inserted into the system.
   The other reactions will be responsible for adding entities and protocols to the link which correspond to their respective counterparts within the first file.

   We add the `LinkInsertedIntoSystem`, `ComponentInsertedIntoLink` and `ProtocolInsertedIntoLink` reactions to the templateReactions.reactions file.

   We take a closer look at the `ProtocolInsertedIntoLink` reaction.

   ```java
      reaction ProtocolInsertedIntoLink {
         after element replaced at model::Link[protocol]
         call addProtocolToLink(affectedEObject, newValue)
      }

      routine addProtocolToLink(model::Link link, model::Protocol protocol) {
         match {
            val mLink = retrieve model2::Link corresponding to link
            val mProtocol = retrieve model2::Protocol corresponding to protocol
         }
         update {
            mLink.protocol = mProtocol
            addCorrespondenceBetween(protocol, mProtocol)
         }
      }
   ```

   Note that we use the `replaced` event instead of the `inserted` event.
   The reason for this lies within the meta-model. Since we specified that the lower bound of the `protocol` reference is `1` we cannot insert a `Protocol` into the `Link` object.
   Even though it is not yet been set, the reaction just looks at the specification of the meta-model and therefore assumes that the `Protocol` is already present since the lower bound is `1`.

3. **Adding a Test Case**

   We now add a test case that checks that when we insert a `Link` into the system and add a `Protocol` as well as `Components` to the `Link` that the corresponding `Link`, `Entities` and `Protocol` are created.
   Have a look at the `insertLink` test case in the `VSUMExampleTest.java` file.
   We first add all the different objects to the system and then check that the corresponding objects are created.
   We check that the `Link` is created and that the `Protocol` and `Entities` are added to the `Link` object. We compare that the names of the `Link` and `Protocol` as well as `Component`/`Entity` objects are the same in both meta-models.

   ```java
      @Test
      void insertLink(@TempDir Path tempDir) {
         InternalVirtualModel vsum = createDefaultVirtualModel(tempDir);
         // add all the objects to the system (and trigger their respective reactions)
         addSystem(vsum, tempDir);
         addComponent(vsum);
         addComponent(vsum);
         addProtocol(vsum);
         // add a link between the two components and the protocol
         addLink(vsum);
         Assertions.assertTrue(assertView(getDefaultView(vsum, List.of(System.class, Root.class)), (View v) -> {
            // assert that a link with the same name has been inserted into root
            return v.getRootObjects(System.class).iterator().next()
               .getLinks().get(0).getName()
               .equals(v.getRootObjects(Root.class).iterator().next()
                  .getLinks().get(0)
                  .getName());
         }));
         Assertions.assertTrue(assertView(getDefaultView(vsum, List.of(System.class, Root.class)), (View v) -> {
            // assert that a two protocols corresponding to the respective links have the
            // same name
            return v.getRootObjects(System.class).iterator().next()
               .getLinks().get(0).getProtocol().getName()
               .equals(v.getRootObjects(Root.class).iterator().next()
                  .getLinks().get(0).getProtocol().getName());
         }));
         Assertions.assertTrue(assertView(getDefaultView(vsum, List.of(System.class, Root.class)), (View v) -> {
            // assert that the components belonging to the link in the system and the
            // corresponding entities in the root have the same name
            return v.getRootObjects(System.class).iterator().next()
               .getLinks().get(0).getComponents().get(0).getName()
               .equals(v.getRootObjects(Root.class).iterator().next()
                  .getLinks().get(0).getEntities().get(0).getName())
               && v.getRootObjects(System.class).iterator().next()
                  .getLinks().get(0).getComponents().get(1).getName()
                  .equals(v.getRootObjects(Root.class).iterator().next()
                        .getLinks().get(0).getEntities().get(1).getName());
         }));
      }

   ```
