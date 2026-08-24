package tools.vitruv.methodologisttemplate.consistency.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import mir.reactions.model2Model2.ComponentInsertedIntoSystemReaction;
import mir.reactions.model2Model2.ComponentRenamedReaction;
import mir.reactions.model2Model2.LinkInsertedIntoSystemReaction;
import mir.reactions.model2Model2.ProtocolInsertedIntoSystemReaction;
import mir.reactions.model2Model2.SystemInsertedAsRootReaction;
import org.junit.jupiter.api.Test;

class ProjectReactionConstraintsTest {

  @Test
  void componentInsertedIntoSystemHasExpectedConstraints() {
    ReactionConstraintRegistry registry = ProjectReactionConstraints.buildRegistry();

    var constraints = registry.getConstraintsFor(ComponentInsertedIntoSystemReaction.class);

    assertTrue(
        constraints.contains(new ConstraintRef("model::Component", "ComponentHasCorrespondingEntity")));
    assertTrue(
        constraints.contains(new ConstraintRef("model::Component", "ComponentHasCorrespondence")));
    assertTrue(
        constraints.contains(new ConstraintRef("model::Component", "ComponentNameMatchesEntityName")));
  }

  @Test
  void componentInsertedIntoSystemIncludesPrePostExampleConstraints() {
    ReactionConstraintRegistry registry = ProjectReactionConstraints.buildRegistry();

    var constraints = registry.getConstraintsFor(ComponentInsertedIntoSystemReaction.class);

    assertTrue(constraints.contains(new ConstraintRef("model::Component", "ComponentHasNameBeforeSync")));
    assertTrue(
        constraints.contains(
            new ConstraintRef("model::Component", "ComponentHasCorrespondingEntityAfterSync")));
  }

  @Test
  void componentRenamedIncludesPrePostExampleConstraint() {
    ReactionConstraintRegistry registry = ProjectReactionConstraints.buildRegistry();

    var constraints = registry.getConstraintsFor(ComponentRenamedReaction.class);

    assertTrue(
        constraints.contains(
            new ConstraintRef("model2::Entity", "EntityNameMatchesComponentAfterRename")));
  }

  @Test
  void linkInsertedIntoSystemHasExpectedConstraints() {
    ReactionConstraintRegistry registry = ProjectReactionConstraints.buildRegistry();

    var constraints = registry.getConstraintsFor(LinkInsertedIntoSystemReaction.class);

    assertTrue(constraints.contains(new ConstraintRef("model2::Link", "LinkHasAtLeastTwoEntities")));
  }

  @Test
  void linkInsertedIntoSystemIncludesPrePostExampleConstraints() {
    ReactionConstraintRegistry registry = ProjectReactionConstraints.buildRegistry();

    var constraints = registry.getConstraintsFor(LinkInsertedIntoSystemReaction.class);

    assertTrue(
        constraints.contains(new ConstraintRef("model::Link", "LinkHasAtLeastOneComponentBeforeSync")));
    assertTrue(
        constraints.contains(new ConstraintRef("model2::Link", "LinkHasAtLeastTwoEntitiesAfterSync")));
  }

  @Test
  void protocolInsertedIntoSystemIncludesPrePostExampleConstraints() {
    ReactionConstraintRegistry registry = ProjectReactionConstraints.buildRegistry();

    var constraints = registry.getConstraintsFor(ProtocolInsertedIntoSystemReaction.class);

    assertTrue(constraints.contains(new ConstraintRef("model::Protocol", "ProtocolHasNameBeforeSync")));
    assertTrue(
        constraints.contains(
            new ConstraintRef(
                "model2::CommunicationStandard", "CommunicationStandardNameMatchesProtocolAfterSync")));
  }

  @Test
  void reactionWithNoMatchingConstraintHasNoRegistrations() {
    ReactionConstraintRegistry registry = ProjectReactionConstraints.buildRegistry();

    assertEquals(List.of(), registry.getConstraintsFor(SystemInsertedAsRootReaction.class));
  }
}
