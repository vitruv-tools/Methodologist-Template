package tools.vitruv.methodologisttemplate.consistency.registry;

import java.util.ArrayList;
import java.util.List;

/**
 * Every violated pre-/postcondition or invariant and what the user decided about it, in order.
 * Approved violations stay in the models, so the log is the record of which checks were
 * overridden.
 */
public final class ContractLog {

  /**
   * One decision about one violated constraint.
   *
   * @param violation the violated precondition, postcondition or invariant
   * @param approved {@code true} if the user let the commit continue despite the violation
   */
  public record Decision(ViolatedConstraint violation, boolean approved) {}

  private final List<Decision> decisions = new ArrayList<>();

  public synchronized void record(ViolatedConstraint violation, boolean approved) {
    decisions.add(new Decision(violation, approved));
  }

  public synchronized List<Decision> decisions() {
    return List.copyOf(decisions);
  }
}
