package tools.vitruv.methodologisttemplate.consistency.registry;

import java.util.List;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;

/** Seam over VitruvOCL, so callers can be unit-tested without a VSUM. */
public interface VitruvOCLGateway {

  /**
   * One {@code inv}/{@code pre}/{@code post} unit declared in the evaluated file(s).
   *
   * @param ref the unit's (contextType, constraintName)
   * @param kind invariant, precondition or postcondition
   * @param text the unit's source text, as VitruvOCL evaluates it
   */
  record DeclaredConstraint(ConstraintRef ref, ConstraintKind kind, String text) {}

  /** Every named constraint declared in the evaluated file(s). Requires a registered VSUM. */
  List<DeclaredConstraint> declarations();

  /**
   * Evaluates one pre- or postcondition against {@code transaction}: {@code @pre}, {@code
   * OCLisNew}, {@code OCLisModified} and {@code OCLisDeleted} refer to these changes (may be
   * empty). Like an invariant, {@code self} ranges over all instances of the context type.
   */
  EvaluatedConstraint evaluate(DeclaredConstraint constraint, List<EChange<EObject>> transaction);

  /** Evaluates every invariant declared in the evaluated file(s) against the current state. */
  List<EvaluatedConstraint> evaluateInvariants();
}
