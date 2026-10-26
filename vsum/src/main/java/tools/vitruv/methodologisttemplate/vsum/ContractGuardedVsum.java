package tools.vitruv.methodologisttemplate.vsum;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import tools.vitruv.change.interaction.InteractionResultProvider;
import tools.vitruv.change.propagation.ChangePropagationMode;
import tools.vitruv.dsls.vitruvocl.pipeline.VitruvOCL;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.ViewSource;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;
import tools.vitruv.methodologisttemplate.consistency.registry.ConstraintEvaluationCoordinator;
import tools.vitruv.methodologisttemplate.consistency.registry.ConstraintViolationsDetectedException;
import tools.vitruv.methodologisttemplate.consistency.registry.ContractLog;
import tools.vitruv.methodologisttemplate.consistency.registry.HookedModel2Model2ChangePropagationSpecification;
import tools.vitruv.methodologisttemplate.consistency.registry.ProjectReactionConstraints;
import tools.vitruv.methodologisttemplate.consistency.registry.ViolatedConstraint;
import tools.vitruv.methodologisttemplate.consistency.registry.VitruvOCLGatewayImpl;

/**
 * A VSUM whose commits are checked on two levels, and rolled back completely if the user rejects a
 * violation:
 *
 * <ol>
 *   <li>during change propagation, the contract of every Reaction execution (its pre- and
 *       postconditions, see {@code HookedReaction} and {@code ContractEnforcer});
 *   <li>after the commit, every invariant, because Reactions that each satisfy their own contract
 *       can still leave the system inconsistent together, and a Reaction that does nothing although
 *       it should leaves no change for its postcondition to inspect.
 * </ol>
 *
 * <p>Rollback: a rejected contract violation is thrown from inside a Reaction, before Vitruvius
 * saves the models ({@code VirtualModelImpl.propagateChange} saves only after propagation has
 * finished), so the storage folder still holds the state before the commit. The invariants,
 * however, can only be checked after the commit has been saved. The storage folder is therefore
 * copied before every commit and copied back if the user rejects an invariant violation. In both
 * cases the in-memory VSUM is discarded and reloaded; views of the discarded VSUM must not be used
 * afterwards -- obtain new ones from {@link #vsum()}.
 */
public final class ContractGuardedVsum {

  /**
   * The result of one guarded commit.
   *
   * @param rejection the violation the user rejected, or {@code null} if the commit was kept
   */
  public record CommitOutcome(ConstraintViolationsDetectedException rejection) {

    public boolean applied() {
      return rejection == null;
    }
  }

  private final Path storageFolder;
  private final Supplier<InternalVirtualModel> loader;
  private final ConstraintEvaluationCoordinator coordinator;
  private final Predicate<List<ViolatedConstraint>> keepInconsistentCommit;
  private final ContractLog log;
  private InternalVirtualModel vsum;

  /**
   * @param initial the VSUM to start with, persisted in {@code storageFolder}
   * @param storageFolder the VSUM's storage folder
   * @param loader loads the VSUM from {@code storageFolder}, with contracts wired in
   * @param coordinator evaluates the invariants after every commit
   * @param keepInconsistentCommit asked when invariants are violated after a commit; {@code true}
   *     keeps the commit, {@code false} rolls it back
   * @param log records the decisions about violated invariants
   */
  public ContractGuardedVsum(
      InternalVirtualModel initial,
      Path storageFolder,
      Supplier<InternalVirtualModel> loader,
      ConstraintEvaluationCoordinator coordinator,
      Predicate<List<ViolatedConstraint>> keepInconsistentCommit,
      ContractLog log) {
    this.vsum = initial;
    this.storageFolder = storageFolder;
    this.loader = loader;
    this.coordinator = coordinator;
    this.keepInconsistentCommit = keepInconsistentCommit;
    this.log = log;
  }

  /**
   * Builds (or reloads) the VSUM in {@code storageFolder} with {@link
   * HookedModel2Model2ChangePropagationSpecification}, registers it with {@link VitruvOCL}, and
   * wires in the contracts of {@link ProjectReactionConstraints} and the invariants, both read from
   * {@code constraintFiles}.
   *
   * @param resultProvider answers the user interactions, including the confirmation asked for a
   *     violated contract
   */
  public static ContractGuardedVsum build(
      Path storageFolder,
      InteractionResultProvider resultProvider,
      Predicate<List<ViolatedConstraint>> keepInconsistentCommit,
      ContractLog log,
      Path... constraintFiles) {
    var coordinator =
        new ConstraintEvaluationCoordinator(
            ProjectReactionConstraints.buildRegistry(), new VitruvOCLGatewayImpl(constraintFiles));
    Supplier<InternalVirtualModel> loader =
        () -> {
          var specification = new HookedModel2Model2ChangePropagationSpecification();
          InternalVirtualModel model;
          try {
            model =
                new VirtualModelBuilder()
                    .withStorageFolder(storageFolder)
                    .withUserInteractorForResultProvider(resultProvider)
                    .withChangePropagationSpecifications(specification)
                    .buildAndInitialize();
          } catch (IOException e) {
            throw new UncheckedIOException("Could not load the VSUM from " + storageFolder, e);
          }
          model.setChangePropagationMode(ChangePropagationMode.TRANSITIVE_CYCLIC);
          VitruvOCL.registerVSUM(model);
          specification.getContractEnforcer().bind(coordinator, log, () -> resourceSetOf(model));
          return model;
        };
    return new ContractGuardedVsum(
        loader.get(), storageFolder, loader, coordinator, keepInconsistentCommit, log);
  }

  /** The ResourceSet of the VSUM's models, or {@code null} while it has none. */
  static ResourceSet resourceSetOf(InternalVirtualModel model) {
    return ((ViewSource) model)
        .getViewSourceModels().stream()
            .map(Resource::getResourceSet)
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .orElse(null);
  }

  /** The current VSUM. After a rollback, this is a new instance. */
  public InternalVirtualModel vsum() {
    return vsum;
  }

  /** Commits {@code view}; if the user rejects a violation, restores the state before it. */
  public CommitOutcome commit(CommittableView view) {
    Path backup = backupStorageFolder();
    try {
      try {
        view.commitChanges();
      } catch (RuntimeException | AssertionError e) {
        ConstraintViolationsDetectedException rejection = findRejection(e);
        if (rejection == null) {
          throw e;
        }
        reload(); // nothing of the commit was saved
        return new CommitOutcome(rejection);
      }
      List<ViolatedConstraint> inconsistencies = coordinator.evaluateInvariants();
      if (inconsistencies.isEmpty()) {
        return new CommitOutcome(null);
      }
      boolean keep = keepInconsistentCommit.test(inconsistencies);
      inconsistencies.forEach(v -> log.record(v, keep));
      if (keep) {
        return new CommitOutcome(null);
      }
      vsum.dispose();
      restore(backup);
      vsum = loader.get();
      return new CommitOutcome(new ConstraintViolationsDetectedException(inconsistencies));
    } finally {
      deleteRecursively(backup);
    }
  }

  private void reload() {
    vsum.dispose();
    vsum = loader.get();
  }

  private static ConstraintViolationsDetectedException findRejection(Throwable thrown) {
    for (Throwable t = thrown; t != null; t = t.getCause() == t ? null : t.getCause()) {
      if (t instanceof ConstraintViolationsDetectedException rejection) {
        return rejection;
      }
    }
    return null;
  }

  // ================================================================
  // Storage folder backup
  // ================================================================

  private Path backupStorageFolder() {
    Path backup = storageFolder.resolveSibling(storageFolder.getFileName() + ".before-commit");
    deleteRecursively(backup);
    copyRecursively(storageFolder, backup);
    return backup;
  }

  private void restore(Path backup) {
    deleteRecursively(storageFolder);
    copyRecursively(backup, storageFolder);
  }

  private static void copyRecursively(Path from, Path to) {
    if (!Files.exists(from)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(from)) {
      for (Path source : (Iterable<Path>) paths::iterator) {
        Path target = to.resolve(from.relativize(source).toString());
        if (Files.isDirectory(source)) {
          Files.createDirectories(target);
        } else {
          Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Could not copy " + from + " to " + to, e);
    }
  }

  private static void deleteRecursively(Path path) {
    if (!Files.exists(path)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(path)) {
      for (Path p : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
        Files.delete(p);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Could not delete " + path, e);
    }
  }
}
