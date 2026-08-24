package tools.vitruv.methodologisttemplate.consistency.registry;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.dsls.vitruvocl.common.CompileError;
import tools.vitruv.dsls.vitruvocl.pipeline.ConstraintResult;
import tools.vitruv.dsls.vitruvocl.pipeline.FileError;
import tools.vitruv.dsls.vitruvocl.pipeline.VitruvOCL;
import tools.vitruv.dsls.vitruvocl.pipeline.Warning;

/**
 * Wraps {@link VitruvOCL#evaluateConstraints(Path)}/{@link VitruvOCL#evaluateConstraints(Path,
 * List)} for one or more constraint files, combining their results. Multiple files matter here
 * because {@code ProjectReactionConstraints} maps a single Reaction to constraints spread across
 * more than one {@code .ocl} file (e.g. both {@code constraints.ocl} and {@code
 * prepost-example.ocl}) -- evaluating only one of them would make every {@link ConstraintRef}
 * declared solely in the other look "unknown" to {@link ConstraintEvaluationCoordinator}'s
 * validation, even though it is perfectly valid.
 *
 * <p>{@link ConstraintResult#getConstraint()} does not expose the context type, constraint kind,
 * and constraint name as separate fields — it returns the raw declaration text, e.g.
 *
 * <pre>{@code
 * context model::Component inv ComponentHasCorrespondingEntity:
 *     @severity CRITICAL
 *     ...
 * }</pre>
 *
 * so {@link #parseHeader(String)} extracts {@code (contextType, kind, constraintName)} from the
 * first line via regex, matching the {@code context <namespace>::<Type> (inv|pre|post) <Name>:}
 * syntax documented in {@code constraints.ocl}.
 *
 * <p>{@link #buildMessage(ConstraintResult)} deliberately does not use {@link
 * ConstraintResult#toString()} -- that wraps each violation in vitruvocl-language's own display
 * formatting (a "✓ SATISFIED"/"✗ VIOLATED" header, then a "WARNINGS (n):" section with a
 * dashed-line box around each one, even though these are genuine violations, not warnings). It
 * reads the same information from the structured accessors ({@link Warning#getMessage()}, {@link
 * CompileError#getMessage()}, {@link FileError#getMessage()}) instead, which is exactly the
 * {@code @message} text (with {@code {self...}} interpolation already applied) and nothing else.
 */
public final class VitruvOCLGatewayImpl implements VitruvOCLGateway {

  private static final Pattern CONSTRAINT_HEADER =
      Pattern.compile("context\\s+(\\S+)\\s+(inv|pre|post)\\s+(\\w+)\\s*:");

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

  @Override
  public List<EvaluatedConstraint> evaluateAll(List<EChange<EObject>> transaction) {
    return constraintFiles.stream()
        .map(file -> VitruvOCL.evaluateConstraints(file, transaction))
        .flatMap(result -> result.getResults().stream())
        .map(this::toEvaluatedConstraint)
        .collect(Collectors.toList());
  }

  private EvaluatedConstraint toEvaluatedConstraint(ConstraintResult constraintResult) {
    Matcher header = parseHeader(constraintResult.getConstraint());
    boolean satisfied = constraintResult.isSuccess() && constraintResult.isSatisfied();
    return new EvaluatedConstraint(
        header.group(1),
        header.group(3),
        ConstraintKind.fromKeyword(header.group(2)),
        satisfied,
        buildMessage(constraintResult));
  }

  private static String buildMessage(ConstraintResult constraintResult) {
    if (!constraintResult.isSuccess()) {
      return Stream.concat(
              constraintResult.getCompilerErrors().stream().map(CompileError::getMessage),
              constraintResult.getFileErrors().stream().map(FileError::getMessage))
          .collect(Collectors.joining("; "));
    }
    if (!constraintResult.isSatisfied()) {
      return constraintResult.getWarnings().stream()
          .filter(warning -> warning.getType() == Warning.WarningType.CONSTRAINT_VIOLATION)
          .map(Warning::getMessage)
          .collect(Collectors.joining("; "));
    }
    return "satisfied";
  }

  private static Matcher parseHeader(String rawConstraintDeclaration) {
    Matcher matcher = CONSTRAINT_HEADER.matcher(rawConstraintDeclaration);
    if (!matcher.find()) {
      throw new IllegalStateException(
          "Could not parse (contextType, kind, constraintName) from constraint declaration: "
              + rawConstraintDeclaration);
    }
    return matcher;
  }
}
