package tools.vitruv.methodologisttemplate.consistency.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ConstraintViolationsDetectedExceptionTest {

  @Test
  void namesTheSpecificKindForASinglePostconditionViolation() {
    var violation =
        new ViolatedConstraint(
            "model2::CommunicationStandard",
            "CommunicationStandardNameMatchesProtocolAfterSync",
            ConstraintKind.POSTCONDITION,
            "the diagnostic vitruvocl produced");

    var exception = new ConstraintViolationsDetectedException(List.of(violation));

    assertEquals(
        "1 postcondition(s) violated by the Reaction that just ran -- its result was rejected"
            + " and the commit was stopped:\n"
            + "the diagnostic vitruvocl produced",
        exception.getMessage());
  }

  @Test
  void namesTheSpecificKindForASinglePreconditionViolation() {
    var violation =
        new ViolatedConstraint(
            "model::Component", "ComponentHasNameBeforeSync", ConstraintKind.PRECONDITION, "diagnostic text");

    var exception = new ConstraintViolationsDetectedException(List.of(violation));

    // ContractEnforcer throws this before the Reaction's own execute() ever runs.
    assertTrue(
        exception
            .getMessage()
            .startsWith(
                "1 precondition(s) violated -- the Reaction was not executed and the commit was"
                    + " stopped:"));
  }

  @Test
  void groupsMixedKindsInASingleSummaryLine() {
    var pre =
        new ViolatedConstraint("model::Component", "SomePre", ConstraintKind.PRECONDITION, "pre diagnostic");
    var post =
        new ViolatedConstraint("model::Component", "SomePost", ConstraintKind.POSTCONDITION, "post diagnostic");

    var exception = new ConstraintViolationsDetectedException(List.of(pre, post));

    assertTrue(
        exception
            .getMessage()
            .startsWith("1 precondition(s) and 1 postcondition(s) violated -- the commit was stopped:"));
  }

  @Test
  void saysThatTheCommitWasRolledBackForInvariantsCheckedAfterTheCommit() {
    var violation =
        new ViolatedConstraint(
            "model2::Link", "LinkHasAtLeastTwoEntities", ConstraintKind.INVARIANT, "diagnostic text");

    var exception = new ConstraintViolationsDetectedException(List.of(violation));

    assertEquals(
        "1 invariant(s) violated after the commit -- the commit was rolled back:\ndiagnostic text",
        exception.getMessage());
    assertEquals(List.of(violation), exception.violations());
  }

  @Test
  void doesNotIncludeTheRawRecordFieldDump() {
    var violation =
        new ViolatedConstraint(
            "model::Component", "ComponentHasNameBeforeSync", ConstraintKind.PRECONDITION, "diagnostic text");

    var exception = new ConstraintViolationsDetectedException(List.of(violation));

    assertTrue(
        exception.getMessage().contains("diagnostic text")
            && !exception.getMessage().contains("ViolatedConstraint["));
  }
}
