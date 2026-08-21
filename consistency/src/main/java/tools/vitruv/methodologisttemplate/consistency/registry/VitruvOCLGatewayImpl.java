package tools.vitruv.methodologisttemplate.consistency.registry;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import tools.vitruv.dsls.vitruvocl.pipeline.ConstraintResult;
import tools.vitruv.dsls.vitruvocl.pipeline.VitruvOCL;

/**
 * Wraps {@link VitruvOCL#evaluateConstraints(Path)} for one constraint file.
 *
 * <p>{@link ConstraintResult#getConstraint()} does not expose the context type and constraint
 * name as separate fields — it returns the raw declaration text, e.g.
 *
 * <pre>{@code
 * context model::Component inv ComponentHasCorrespondingEntity:
 *     @severity CRITICAL
 *     ...
 * }</pre>
 *
 * so {@link #parseRef(String)} extracts the {@code (contextType, constraintName)} pair from the
 * first line via regex, matching the {@code context <namespace>::<Type> inv <Name>:} syntax
 * documented in {@code constraints.ocl}.
 */
public final class VitruvOCLGatewayImpl implements VitruvOCLGateway {

  private static final Pattern CONSTRAINT_HEADER =
      Pattern.compile("context\\s+(\\S+)\\s+inv\\s+(\\w+)\\s*:");

  private final Path constraintFile;

  public VitruvOCLGatewayImpl(Path constraintFile) {
    this.constraintFile = constraintFile;
  }

  @Override
  public List<ViolatedConstraint> evaluateAll() {
    var result = VitruvOCL.evaluateConstraints(constraintFile);
    return Stream.concat(
            result.getViolatedConstraints().stream(), result.getFailedConstraints().stream())
        .map(this::toViolatedConstraint)
        .collect(Collectors.toList());
  }

  private ViolatedConstraint toViolatedConstraint(ConstraintResult constraintResult) {
    ConstraintRef ref = parseRef(constraintResult.getConstraint());
    return new ViolatedConstraint(
        ref.contextType(), ref.constraintName(), constraintResult.toString());
  }

  private static ConstraintRef parseRef(String rawConstraintDeclaration) {
    Matcher matcher = CONSTRAINT_HEADER.matcher(rawConstraintDeclaration);
    if (!matcher.find()) {
      throw new IllegalStateException(
          "Could not parse (contextType, constraintName) from constraint declaration: "
              + rawConstraintDeclaration);
    }
    return new ConstraintRef(matcher.group(1), matcher.group(2));
  }
}
