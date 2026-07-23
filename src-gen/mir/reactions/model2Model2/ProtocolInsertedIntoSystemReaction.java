package mir.reactions.model2Model2;

import java.util.function.Function;
import mir.routines.model2Model2.Model2Model2RoutinesFacade;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.xbase.lib.Extension;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.feature.reference.InsertEReference;
import tools.vitruv.dsls.reactions.runtime.reactions.AbstractReaction;
import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutine;
import tools.vitruv.dsls.reactions.runtime.routines.RoutinesFacade;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.methodologisttemplate.model.model.Protocol;

@SuppressWarnings("all")
public class ProtocolInsertedIntoSystemReaction extends AbstractReaction {
  private InsertEReference<EObject> insertChange;

  public ProtocolInsertedIntoSystemReaction(final Function<ReactionExecutionState, RoutinesFacade> routinesFacadeGenerator) {
    super(routinesFacadeGenerator);
  }

  private static class Call extends AbstractRoutine.Update {
    public Call(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public void updateModels(final InsertEReference insertChange, final tools.vitruv.methodologisttemplate.model.model.System affectedEObject, final EReference affectedFeature, final Protocol newValue, final int index, @Extension final Model2Model2RoutinesFacade _routinesFacade) {
      _routinesFacade.createAndInsertProtocol(affectedEObject, newValue);
    }
  }

  public boolean isCurrentChangeMatchingTrigger(final EChange change) {
    if (!(change instanceof InsertEReference<?>)) {
    	return false;
    }
    
    InsertEReference<org.eclipse.emf.ecore.EObject> _localTypedChange = (InsertEReference<org.eclipse.emf.ecore.EObject>) change;
    if (!(_localTypedChange.getAffectedElement() instanceof tools.vitruv.methodologisttemplate.model.model.System)) {
    	return false;
    }
    if (!_localTypedChange.getAffectedFeature().getName().equals("protocols")) {
    	return false;
    }
    if (!(_localTypedChange.getNewValue() instanceof tools.vitruv.methodologisttemplate.model.model.Protocol)) {
    	return false;
    }
    this.insertChange = (InsertEReference<org.eclipse.emf.ecore.EObject>) change;
    return true;
  }

  public void executeReaction(final EChange change, final ReactionExecutionState executionState, final RoutinesFacade routinesFacadeUntyped) {
    mir.routines.model2Model2.Model2Model2RoutinesFacade routinesFacade = (mir.routines.model2Model2.Model2Model2RoutinesFacade)routinesFacadeUntyped;
    if (!isCurrentChangeMatchingTrigger(change)) {
    	return;
    }
    tools.vitruv.methodologisttemplate.model.model.System affectedEObject = (tools.vitruv.methodologisttemplate.model.model.System)insertChange.getAffectedElement();
    EReference affectedFeature = insertChange.getAffectedFeature();
    tools.vitruv.methodologisttemplate.model.model.Protocol newValue = (tools.vitruv.methodologisttemplate.model.model.Protocol)insertChange.getNewValue();
    int index = insertChange.getIndex();
    if (getLogger().isTraceEnabled()) {
    	getLogger().trace("Passed complete precondition check of Reaction " + this.getClass().getName());
    }
    
    new mir.reactions.model2Model2.ProtocolInsertedIntoSystemReaction.Call(executionState).updateModels(insertChange, affectedEObject, affectedFeature, newValue, index, routinesFacade);
  }
}
