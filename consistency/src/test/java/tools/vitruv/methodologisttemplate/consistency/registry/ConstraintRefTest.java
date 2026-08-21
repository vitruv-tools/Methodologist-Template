package tools.vitruv.methodologisttemplate.consistency.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ConstraintRefTest {

  @Test
  void equalRefsAreEqual() {
    var a = new ConstraintRef("model::System", "SystemInsertedAsRoot");
    var b = new ConstraintRef("model::System", "SystemInsertedAsRoot");
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void differentContextTypeMakesRefsUnequal() {
    var a = new ConstraintRef("model::System", "SystemInsertedAsRoot");
    var b = new ConstraintRef("model::Component", "SystemInsertedAsRoot");
    assertNotEquals(a, b);
  }

  @Test
  void toStringIsReadableForDiagnostics() {
    var ref = new ConstraintRef("model::System", "SystemInsertedAsRoot");
    assertEquals("model::System::SystemInsertedAsRoot", ref.toString());
  }

  @Test
  void rejectsNullOrBlankFields() {
    assertThrows(NullPointerException.class, () -> new ConstraintRef(null, "X"));
    assertThrows(NullPointerException.class, () -> new ConstraintRef("model::System", null));
    assertThrows(IllegalArgumentException.class, () -> new ConstraintRef("", "X"));
    assertThrows(IllegalArgumentException.class, () -> new ConstraintRef("model::System", ""));
  }
}
