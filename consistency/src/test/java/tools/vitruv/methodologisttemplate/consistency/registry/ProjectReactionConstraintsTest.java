package tools.vitruv.methodologisttemplate.consistency.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import mir.reactions.model2Model2.ComponentInsertedIntoSystemReaction;
import mir.reactions.model2Model2.LinkInsertedIntoSystemReaction;
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
  void linkInsertedIntoSystemHasExpectedConstraints() {
    ReactionConstraintRegistry registry = ProjectReactionConstraints.buildRegistry();

    var constraints = registry.getConstraintsFor(LinkInsertedIntoSystemReaction.class);

    assertTrue(constraints.contains(new ConstraintRef("model2::Link", "LinkHasAtLeastTwoEntities")));
  }

  @Test
  void reactionWithNoMatchingConstraintHasNoRegistrations() {
    ReactionConstraintRegistry registry = ProjectReactionConstraints.buildRegistry();

    assertEquals(List.of(), registry.getConstraintsFor(SystemInsertedAsRootReaction.class));
  }
}
