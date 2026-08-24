package tools.vitruv.methodologisttemplate.consistency.registry;

import java.util.List;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;

/** Seam over VitruvOCL's whole-file evaluation, so callers can be unit-tested without a VSUM. */
public interface VitruvOCLGateway {

  /**
   * Every constraint declared in the evaluated file(s), regardless of whether it was satisfied,
   * evaluated with no transaction context at all -- {@code pre}/{@code post} blocks are therefore
   * skipped outright (reported as vacuously satisfied) rather than genuinely evaluated.
   *
   * <p><b>Deliberately not implemented as {@code evaluateAll(List.of())}:</b> an empty transaction
   * is not the same thing as no transaction context. {@link #evaluateAll(List)} treats an empty
   * list as "the transaction genuinely made zero changes" and evaluates {@code pre}/{@code post}
   * for real against it -- e.g. {@code OCLisModified} would then evaluate to {@code false} for
   * every instance, which is a real (if usually uninteresting) result. This method instead means
   * "there is no transaction to evaluate against at all", which must skip {@code pre}/{@code
   * post} outright to avoid exactly that kind of vacuous-empty-transaction false violation. Prefer
   * {@link #evaluateAll(List)} whenever a real transaction is available, such as from a
   * Reaction-execution hook -- see {@link ReactionConstraintCheckingListener}.
   */
  List<EvaluatedConstraint> evaluateAll();

  /**
   * Every constraint declared in the evaluated file(s), regardless of whether it was satisfied,
   * evaluated against {@code transaction} -- unlike {@link #evaluateAll()}, {@code pre}/{@code
   * post} blocks are genuinely evaluated here, not skipped.
   *
   * @param transaction the ordered list of atomic changes between pre-state and the current
   *     post-state, evaluated by {@code @pre}/{@code OCLisNew}/{@code OCLisModified}/{@code
   *     OCLisDeleted} (may be empty)
   */
  List<EvaluatedConstraint> evaluateAll(List<EChange<EObject>> transaction);
}
