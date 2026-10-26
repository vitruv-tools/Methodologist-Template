package tools.vitruv.methodologisttemplate.consistency.registry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import tools.vitruv.change.atomic.AdditiveEChange;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.SubtractiveEChange;
import tools.vitruv.change.atomic.eobject.EObjectExistenceEChange;
import tools.vitruv.change.atomic.feature.FeatureEChange;
import tools.vitruv.change.atomic.root.RootEChange;
import tools.vitruv.change.composite.recording.ChangeRecorder;
import tools.vitruv.dsls.reactions.runtime.reactions.Reaction;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;

/**
 * Wraps a generated Reaction and enforces its contract on every execution of the Reaction:
 *
 * <ol>
 *   <li>its preconditions, right before the Reaction runs, against the current state;
 *   <li>the Reaction itself, while recording exactly the changes it makes;
 *   <li>its postconditions, right after the Reaction, against those changes only -- so {@code
 *       @pre} reads the state right before this execution, and {@code OCLisNew}/{@code
 *       OCLisModified}/{@code OCLisDeleted} refer to what this execution did.
 * </ol>
 *
 * <p>A Reaction is defined for a type, but the Reactions runtime executes it once per atomic change
 * that triggers it, so a commit that inserts two Components executes {@code
 * ComponentInsertedIntoSystem} twice. Checking the contract per execution, rather than once at the
 * end of the commit, attributes a violation to the execution that caused it, and a later Reaction
 * of the same commit cannot mask it.
 *
 * <p>The Reactions runtime calls {@link Reaction#execute} for every registered Reaction on every
 * change, regardless of relevance (see {@code AbstractReactionsChangePropagationSpecification
 * .propagateChange}). The contract only applies when the Reaction actually matches the change; the
 * Reactions compiler generates a public {@code isCurrentChangeMatchingTrigger(EChange)} method on
 * every concrete Reaction class for that, but does not expose it through {@link Reaction} or any
 * shared supertype, so it is resolved reflectively once per wrapped instance.
 */
final class HookedReaction implements Reaction {

  private static final String MATCH_METHOD_NAME = "isCurrentChangeMatchingTrigger";
  private static final String GENERATED_SUFFIX = "Reaction";

  private final Reaction delegate;
  private final Class<? extends Reaction> delegateClass;
  private final Method matchMethod;
  private final ContractEnforcer contractEnforcer;
  private final String reactionName;

  @SuppressWarnings("unchecked")
  HookedReaction(Reaction delegate, ContractEnforcer contractEnforcer) {
    this.delegate = delegate;
    this.delegateClass = (Class<? extends Reaction>) delegate.getClass();
    this.contractEnforcer = contractEnforcer;
    this.matchMethod = resolveMatchMethod(delegateClass);
    String simpleName = delegateClass.getSimpleName();
    this.reactionName =
        simpleName.endsWith(GENERATED_SUFFIX)
            ? simpleName.substring(0, simpleName.length() - GENERATED_SUFFIX.length())
            : simpleName;
  }

  private static Method resolveMatchMethod(Class<? extends Reaction> reactionClass) {
    try {
      return reactionClass.getMethod(MATCH_METHOD_NAME, EChange.class);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(
          "Generated Reaction "
              + reactionClass.getName()
              + " does not expose "
              + MATCH_METHOD_NAME
              + "(EChange) -- HookedReaction relies on this reactions-compiler convention to know"
              + " whether a Reaction actually fires for a given change.",
          e);
    }
  }

  @Override
  public void execute(EChange<EObject> change, ReactionExecutionState state) {
    if (!contractEnforcer.appliesTo(delegateClass) || !invokeMatch(change)) {
      delegate.execute(change, state);
      return;
    }
    // Checked before delegate.execute() runs: if the user rejects a violated precondition, this
    // throws and the Reaction's own execute body never runs.
    contractEnforcer.checkBeforeExecuting(delegateClass, reactionName, state.userInteractor());
    if (!contractEnforcer.hasPostconditions(delegateClass)) {
      delegate.execute(change, state);
      return;
    }
    List<EChange<EObject>> reactionChanges = executeRecording(change, state);
    contractEnforcer.checkAfterExecuting(
        delegateClass, reactionName, reactionChanges, state.userInteractor());
  }

  /** Runs the Reaction and returns exactly the changes it made. */
  private List<EChange<EObject>> executeRecording(
      EChange<EObject> change, ReactionExecutionState state) {
    ResourceSet resourceSet = resourceSetOf(change);
    if (resourceSet == null) {
      // e.g. the deletion of an element, which is no longer attached to any resource
      resourceSet = contractEnforcer.modelResourceSet();
    }
    if (resourceSet == null) {
      throw new IllegalStateException(
          "Cannot record the changes of Reaction "
              + reactionName
              + ": the triggering change is not attached to a ResourceSet");
    }
    try (var recorder = new ChangeRecorder(resourceSet)) {
      recorder.addToRecording(resourceSet);
      recorder.beginRecording();
      delegate.execute(change, state);
      return recorder.endRecording().getEChanges();
    }
  }

  /** The ResourceSet of the models, reached through any object the triggering change touches. */
  private static ResourceSet resourceSetOf(EChange<EObject> change) {
    if (change instanceof RootEChange<EObject> rootChange && rootChange.getResource() != null) {
      return rootChange.getResource().getResourceSet();
    }
    for (Object touched : touchedObjects(change)) {
      if (touched instanceof EObject object) {
        Resource resource = object.eResource();
        if (resource != null && resource.getResourceSet() != null) {
          return resource.getResourceSet();
        }
      }
    }
    return null;
  }

  private static List<Object> touchedObjects(EChange<EObject> change) {
    Object affected =
        change instanceof FeatureEChange<EObject, ?> featureChange
            ? featureChange.getAffectedElement()
            : change instanceof EObjectExistenceEChange<EObject> existenceChange
                ? existenceChange.getAffectedElement()
                : null;
    Object added =
        change instanceof AdditiveEChange<EObject, ?> additive ? additive.getNewValue() : null;
    Object removed =
        change instanceof SubtractiveEChange<EObject, ?> subtractive
            ? subtractive.getOldValue()
            : null;
    return Arrays.asList(affected, added, removed);
  }

  private boolean invokeMatch(EChange<EObject> change) {
    try {
      return (boolean) matchMethod.invoke(delegate, change);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException(
          "Failed to invoke " + MATCH_METHOD_NAME + " reflectively on " + delegateClass.getName(),
          e);
    }
  }

  /** Visible for {@code HookedModel2Model2ChangePropagationSpecificationTest}. */
  Class<? extends Reaction> wrappedReactionClass() {
    return delegateClass;
  }
}
