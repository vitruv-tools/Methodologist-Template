package tools.vitruv.methodologisttemplate.consistency.registry;

import java.nio.file.Path;
import java.util.ArrayList;
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
 * Implements {@link VitruvOCLGateway} on top of {@link VitruvOCL}, for one or more constraint
 * files. Multiple files matter because a Reaction's contract and the invariants checked after a
 * commit can be spread across files (e.g. {@code constraints.ocl} and {@code prepost-example.ocl}).
 *
 * <p>The files are split into their {@code inv}/{@code pre}/{@code post} units by VitruvOCL itself:
 * {@link VitruvOCL#evaluateConstraints(Path)} returns one {@link ConstraintResult} per unit whose
 * {@link ConstraintResult#getConstraint()} is the unit's raw source text, e.g.
 *
 * <pre>{@code
 * context model::Component pre ComponentHasNameBeforeSync:
 *     @severity CRITICAL
 *     ...
 * }</pre>
 *
 * {@link #parseHeader(String)} extracts {@code (contextType, kind, constraintName)} from its first
 * line. A single pre- or postcondition is then evaluated on its own with {@link
 * VitruvOCL#evaluateConstraint(String, List)}.
 *
 * <p>{@link #buildMessage(ConstraintResult)} deliberately does not use {@link
 * ConstraintResult#toString()}, which wraps each violation in VitruvOCL's own display formatting;
 * it reads the {@code @message} diagnostics (with {@code {self...}} interpolation applied) from the
 * structured accessors instead.
 */
public final class VitruvOCLGatewayImpl implements VitruvOCLGateway {

  private static final Pattern CONSTRAINT_HEADER =
      Pattern.compile("context\\s+(\\S+)\\s+(inv|pre|post)\\s+(\\w+)\\s*:");

  private final List<Path> constraintFiles;
  private List<DeclaredConstraint> declarations;

  public VitruvOCLGatewayImpl(Path... constraintFiles) {
    this.constraintFiles = List.of(constraintFiles);
  }

  @Override
  public synchronized List<DeclaredConstraint> declarations() {
    if (declarations == null) {
      List<DeclaredConstraint> declared = new ArrayList<>();
      for (Path file : constraintFiles) {
        for (ConstraintResult unit : VitruvOCL.evaluateConstraints(file).getResults()) {
          declared.add(declarationOf(unit));
        }
      }
      declarations = List.copyOf(declared);
    }
    return declarations;
  }

  @Override
  public EvaluatedConstraint evaluate(
      DeclaredConstraint constraint, List<EChange<EObject>> transaction) {
    return toEvaluatedConstraint(
        constraint, VitruvOCL.evaluateConstraint(constraint.text(), transaction));
  }

  @Override
  public List<EvaluatedConstraint> evaluateInvariants() {
    List<EvaluatedConstraint> results = new ArrayList<>();
    for (Path file : constraintFiles) {
      for (ConstraintResult unit : VitruvOCL.evaluateConstraints(file).getResults()) {
        DeclaredConstraint declared = declarationOf(unit);
        if (declared.kind() == ConstraintKind.INVARIANT) {
          results.add(toEvaluatedConstraint(declared, unit));
        }
      }
    }
    return results;
  }

  private static DeclaredConstraint declarationOf(ConstraintResult unit) {
    Matcher header = parseHeader(unit.getConstraint());
    return new DeclaredConstraint(
        new ConstraintRef(header.group(1), header.group(3)),
        ConstraintKind.fromKeyword(header.group(2)),
        unit.getConstraint());
  }

  private static EvaluatedConstraint toEvaluatedConstraint(
      DeclaredConstraint constraint, ConstraintResult result) {
    return new EvaluatedConstraint(
        constraint.ref().contextType(),
        constraint.ref().constraintName(),
        constraint.kind(),
        result.isSuccess() && result.isSatisfied(),
        buildMessage(result));
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
