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

The evaluator is provided by the `tools.vitruv.dsls:vitruvocl-language` artifact:

```java
import tools.vitruv.dsls.vitruvocl.pipeline.VitruvOCL;
```

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

### Reaction-Constraint Registry: Scoping Evaluation to What Changed

`VitruvOCL.evaluateConstraints(path)` always evaluates **every** constraint in the file. That is
fine for a manual check, but after a single transaction you usually only care about the
constraints that could plausibly have been broken by the Reactions that just fired — not the
whole file. The `tools.vitruv.methodologisttemplate.consistency.registry` package (in the
`consistency` module) provides a small, hand-maintained mapping from Reaction classes to the
`ConstraintRef`s they are responsible for, plus a coordinator that uses it to filter the
evaluation result down to what is actually relevant.

The package contains:

- **`ConstraintRef`** — identifies one constraint declaration by `(contextType, constraintName)`,
  e.g. `new ConstraintRef("model::Component", "ComponentHasCorrespondingEntity")`. The
  `contextType` must include the namespace prefix (`model::` vs `model2::`), because both models
  can declare a metaclass with the same simple name (e.g. `Link`).
- **`ReactionConstraintRegistry`** — a `Class<? extends Reaction> -> List<ConstraintRef>` map with
  `register(...)`, `getConstraintsFor(reactionClass)`, and `getConstraintsForAll(reactionClasses)`
  (the union across several fired Reactions, deduplicated).
- **`ProjectReactionConstraints`** — the actual project mapping, built by `buildRegistry()`. This
  is the file you edit whenever you add a Reaction and a constraint that belongs together — see
  below.
- **`ConstraintEvaluationCoordinator`** + **`VitruvOCLGateway`/`VitruvOCLGatewayImpl`** — given the
  set of Reaction classes that fired in a transaction, looks up the relevant `ConstraintRef`s and
  filters `VitruvOCL.evaluateConstraints(...)`'s result down to just those, returning a
  `List<ViolatedConstraint>` (empty if nothing relevant was violated).

#### Adding a mapping entry

Whenever you add a Reaction and a constraint that are meant to guard the same consistency rule,
register the pair in `ProjectReactionConstraints.buildRegistry()`:

```java
registry.register(
    ComponentInsertedIntoSystemReaction.class,
    new ConstraintRef("model::Component", "ComponentHasCorrespondingEntity"),
    new ConstraintRef("model::Component", "ComponentHasCorrespondence"),
    new ConstraintRef("model::Component", "ComponentNameMatchesEntityName"));
```

Because the key is the generated Reaction **class**, not a string, renaming or deleting a
Reaction makes every registration referencing it fail to compile — there is no way for an entry
to silently go stale. A Reaction with no matching constraint simply has no registration; that is
expected, not an error (see the `SystemInsertedAsRootReaction` comment in the file).

#### Evaluating only the relevant constraints

```java
var registry = ProjectReactionConstraints.buildRegistry();
var gateway = new VitruvOCLGatewayImpl(CONSTRAINT_FILE);
var coordinator = new ConstraintEvaluationCoordinator(registry, gateway);

Set<Class<? extends Reaction>> firedReactions = Set.of(ComponentInsertedIntoSystemReaction.class);
List<ViolatedConstraint> violations = coordinator.evaluateFor(firedReactions);
```

**Known gap:** there is currently no automatic way to obtain `firedReactions` from a running
transaction — `ChangePropagationListener.finishedChangePropagation(...)` reports the resulting
`PropagatedChange`s, not which Reaction classes produced them. Until that is resolved, callers
must supply the fired-Reaction set themselves (see `ConstraintEvaluationIntegrationTest` in the
`consistency` module for a worked example). Acting on a non-empty violation list — e.g. rolling
back the transaction — is likewise out of scope until a `TransactionManager` with rollback
capability exists.

#### `pre`/`post` example: `prepost-example.ocl`

`consistency/src/main/constraints/tools/vitruv/methodologisttemplate/consistency/prepost-example.ocl`
demonstrates the same registry workflow using OCL#'s `pre`/`post` keywords instead of `inv`,
against the metaclasses already mapped to Reactions above. **Known limitation:** as of the
`vitruvocl-language` snapshot this project currently depends on, `pre`/`post` blocks parse and
type-check correctly but are never evaluated for truthiness — the runtime's `EvaluationVisitor`
implements `visitInvCS(...)` but has no `visitPreCS(...)`/`visitPostCS(...)` override, so every
`pre`/`post` constraint is reported as satisfied regardless of its body. This is expected to be
fixed upstream; nothing on this side (`ConstraintRef`, `ReactionConstraintRegistry`,
`ConstraintEvaluationCoordinator`, `VitruvOCLGatewayImpl`) needs to change once it is, since they
already match `pre`/`post` headers the same way as `inv`. See `PrePostConstraintWorkflowTest` in
the `consistency` module — its second test case documents exactly this limitation and is meant
to start failing (by design) once the upstream fix lands, as a signal to flip its assertion.

### Interactive Evaluation via the VS Code Extension

Constraints can also be evaluated interactively in VS Code with the `vitruvocl` extension. The
extension evaluates constraints against files on disk — it does not talk to a running JVM — so it
needs a persisted VSUM (model instances plus a `.correspondence` file) to exist somewhere in the
workspace before it can run anything.

To create one, run `VSUMExample.main()` **once from the repository root**, on **JDK 21+**
(`vitruvocl-language` ships Java 21 bytecode; JDK 17 fails with `UnsupportedClassVersionError`).

```powershell
$env:JAVA_HOME = "<path-to-a-JDK-21-installation>"
mvn -pl vsum test-compile exec:java "-Dexec.mainClass=tools.vitruv.methodologisttemplate.vsum.VSUMExample" "-Dexec.classpathScope=test"
```

```bash
JAVA_HOME=<path-to-a-JDK-21-installation> \
mvn -pl vsum test-compile exec:java -Dexec.mainClass=tools.vitruv.methodologisttemplate.vsum.VSUMExample -Dexec.classpathScope=test
```

This creates a `vsumexample/` folder at the repository root containing the model instances and
`vsum/correspondences.correspondence`. The VS Code extension discovers it there automatically —
open `constraints.ocl` and use its inline "Run" / "Run all constraints" actions.

`vsumexample/` is a generated runtime artifact (see `.gitignore`) — rerun the command above whenever
you want a fresh VSUM state to evaluate against. Running it from your IDE's "Run" button works too,
as long as the IDE is actually configured to launch with a JDK 21 runtime.

## ViewType

This folder contains the definition of the view types. These are necessary to create views of the vsum.

## Vsum

This folder contains the VSUM

## Useful Links

Details about the build process and configurations can be found in the readmes of the relevant projects.

- <https://github.com/vitruv-tools/Maven-Build-Parent/blob/main/readme.md>
- <https://github.com/vitruv-tools/EMF-Template/blob/main/readme.md>