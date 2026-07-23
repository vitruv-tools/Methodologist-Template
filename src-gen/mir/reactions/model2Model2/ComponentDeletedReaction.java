package mir.reactions.model2Model2;

import java.util.function.Function;
import mir.routines.model2Model2.Model2Model2RoutinesFacade;
import org.eclipse.xtext.xbase.lib.Extension;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.eobject.DeleteEObject;
import tools.vitruv.dsls.reactions.runtime.reactions.AbstractReaction;
import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutine;
import tools.vitruv.dsls.reactions.runtime.routines.RoutinesFacade;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.methodologisttemplate.model.model.Component;

@SuppressWarnings("all")
public class ComponentDeletedReaction extends AbstractReaction {
  private DeleteEObject<Component> deleteChange;

  public ComponentDeletedReaction(final Function<ReactionExecutionState, RoutinesFacade> routinesFacadeGenerator) {
    super(routinesFacadeGenerator);
  }

  private static class Call extends AbstractRoutine.Update {
    public Call(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public void updateModels(final DeleteEObject deleteChange, final Component affectedEObject, @Extension final Model2Model2RoutinesFacade _routinesFacade) {
      _routinesFacade.deleteEntity(affectedEObject);
    }
  }

  public boolean isCurrentChangeMatchingTrigger(final EChange change) {
    if (!(change instanceof DeleteEObject<?>)) {
    	return false;
    }
    
    DeleteEObject<tools.vitruv.methodologisttemplate.model.model.Component> _localTypedChange = (DeleteEObject<tools.vitruv.methodologisttemplate.model.model.Component>) change;
    if (!(_localTypedChange.getAffectedElement() instanceof tools.vitruv.methodologisttemplate.model.model.Component)) {
    	return false;
    }
    this.deleteChange = (DeleteEObject<tools.vitruv.methodologisttemplate.model.model.Component>) change;
    return true;
  }

  public void executeReaction(final EChange change, final ReactionExecutionState executionState, final RoutinesFacade routinesFacadeUntyped) {
    mir.routines.model2Model2.Model2Model2RoutinesFacade routinesFacade = (mir.routines.model2Model2.Model2Model2RoutinesFacade)routinesFacadeUntyped;
    if (!isCurrentChangeMatchingTrigger(change)) {
    	return;
    }
    tools.vitruv.methodologisttemplate.model.model.Component affectedEObject = (tools.vitruv.methodologisttemplate.model.model.Component)deleteChange.getAffectedElement();
    if (getLogger().isTraceEnabled()) {
    	getLogger().trace("Passed complete precondition check of Reaction " + this.getClass().getName());
    }
    
    new mir.reactions.model2Model2.ComponentDeletedReaction.Call(executionState).updateModels(deleteChange, affectedEObject, routinesFacade);
  }
}
