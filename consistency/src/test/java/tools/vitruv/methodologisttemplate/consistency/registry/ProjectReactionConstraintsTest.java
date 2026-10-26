package tools.vitruv.methodologisttemplate.consistency.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import mir.reactions.model2Model2.ComponentDeletedReaction;
import mir.reactions.model2Model2.ComponentInsertedIntoLinkReaction;
import mir.reactions.model2Model2.ComponentInsertedIntoSystemReaction;
import mir.reactions.model2Model2.ComponentRenamedReaction;
import mir.reactions.model2Model2.LinkInsertedIntoSystemReaction;
import mir.reactions.model2Model2.ProtocolInsertedIntoSystemReaction;
import mir.reactions.model2Model2.SystemInsertedAsRootReaction;
import org.junit.jupiter.api.Test;

class ProjectReactionConstraintsTest {

  private final ReactionConstraintRegistry registry = ProjectReactionConstraints.buildRegistry();

  @Test
  void componentInsertedIntoSystemHasPreAndPostconditions() {
    var constraints = registry.getConstraintsFor(ComponentInsertedIntoSystemReaction.class);

    assertTrue(constraints.contains(new ConstraintRef("model::Component", "ComponentHasNameBeforeSync")));
    assertTrue(
        constraints.contains(new ConstraintRef("model2::Root", "ComponentInsertCreatedExactlyOneEntity")));
    assertTrue(constraints.contains(new ConstraintRef("model2::Entity", "NewEntityMatchesComponent")));
  }

  @Test
  void componentRenamedHasPreAndPostconditions() {
    var constraints = registry.getConstraintsFor(ComponentRenamedReaction.class);

    assertTrue(constraints.contains(new ConstraintRef("model::Component", "ComponentHasNameBeforeSync")));
    assertTrue(
        constraints.contains(new ConstraintRef("model2::Entity", "RenamedEntityMatchesComponent")));
  }

  @Test
  void componentDeletedHasAPostcondition() {
    assertEquals(
        List.of(new ConstraintRef("model2::Root", "ComponentDeleteRemovedExactlyOneEntity")),
        registry.getConstraintsFor(ComponentDeletedReaction.class));
  }

  @Test
  void linkReactionsHaveTheirContracts() {
    var linkInserted = registry.getConstraintsFor(LinkInsertedIntoSystemReaction.class);
    assertTrue(
        linkInserted.contains(new ConstraintRef("model::Link", "LinkHasAtLeastOneComponentBeforeSync")));
    assertTrue(linkInserted.contains(new ConstraintRef("model2::Link", "NewLinkCorrespondsToLink")));

    assertEquals(
        List.of(new ConstraintRef("model2::Link", "LinkEntitiesCorrespondToLinkComponents")),
        registry.getConstraintsFor(ComponentInsertedIntoLinkReaction.class));
  }

  @Test
  void protocolInsertedIntoSystemHasPreAndPostconditions() {
    var constraints = registry.getConstraintsFor(ProtocolInsertedIntoSystemReaction.class);

    assertTrue(constraints.contains(new ConstraintRef("model::Protocol", "ProtocolHasNameBeforeSync")));
    assertTrue(
        constraints.contains(
            new ConstraintRef("model2::CommunicationStandard", "NewStandardMatchesProtocol")));
  }

  @Test
  void invariantsAreNotRegisteredBecauseTheyAreCheckedAfterEveryCommit() {
    assertTrue(
        registry.allConstraints().stream()
            .noneMatch(ref -> ref.constraintName().equals("LinkHasAtLeastTwoEntities")));
  }

  @Test
  void reactionWithoutContractHasNoRegistrations() {
    assertEquals(List.of(), registry.getConstraintsFor(SystemInsertedAsRootReaction.class));
  }
}
