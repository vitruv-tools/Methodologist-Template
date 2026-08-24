package tools.vitruv.methodologisttemplate.consistency.registry;

import mir.reactions.model2Model2.ComponentDeletedReaction;
import mir.reactions.model2Model2.ComponentInsertedIntoLinkReaction;
import mir.reactions.model2Model2.ComponentInsertedIntoSystemReaction;
import mir.reactions.model2Model2.ComponentRenamedReaction;
import mir.reactions.model2Model2.LinkInsertedIntoSystemReaction;
import mir.reactions.model2Model2.ProtocolInsertedIntoSystemReaction;

/**
 * Hand-maintained association between this project's Reactions (declared in {@code
 * templateReactions.reactions}) and the OCL# constraints — both the invariants in {@code
 * constraints.ocl} and the pre-/postconditions in {@code prepost-example.ocl} — that must hold
 * around each Reaction's execution.
 *
 * <p>Extend this alongside adding a Reaction and its constraint to the relevant {@code .ocl}
 * file — this file is the single place that ties the two together. If a Reaction is renamed or
 * removed, the compiler will flag every registration referencing it, because the key is the
 * generated class, not a string.
 *
 * <p>{@code SystemInsertedAsRootReaction} and {@code ProtocolInsertedIntoLinkReaction} have no
 * entries below because neither {@code .ocl} file currently declares a constraint over {@code
 * model::System}, {@code model2::Root}, or the {@code model::Link[protocol]}/{@code
 * model2::Link[standard]} relationship.
 */
public final class ProjectReactionConstraints {

  private ProjectReactionConstraints() {}

  public static ReactionConstraintRegistry buildRegistry() {
    var registry = new ReactionConstraintRegistry();

    registry.register(
        ComponentInsertedIntoSystemReaction.class,
        new ConstraintRef("model::Component", "ComponentHasCorrespondingEntity"),
        new ConstraintRef("model::Component", "ComponentHasCorrespondence"),
        new ConstraintRef("model::Component", "ComponentNameMatchesEntityName"),
        // pre-/postconditions from prepost-example.ocl — genuinely evaluated when checked via
        // the automatic Reaction-Constraint hook (a real transaction), skipped when checked via
        // VitruvOCLGateway#evaluateAll() with no transaction — see that file's header comment.
        new ConstraintRef("model::Component", "ComponentHasNameBeforeSync"),
        new ConstraintRef("model::Component", "ComponentHasCorrespondingEntityAfterSync"));

    registry.register(
        ComponentRenamedReaction.class,
        new ConstraintRef("model::Component", "ComponentNameMatchesEntityName"),
        new ConstraintRef("model2::Entity", "EntityNameMatchesComponentAfterRename"));

    registry.register(
        ComponentDeletedReaction.class,
        new ConstraintRef("model::Component", "ComponentHasCorrespondingEntity"),
        new ConstraintRef("model::Component", "ComponentHasCorrespondence"));

    registry.register(
        LinkInsertedIntoSystemReaction.class,
        new ConstraintRef("model2::Link", "LinkHasAtLeastTwoEntities"),
        new ConstraintRef("model::Link", "LinkHasAtLeastOneComponentBeforeSync"),
        new ConstraintRef("model2::Link", "LinkHasAtLeastTwoEntitiesAfterSync"));

    registry.register(
        ComponentInsertedIntoLinkReaction.class,
        new ConstraintRef("model2::Link", "LinkHasAtLeastTwoEntities"),
        new ConstraintRef("model::Link", "LinkHasAtLeastOneComponentBeforeSync"),
        new ConstraintRef("model2::Link", "LinkHasAtLeastTwoEntitiesAfterSync"));

    registry.register(
        ProtocolInsertedIntoSystemReaction.class,
        new ConstraintRef("model::Protocol", "ProtocolHasNameBeforeSync"),
        new ConstraintRef("model2::CommunicationStandard", "CommunicationStandardNameMatchesProtocolAfterSync"));

    return registry;
  }
}
