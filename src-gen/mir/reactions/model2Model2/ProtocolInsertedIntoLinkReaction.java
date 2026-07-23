package mir.reactions.model2Model2;

import java.util.function.Function;
import mir.routines.model2Model2.Model2Model2RoutinesFacade;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.xbase.lib.Extension;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.feature.reference.ReplaceSingleValuedEReference;
import tools.vitruv.dsls.reactions.runtime.reactions.AbstractReaction;
import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutine;
import tools.vitruv.dsls.reactions.runtime.routines.RoutinesFacade;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.methodologisttemplate.model.model.Link;
import tools.vitruv.methodologisttemplate.model.model.Protocol;

@SuppressWarnings("all")
public class ProtocolInsertedIntoLinkReaction extends AbstractReaction {
  private ReplaceSingleValuedEReference<EObject> replaceChange;

  public ProtocolInsertedIntoLinkReaction(final Function<ReactionExecutionState, RoutinesFacade> routinesFacadeGenerator) {
    super(routinesFacadeGenerator);
  }

  private static class Call extends AbstractRoutine.Update {
    public Call(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public void updateModels(final ReplaceSingleValuedEReference replaceChange, final Link affectedEObject, final EReference affectedFeature, final Protocol oldValue, final Protocol newValue, @Extension final Model2Model2RoutinesFacade _routinesFacade) {
      _routinesFacade.updateLinkProtocols(affectedEObject, newValue);
    }
  }

  public boolean isCurrentChangeMatchingTrigger(final EChange change) {
    if (!(change instanceof ReplaceSingleValuedEReference<?>)) {
    	return false;
    }
    
    ReplaceSingleValuedEReference<org.eclipse.emf.ecore.EObject> _localTypedChange = (ReplaceSingleValuedEReference<org.eclipse.emf.ecore.EObject>) change;
    if (!(_localTypedChange.getAffectedElement() instanceof tools.vitruv.methodologisttemplate.model.model.Link)) {
    	return false;
    }
    if (!_localTypedChange.getAffectedFeature().getName().equals("protocol")) {
    	return false;
    }
    if (_localTypedChange.isFromNonDefaultValue() && !(_localTypedChange.getOldValue() instanceof tools.vitruv.methodologisttemplate.model.model.Protocol)) {
    	return false;
    }
    if (_localTypedChange.isToNonDefaultValue() && !(_localTypedChange.getNewValue() instanceof tools.vitruv.methodologisttemplate.model.model.Protocol)) {
    	return false;
    }
    this.replaceChange = (ReplaceSingleValuedEReference<org.eclipse.emf.ecore.EObject>) change;
    return true;
  }

  public void executeReaction(final EChange change, final ReactionExecutionState executionState, final RoutinesFacade routinesFacadeUntyped) {
    mir.routines.model2Model2.Model2Model2RoutinesFacade routinesFacade = (mir.routines.model2Model2.Model2Model2RoutinesFacade)routinesFacadeUntyped;
    if (!isCurrentChangeMatchingTrigger(change)) {
    	return;
    }
    tools.vitruv.methodologisttemplate.model.model.Link affectedEObject = (tools.vitruv.methodologisttemplate.model.model.Link)replaceChange.getAffectedElement();
    EReference affectedFeature = replaceChange.getAffectedFeature();
    tools.vitruv.methodologisttemplate.model.model.Protocol oldValue = (tools.vitruv.methodologisttemplate.model.model.Protocol)replaceChange.getOldValue();
    tools.vitruv.methodologisttemplate.model.model.Protocol newValue = (tools.vitruv.methodologisttemplate.model.model.Protocol)replaceChange.getNewValue();
    if (getLogger().isTraceEnabled()) {
    	getLogger().trace("Passed complete precondition check of Reaction " + this.getClass().getName());
    }
    
    new mir.reactions.model2Model2.ProtocolInsertedIntoLinkReaction.Call(executionState).updateModels(replaceChange, affectedEObject, affectedFeature, oldValue, newValue, routinesFacade);
  }
}
