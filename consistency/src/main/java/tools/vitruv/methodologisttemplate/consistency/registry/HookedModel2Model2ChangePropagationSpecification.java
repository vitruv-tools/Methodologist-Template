package tools.vitruv.methodologisttemplate.consistency.registry;

import mir.reactions.model2Model2.ComponentDeletedReaction;
import mir.reactions.model2Model2.ComponentInsertedIntoLinkReaction;
import mir.reactions.model2Model2.ComponentInsertedIntoSystemReaction;
import mir.reactions.model2Model2.ComponentRenamedReaction;
import mir.reactions.model2Model2.LinkInsertedIntoSystemReaction;
import mir.reactions.model2Model2.Model2Model2ChangePropagationSpecification;
import mir.reactions.model2Model2.ProtocolInsertedIntoLinkReaction;
import mir.reactions.model2Model2.ProtocolInsertedIntoSystemReaction;
import mir.reactions.model2Model2.SystemInsertedAsRootReaction;
import tools.vitruv.dsls.reactions.runtime.reactions.Reaction;
import tools.vitruv.dsls.reactions.runtime.routines.RoutinesFacade;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.dsls.reactions.runtime.structure.ReactionsImportPath;

/**
 * Drop-in replacement for {@link Model2Model2ChangePropagationSpecification} that wraps every
 * registered Reaction in a {@link HookedReaction}, so the contract of each Reaction (its pre- and
 * postconditions, see {@link ProjectReactionConstraints}) is checked on every execution. Call
 * {@link ContractEnforcer#bind} on {@link #getContractEnforcer()} right after building the VSUM --
 * see {@code ContractGuardedVsum} and {@code VSUMExampleTest} in the {@code vsum} module for the
 * full wiring.
 *
 * <p><b>Maintenance note:</b> {@link #setup()} below must be kept in sync with the generated
 * {@link Model2Model2ChangePropagationSpecification#setup()}. It cannot simply call {@code
 * super.setup()} and wrap the result afterwards, because the Reaction list in {@code
 * AbstractReactionsChangePropagationSpecification} is private with no accessor -- wrapping has to
 * happen at registration time. {@code setup()} is regenerated from {@code
 * templateReactions.reactions} on every build; if a Reaction is added, renamed, or removed there,
 * update the matching {@code addReaction(hook(new ...))} call below too --
 * {@code HookedModel2Model2ChangePropagationSpecificationTest} fails if the two ever diverge.
 *
 * <p><b>Why {@code contractEnforcer} is only created once:</b> {@code
 * AbstractChangePropagationSpecification.setUserInteractor(...)} -- called by the Reactions
 * runtime's {@code ChangePropagator} before every single transactional change round, not just
 * once -- clears the Reaction list and calls {@link #setup()} again each time. If {@link #setup()}
 * created a fresh {@link ContractEnforcer} every time it re-runs, the instance bound right after
 * construction would no longer be the one the Reactions use. Reusing the same enforcer across
 * every {@link #setup()} re-invocation (guarded by the null check below) keeps it valid for the
 * lifetime of this specification instance.
 */
public final class HookedModel2Model2ChangePropagationSpecification
    extends Model2Model2ChangePropagationSpecification {

  // Assigned inside setup(), which the superclass constructor invokes (via virtual dispatch)
  // before this subclass's own constructor body would otherwise run -- a plain constructor
  // parameter/field-initializer can't be used here, since it would still be null at that point.
  private ContractEnforcer contractEnforcer;

  /**
   * Available once construction completes; call {@link ContractEnforcer#bind} on it with a {@link
   * ConstraintEvaluationCoordinator} right after building the VSUM, so the Reactions' contracts
   * are actually checked. Until then, Reactions run unchecked.
   */
  public ContractEnforcer getContractEnforcer() {
    return contractEnforcer;
  }

  @Override
  protected void setup() {
    if (contractEnforcer == null) {
      contractEnforcer = new ContractEnforcer();
    }

    org.eclipse.emf.ecore.EPackage.Registry.INSTANCE.putIfAbsent(
        tools.vitruv.methodologisttemplate.model.model.impl.ModelPackageImpl.eNS_URI,
        tools.vitruv.methodologisttemplate.model.model.impl.ModelPackageImpl.eINSTANCE);
    org.eclipse.emf.ecore.EPackage.Registry.INSTANCE.putIfAbsent(
        tools.vitruv.methodologisttemplate.model.model2.impl.Model2PackageImpl.eNS_URI,
        tools.vitruv.methodologisttemplate.model.model2.impl.Model2PackageImpl.eINSTANCE);

    addReaction(hook(new SystemInsertedAsRootReaction(this::routinesFacade)));
    addReaction(hook(new ComponentInsertedIntoSystemReaction(this::routinesFacade)));
    addReaction(hook(new ComponentRenamedReaction(this::routinesFacade)));
    addReaction(hook(new ComponentDeletedReaction(this::routinesFacade)));
    addReaction(hook(new ProtocolInsertedIntoSystemReaction(this::routinesFacade)));
    addReaction(hook(new LinkInsertedIntoSystemReaction(this::routinesFacade)));
    addReaction(hook(new ComponentInsertedIntoLinkReaction(this::routinesFacade)));
    addReaction(hook(new ProtocolInsertedIntoLinkReaction(this::routinesFacade)));
  }

  private HookedReaction hook(Reaction reaction) {
    return new HookedReaction(reaction, contractEnforcer);
  }

  private RoutinesFacade routinesFacade(ReactionExecutionState executionState) {
    return createRoutinesFacadesProvider(executionState)
        .getRoutinesFacade(ReactionsImportPath.fromPathString("model2Model2"));
  }
}
