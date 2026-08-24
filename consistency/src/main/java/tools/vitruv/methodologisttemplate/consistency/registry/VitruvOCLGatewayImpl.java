package tools.vitruv.methodologisttemplate.consistency.registry;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import tools.vitruv.dsls.vitruvocl.pipeline.ConstraintResult;
import tools.vitruv.dsls.vitruvocl.pipeline.VitruvOCL;

/**
 * Wraps {@link VitruvOCL#evaluateConstraints(Path)} for one or more constraint files, combining
 * their results. Multiple files matter here because {@code ProjectReactionConstraints} maps a
 * single Reaction to constraints spread across more than one {@code .ocl} file (e.g. both
 * {@code constraints.ocl} and {@code prepost-example.ocl}) -- evaluating only one of them would
 * make every {@link ConstraintRef} declared solely in the other look "unknown" to {@link
 * ConstraintEvaluationCoordinator}'s validation, even though it is perfectly valid.
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
 * first line via regex, matching the {@code context <namespace>::<Type> (inv|pre|post) <Name>:}
 * syntax documented in {@code constraints.ocl}. {@code pre}/{@code post} are OCL# keywords too
 * (see {@code prepost-example.ocl}) — as of the currently used vitruvocl-language snapshot they
 * parse and type-check like {@code inv} but are not yet evaluated for truthiness by the runtime
 * (its {@code EvaluationVisitor} has no {@code visitPreCS}/{@code visitPostCS} override), so they
 * are still matched here for forward-compatibility and so a compile-time failure on a pre/post
 * declaration (which does reach {@link ConstraintResult#getConstraint()}) parses correctly
 * instead of throwing.
 */
public final class VitruvOCLGatewayImpl implements VitruvOCLGateway {

  private static final Pattern CONSTRAINT_HEADER =
      Pattern.compile("context\\s+(\\S+)\\s+(?:inv|pre|post)\\s+(\\w+)\\s*:");

  private final List<Path> constraintFiles;

  public VitruvOCLGatewayImpl(Path... constraintFiles) {
    this.constraintFiles = List.of(constraintFiles);
  }

  @Override
  public List<EvaluatedConstraint> evaluateAll() {
    return constraintFiles.stream()
        .map(VitruvOCL::evaluateConstraints)
        .flatMap(result -> result.getResults().stream())
        .map(this::toEvaluatedConstraint)
        .collect(Collectors.toList());
  }

  private EvaluatedConstraint toEvaluatedConstraint(ConstraintResult constraintResult) {
    ConstraintRef ref = parseRef(constraintResult.getConstraint());
    boolean satisfied = constraintResult.isSuccess() && constraintResult.isSatisfied();
    return new EvaluatedConstraint(
        ref.contextType(), ref.constraintName(), satisfied, constraintResult.toString());
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
