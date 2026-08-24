package tools.vitruv.methodologisttemplate.consistency.registry;

import java.util.List;

/** Seam over VitruvOCL's whole-file evaluation, so callers can be unit-tested without a VSUM. */
public interface VitruvOCLGateway {

  /** Every constraint declared in the evaluated file(s), regardless of whether it was satisfied. */
  List<EvaluatedConstraint> evaluateAll();
}
