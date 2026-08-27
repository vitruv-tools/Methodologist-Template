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
        "1 postcondition(s) violated by the Reaction(s) that just fired:\n"
            + "the diagnostic vitruvocl produced",
        exception.getMessage());
  }

  @Test
  void namesTheSpecificKindForASinglePreconditionViolation() {
    var violation =
        new ViolatedConstraint(
            "model::Component", "ComponentHasNameBeforeSync", ConstraintKind.PRECONDITION, "diagnostic text");

    var exception = new ConstraintViolationsDetectedException(List.of(violation));

    // PreconditionGuard throws this before the Reaction's own execute() ever runs -- "that just
    // fired" would be wrong here, since the whole point is that it did not.
    assertTrue(exception.getMessage().startsWith("1 precondition(s) violated -- the Reaction was not executed:"));
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
            .startsWith("1 precondition(s) and 1 postcondition(s) violated by the Reaction(s) that just fired:"));
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
