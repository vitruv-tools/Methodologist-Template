package tools.vitruv.methodologisttemplate.consistency.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.vitruv.dsls.reactions.runtime.reactions.Reaction;

/**
 * Associates a Reaction (identified by its generated class — not a String name, so a rename
 * fails the build instead of silently orphaning a registry entry) with the set of OCL#
 * pre-/postcondition constraints that must hold after it executes.
 *
 * <p>Instance-based (not static) so tests can create isolated registries; production code is
 * expected to build one instance at startup and populate it via {@link #register}, mirroring
 * the existing generated {@code setup()} registration idiom used elsewhere in this module.
 */
public final class ReactionConstraintRegistry {

  private final Map<Class<? extends Reaction>, List<ConstraintRef>> reactionToConstraints =
      new LinkedHashMap<>();

  public void register(Class<? extends Reaction> reactionClass, ConstraintRef... constraints) {
    reactionToConstraints
        .computeIfAbsent(reactionClass, k -> new ArrayList<>())
        .addAll(List.of(constraints));
  }

  public List<ConstraintRef> getConstraintsFor(Class<? extends Reaction> reactionClass) {
    return List.copyOf(reactionToConstraints.getOrDefault(reactionClass, List.of()));
  }

  public Set<ConstraintRef> getConstraintsForAll(
      Collection<Class<? extends Reaction>> reactionClasses) {
    var result = new LinkedHashSet<ConstraintRef>();
    reactionClasses.forEach(r -> result.addAll(getConstraintsFor(r)));
    return result;
  }
}
