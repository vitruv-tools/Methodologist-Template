package mir.reactions.model2Model2;

import java.util.function.Function;
import mir.routines.model2Model2.Model2Model2RoutinesFacade;
import org.eclipse.xtext.xbase.lib.Extension;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.root.InsertRootEObject;
import tools.vitruv.dsls.reactions.runtime.reactions.AbstractReaction;
import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutine;
import tools.vitruv.dsls.reactions.runtime.routines.RoutinesFacade;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;

@SuppressWarnings("all")
public class SystemInsertedAsRootReaction extends AbstractReaction {
  private InsertRootEObject<tools.vitruv.methodologisttemplate.model.model.System> insertChange;

  public SystemInsertedAsRootReaction(final Function<ReactionExecutionState, RoutinesFacade> routinesFacadeGenerator) {
    super(routinesFacadeGenerator);
  }

  private static class Call extends AbstractRoutine.Update {
    public Call(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public void updateModels(final InsertRootEObject insertChange, final tools.vitruv.methodologisttemplate.model.model.System newValue, final int index, @Extension final Model2Model2RoutinesFacade _routinesFacade) {
      _routinesFacade.createAndRegisterRoot(newValue);
    }
  }

  public boolean isCurrentChangeMatchingTrigger(final EChange change) {
    if (!(change instanceof InsertRootEObject<?>)) {
    	return false;
    }
    
    InsertRootEObject<tools.vitruv.methodologisttemplate.model.model.System> _localTypedChange = (InsertRootEObject<tools.vitruv.methodologisttemplate.model.model.System>) change;
    if (!(_localTypedChange.getNewValue() instanceof tools.vitruv.methodologisttemplate.model.model.System)) {
    	return false;
    }
    this.insertChange = (InsertRootEObject<tools.vitruv.methodologisttemplate.model.model.System>) change;
    return true;
  }

  public void executeReaction(final EChange change, final ReactionExecutionState executionState, final RoutinesFacade routinesFacadeUntyped) {
    mir.routines.model2Model2.Model2Model2RoutinesFacade routinesFacade = (mir.routines.model2Model2.Model2Model2RoutinesFacade)routinesFacadeUntyped;
    if (!isCurrentChangeMatchingTrigger(change)) {
    	return;
    }
    tools.vitruv.methodologisttemplate.model.model.System newValue = (tools.vitruv.methodologisttemplate.model.model.System)insertChange.getNewValue();
    int index = insertChange.getIndex();
    if (getLogger().isTraceEnabled()) {
    	getLogger().trace("Passed complete precondition check of Reaction " + this.getClass().getName());
    }
    
    new mir.reactions.model2Model2.SystemInsertedAsRootReaction.Call(executionState).updateModels(insertChange, newValue, index, routinesFacade);
  }
}
