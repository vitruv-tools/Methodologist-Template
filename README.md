# Methodologist Template Project

This project is a template for the methodologists who are creating a V-SUM.

## Getting Started

The Methodologist Template can be executed using the Maven build system. The project comes with a maven wrapper, so you can run it without installing Maven.
On a fresh clone, running **one command** from the repository root is enough to get everything
working:

```bash
./mvnw clean install
```

This is the only command you need to run, and the order of the four modules (`viewtype`,
`model`, `consistency`, `vsum`) does not matter — Maven's reactor works it out from their
dependencies automatically, regardless of the order they're listed in in the root `pom.xml`. In
one pass it will:

- generate the EMF model classes from `model.ecore`/`model2.ecore` (in `model`),
- generate the Reaction classes from `templateReactions.reactions` (in `consistency`) — these are
  the actual `SystemInsertedAsRootReaction`, `ComponentInsertedIntoSystemReaction`, etc. classes
  the Reaction-Constraint Registry (see below) registers against,
- compile and run every test in `consistency` and `vsum`, and
- **install** all four modules into your local Maven repository (`~/.m2`).

That last point matters: use `install`, not `verify` (or `package`). Later, one-off commands like
the `VSUMExample.main()` invocation below (for the VS Code extension) run as a **separate**,
single-module Maven invocation (`-pl vsum`) — and a single-module invocation resolves its sibling
modules (`model`, `consistency`) from your local repository, not from whatever another Maven
invocation built earlier. Without `install` having put them there first, that command fails with
`Could not resolve dependencies ... tools.vitruv.methodologisttemplate.model:jar:0.1.0-SNAPSHOT`
(reproduced while writing this). `./mvnw clean install` from the root is the one command that
avoids that.

You'll also need `JAVA_HOME` pointing at a **JDK 21 or newer** — `vitruvocl-language` ships Java
21 bytecode, so JDK 17 fails outright, and some IDEs default to an older `java` on `PATH` even
when a newer JDK is installed.

Now you can start to modify the project to your needs. Or jump to the [Tutorial](#tutorial) section to get a quick start. First we will explain what tests are run and what they are testing.

### Tests

Tests live in two places: `vsum/src/test` (VSUM setup, Reactions, and VitruviusOCL usage) and
`consistency/src/test` (the Reaction-Constraint Registry — `ConstraintRef`,
`ReactionConstraintRegistry`, `ConstraintEvaluationCoordinator`, the automatic hook, etc.,
documented further down).
The goal is to ensure that the reactions are keeping the model consistent.

`VSUMExampleTest.createDefaultVirtualModel(...)` wires every VSUM it builds — so every test in
that file, to the automatic hook via `HookedModel2Model2ChangePropagationSpecification` and
`ReactionConstraintCheckingListener.failFast(...)` (see "Automatic checking, hooked into every
commit" further down). That means any `commitChanges()` in any test in this file that violates a
constraint registered for the Reaction it triggers throws immediately and fails that test.

Consider the following example taken from the `VSUMExampleTest.java` file:

```java
@Test
  void systemInsertionAndPropagationTest(@TempDir Path tempDir) {
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addSystem(vsum, tempDir);
    // assert that the directly added System is present
    Assertions.assertEquals(1, getDefaultView(vsum, List.of(System.class)).getRootObjects().size());
    // as well as the Root that should be created by the Reactions, see templateReactions.
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
Constraints are written in OCL-like syntax and stored in `.ocl` files next to the reactions.

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

See `VSUMExampleTest.java` for a test case that demonstrates manual evaluation after change
propagation. `VSUMExample.java` demonstrates automatic integration too, but via the
Reaction-Constraint Registry's hook (see below) rather than a hand-rolled listener — both it and
`VSUMExampleTest.createDefaultVirtualModel` wire up the exact same mechanism.

### Reaction-Constraint Registry: Scoping Evaluation to What Changed

`VitruvOCL.evaluateConstraints(path)` always evaluates **every** constraint in the file. That is
fine for a manual check, but after a single transaction you usually only care about the
constraints that could plausibly have been broken by the Reactions that just fired. The `tools.vitruv.methodologisttemplate.consistency.registry` package (in the
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
  `List<ViolatedConstraint>` (empty if nothing relevant was violated). `VitruvOCLGatewayImpl`
  takes one or more `.ocl` files and combines their results, since a single Reaction's constraints
  can be spread across more than one file (see `ProjectReactionConstraints`, which references both
  `constraints.ocl` and `prepost-example.ocl`).

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
Reaction makes every registration referencing it fail to compile. A Reaction with no matching constraint simply has no registration; that is
expected, not an error (see the `SystemInsertedAsRootReaction` comment in the file).

#### Automatic checking, hooked into every commit

For automatic, zero-argument checking after every commit, three more classes in the same package wire the registry directly into Reaction execution:

- **`FiredReactionsCollector`** — accumulates the Reaction classes that actually fired during the
  current transaction.
- **`HookedReaction`** — wraps one generated Reaction so it reports into a
  `FiredReactionsCollector` whenever it actually matches and fires (not merely whenever it is
  invoked — the Reactions runtime calls `execute(...)` on *every* registered Reaction for *every*
  incoming change regardless of relevance; only some of those calls actually do anything). Since
  the match check, `isCurrentChangeMatchingTrigger(EChange)`, is generated per Reaction class and
  not exposed through any shared interface, it is resolved reflectively once per wrapped instance.
- **`HookedModel2Model2ChangePropagationSpecification`** — a drop-in replacement for the generated
  `Model2Model2ChangePropagationSpecification` that registers every Reaction wrapped in a
  `HookedReaction` instead of bare. Use it in place of `Model2Model2ChangePropagationSpecification`
  when building the VSUM.
- **`ReactionConstraintCheckingListener`** — a `ChangePropagationListener` that, once change
  propagation for a transaction finishes, drains the collector, reconstructs the transaction from
  the `PropagatedChange`s it observed (original edit plus every consequential change the fired
  Reactions made), and calls the transaction-aware
  `ConstraintEvaluationCoordinator.evaluateFor(Set, List)` automatically — so `post` and `inv`
  constraints get evaluated for real here, not skipped.
- **`PreconditionGuard`** — checked by `HookedReaction` *before* a Reaction's generated `execute`
  body runs, not after. A violated `pre` constraint throws right there, so the Reaction's own
  `execute` body never runs at all and the model is never mutated by it — a real guard, not a
  post-hoc detection. Bound to a `ConstraintEvaluationCoordinator` after the VSUM is built (see
  below), since the coordinator cannot exist yet while the Hooked specification is still wrapping
  Reactions during `buildAndInitialize()`.

```java
var hookedSpecification = new HookedModel2Model2ChangePropagationSpecification();

InternalVirtualModel vsum = new VirtualModelBuilder()
    .withStorageFolder(storageFolder)
    .withUserInteractorForResultProvider(...)
    .withChangePropagationSpecifications(hookedSpecification) // instead of the generated one
    .buildAndInitialize();

var registry = ProjectReactionConstraints.buildRegistry();
var gateway = new VitruvOCLGatewayImpl(CONSTRAINT_FILE, PREPOST_CONSTRAINT_FILE);
var coordinator = new ConstraintEvaluationCoordinator(registry, gateway);
hookedSpecification.getPreconditionGuard().bind(coordinator); // required for pre to actually gate execution

vsum.addChangePropagationListener(
    ReactionConstraintCheckingListener.failFast(hookedSpecification.getCollector(), coordinator));
```

From this point on, every `commitChanges()` automatically checks exactly the constraints relevant
to whatever Reactions fired in that transaction, no further calls needed. Skip the
`getPreconditionGuard().bind(...)` line and `pre` constraints simply never block anything — the
guard silently no-ops when unbound, so this is easy to forget without erroring.

#### `pre`/`post` example: `prepost-example.ocl`

`consistency/src/main/constraints/tools/vitruv/methodologisttemplate/consistency/prepost-example.ocl` demonstrates the same registry workflow using OCL's `pre`/`post` keywords instead of `inv`,
against the metaclasses already mapped to Reactions above.

**`pre` and `post` are both genuinely evaluated, at two different points in time:** `pre` is
checked by `PreconditionGuard` immediately before the Reaction it guards executes, against the
model's current (pre-Reaction) state — no transaction needed for a constraint that only reads
`self`'s current attributes, though one that also relies on `OCLisNew`/`OCLisModified`/
`OCLisDeleted` will not see this round's changes yet at that point. `post` is checked afterwards
by `ReactionConstraintCheckingListener`, against the fully recorded transaction, so `@pre` and
those three operators resolve correctly there. Evaluating either kind with no transaction context
at all (`VitruvOCLGateway#evaluateAll()`, no arguments) skips both outright instead.

### Interactive Evaluation via the VS Code Extension

Constraints can also be evaluated interactively in VS Code with the `vitruvocl` extension. The
extension evaluates constraints against files on disk — it does not talk to a running JVM, so it
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