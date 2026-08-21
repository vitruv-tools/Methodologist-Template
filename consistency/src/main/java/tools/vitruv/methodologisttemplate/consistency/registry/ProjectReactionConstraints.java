package tools.vitruv.methodologisttemplate.consistency.registry;

import mir.reactions.model2Model2.ComponentDeletedReaction;
import mir.reactions.model2Model2.ComponentInsertedIntoLinkReaction;
import mir.reactions.model2Model2.ComponentInsertedIntoSystemReaction;
import mir.reactions.model2Model2.ComponentRenamedReaction;
import mir.reactions.model2Model2.LinkInsertedIntoSystemReaction;

/**
 * Hand-maintained association between this project's Reactions (declared in {@code
 * templateReactions.reactions}) and the OCL# constraints in {@code constraints.ocl} that must
 * hold after each one executes.
 *
 * <p>Extend this alongside adding a Reaction and its pre-/postcondition to {@code
 * constraints.ocl} — this file is the single place that ties the two together. If a Reaction is
 * renamed or removed, the compiler will flag every registration referencing it, because the key
 * is the generated class, not a string.
 *
 * <p>{@code SystemInsertedAsRootReaction}, {@code ProtocolInsertedIntoSystemReaction}, and {@code
 * ProtocolInsertedIntoLinkReaction} have no entries below because {@code constraints.ocl}
 * currently declares no constraint over {@code model::System}, {@code model2::Root}, or {@code
 * model2::CommunicationStandard}.
 */
public final class ProjectReactionConstraints {

  private ProjectReactionConstraints() {}

  public static ReactionConstraintRegistry buildRegistry() {
    var registry = new ReactionConstraintRegistry();

    registry.register(
        ComponentInsertedIntoSystemReaction.class,
        new ConstraintRef("model::Component", "ComponentHasCorrespondingEntity"),
        new ConstraintRef("model::Component", "ComponentHasCorrespondence"),
        new ConstraintRef("model::Component", "ComponentNameMatchesEntityName"));

    registry.register(
        ComponentRenamedReaction.class,
        new ConstraintRef("model::Component", "ComponentNameMatchesEntityName"));

    registry.register(
        ComponentDeletedReaction.class,
        new ConstraintRef("model::Component", "ComponentHasCorrespondingEntity"),
        new ConstraintRef("model::Component", "ComponentHasCorrespondence"));

    registry.register(
        LinkInsertedIntoSystemReaction.class,
        new ConstraintRef("model2::Link", "LinkHasAtLeastTwoEntities"));

    registry.register(
        ComponentInsertedIntoLinkReaction.class,
        new ConstraintRef("model2::Link", "LinkHasAtLeastTwoEntities"));

    return registry;
  }
}
