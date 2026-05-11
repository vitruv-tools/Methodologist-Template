# Methodologist Template Project

This project is a template for the methodologists who are creating a V-SUM.

## Getting Started

The Methodologist Template can be executed using the Maven build system. The project comes with a maven wrapper, so you can run it without installing Maven.
To build the project you can run the following command:

```bash
./mvnw clean verify
```

Verify that all tests are passing. The tests are located in the `vsum` folder.
Now you can start to modify the project to your needs. Or jump to the [Tutorial](#tutorial) section to get a quick start. First we will explain what tests are run and what they are testing.

### Tests

The tests test the vsum and its reactions.
The goal is to ensure that the reactions are keeping the model consistent.
Consider the following example taken from the `VSUMExampleTest.java` file:

```java
@Test
  void systemInsertionAndPropagationTest(@TempDir Path tempDir) {
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addSystem(vsum, tempDir);
    // assert that the directly added System is present
    Assertions.assertEquals(1, getDefaultView(vsum, List.of(System.class)).getRootObjects().size());
    // as well as the Root that should be created by the Reactions, see templateReactions.reactions#14
    Assertions.assertEquals(1, getDefaultView(vsum, List.of(Root.class)).getRootObjects().size());
  }
```

In this testcase a system is added to the vsum. The test checks that the system is present in the view and that a root object is created by the reaction. The reaction is defined in the `consistency` folder.

## Tutorial

To dive into the project, we recommend to follow the [Tutorial](./tutorial.md) that is provided in this repository. The tutorial will guide you through the process of creating a V-SUM, adding systems, and defining reactions. It will also explain how to use the provided tools and features effectively.

## Model

The `model` folder contains the meta-model in the ecore format. Note that each ecore file is accompanied by a genmodel. The genmodel is used to generate the code. If you update the ecore model, you need to update the genmodel. You can easily edit ecore models with the Eclipse Modeling Framework (EMF) in Eclipse. There you can also automatically update the genmodel. For more information on how to do that please refer to [this Tutorial by Lars Vogel](https://www.vogella.com/tutorials/EclipseEMF/article.html) on EMF and ecore.

## Consistency

This folder contains the consistency specifications, like reactions.
Reaction files (`.reactions`) define how changes in one model are propagated to keep other models consistent.
Alongside the reactions, this folder also contains OCL constraint files (`.ocl`) that express declarative
consistency rules which can be evaluated against the VSUM at any point in time.

## Constraints with VitruviusOCL

VitruviusOCL is a cross-model constraint language and evaluator for Vitruvius VSUMs.
Constraints are written in OCL# syntax and stored in `.ocl` files next to the reactions.

The constraint file for this template is located at:
```
consistency/src/main/constraints/tools/vitruv/methodologisttemplate/consistency/constraints.ocl
```

### How it works

1. **Define constraints** in a `.ocl` file using the OCL# syntax (see `constraints.ocl` for examples).
2. **Register your VSUM** once with `VitruvOCL.registerVSUM(vsum)`.
3. **Evaluate constraints** at any time with `VitruvOCL.evaluateConstraints(path)`, which returns a
   `BatchValidationResult` indicating per constraint whether it is satisfied or violated.

Constraint evaluation can be triggered in two ways:

- **Manually**: Call `VitruvOCL.evaluateConstraints(path)` explicitly after any `commitChanges()`.
- **Automatically after every propagation**: Register a `ChangePropagationListener` on the VSUM.
  Vitruvius will call `finishedChangePropagation()` automatically after each `commitChanges()` once
  all Reactions have executed — making it the ideal place to invoke the constraint evaluator.

See `VSUMExample.java` for a complete example of the automatic integration, and
`VSUMExampleTest.java` for a test case that demonstrates manual evaluation after change propagation.

## ViewType

This folder contains the definition of the view types. These are necessary to create views of the vsum.

## Vsum

This folder contains the VSUM

## Useful Links

Details about the build process and configurations can be found in the readmes of the relevant projects.

- <https://github.com/vitruv-tools/Maven-Build-Parent/blob/main/readme.md>
- <https://github.com/vitruv-tools/EMF-Template/blob/main/readme.md>