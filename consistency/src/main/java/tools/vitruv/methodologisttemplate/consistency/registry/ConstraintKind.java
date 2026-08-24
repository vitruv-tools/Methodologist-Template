package tools.vitruv.methodologisttemplate.consistency.registry;

/**
 * Which of OCL#'s three constraint kinds a declaration uses -- parsed from its {@code inv}/{@code
 * pre}/{@code post} keyword (see {@link VitruvOCLGatewayImpl}) so failures can be reported as
 * "invariant violated" / "precondition violated" / "postcondition violated" instead of a generic
 * "constraint violated".
 */
public enum ConstraintKind {
  INVARIANT("invariant"),
  PRECONDITION("precondition"),
  POSTCONDITION("postcondition");

  private final String label;

  ConstraintKind(String label) {
    this.label = label;
  }

  /** Singular, lowercase noun for this kind, e.g. {@code "postcondition"}. */
  public String label() {
    return label;
  }

  static ConstraintKind fromKeyword(String keyword) {
    return switch (keyword) {
      case "inv" -> INVARIANT;
      case "pre" -> PRECONDITION;
      case "post" -> POSTCONDITION;
      default -> throw new IllegalArgumentException("Unknown OCL# constraint keyword: " + keyword);
    };
  }
}
