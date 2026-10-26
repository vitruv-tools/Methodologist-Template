package tools.vitruv.methodologisttemplate.consistency.registry;

import mir.reactions.model2Model2.ComponentDeletedReaction;
import mir.reactions.model2Model2.ComponentInsertedIntoLinkReaction;
import mir.reactions.model2Model2.ComponentInsertedIntoSystemReaction;
import mir.reactions.model2Model2.ComponentRenamedReaction;
import mir.reactions.model2Model2.LinkInsertedIntoSystemReaction;
import mir.reactions.model2Model2.ProtocolInsertedIntoSystemReaction;

/**
 * Hand-maintained association between this project's Reactions (declared in {@code
 * templateReactions.reactions}) and their contracts in {@code prepost-example.ocl}: the
 * preconditions checked right before and the postconditions checked right after each execution of
 * the Reaction (see {@link HookedReaction}).
 *
 * <p>Extend this alongside adding a Reaction and its contract to the {@code .ocl} file -- this
 * file is the single place that ties the two together. If a Reaction is renamed or removed, the
 * compiler will flag every registration referencing it, because the key is the generated class,
 * not a string. A (contextType, constraintName) pair that no evaluated {@code .ocl} file declares
 * fails with an {@link UnknownConstraintException}.
 *
 * <p>Invariants are not registered here: all invariants in {@code constraints.ocl} are checked
 * after every commit, independent of which Reactions ran (see {@code ContractGuardedVsum} in the
 * {@code vsum} module). {@code SystemInsertedAsRootReaction} and {@code
 * ProtocolInsertedIntoLinkReaction} have no contract.
 */
public final class ProjectReactionConstraints {

  private static final String ROOT = "model2::Root";

  private ProjectReactionConstraints() {}

  public static ReactionConstraintRegistry buildRegistry() {
    var registry = new ReactionConstraintRegistry();
    var componentHasName = new ConstraintRef("model::Component", "ComponentHasNameBeforeSync");

    registry.register(
        ComponentInsertedIntoSystemReaction.class,
        componentHasName,
        new ConstraintRef(ROOT, "ComponentInsertCreatedExactlyOneEntity"),
        new ConstraintRef("model2::Entity", "NewEntityMatchesComponent"));

    registry.register(
        ComponentRenamedReaction.class,
        componentHasName,
        new ConstraintRef("model2::Entity", "RenamedEntityMatchesComponent"));

    registry.register(
        ComponentDeletedReaction.class,
        new ConstraintRef(ROOT, "ComponentDeleteRemovedExactlyOneEntity"));

    registry.register(
        LinkInsertedIntoSystemReaction.class,
        new ConstraintRef("model::Link", "LinkHasAtLeastOneComponentBeforeSync"),
        new ConstraintRef(ROOT, "LinkInsertCreatedExactlyOneLink"),
        new ConstraintRef("model2::Link", "NewLinkCorrespondsToLink"));

    registry.register(
        ComponentInsertedIntoLinkReaction.class,
        new ConstraintRef("model2::Link", "LinkEntitiesCorrespondToLinkComponents"));

    registry.register(
        ProtocolInsertedIntoSystemReaction.class,
        new ConstraintRef("model::Protocol", "ProtocolHasNameBeforeSync"),
        new ConstraintRef(
            "model2::CommunicationStandard", "NewStandardMatchesProtocol"));

    return registry;
  }
}
