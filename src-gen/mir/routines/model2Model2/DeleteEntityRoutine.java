package mir.routines.model2Model2;

import java.io.IOException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.lib.Extension;
import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutine;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.dsls.reactions.runtime.structure.CallHierarchyHaving;
import tools.vitruv.methodologisttemplate.model.model.Component;
import tools.vitruv.methodologisttemplate.model.model2.Entity;

@SuppressWarnings("all")
public class DeleteEntityRoutine extends AbstractRoutine {
  private DeleteEntityRoutine.InputValues inputValues;

  private DeleteEntityRoutine.Match.RetrievedValues retrievedValues;

  public class InputValues {
    public final Component component;

    public InputValues(final Component component) {
      this.component = component;
    }
  }

  private static class Match extends AbstractRoutine.Match {
    public class RetrievedValues {
      public final Entity entity;

      public RetrievedValues(final Entity entity) {
        this.entity = entity;
      }
    }

    public Match(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public EObject getCorrepondenceSourceEntity(final Component component) {
      return component;
    }

    public DeleteEntityRoutine.Match.RetrievedValues match(final Component component) throws IOException {
      tools.vitruv.methodologisttemplate.model.model2.Entity entity = getCorrespondingElement(
      	getCorrepondenceSourceEntity(component), // correspondence source supplier
      	tools.vitruv.methodologisttemplate.model.model2.Entity.class,
      	null, // correspondence precondition checker
      	null, 
      	false // asserted
      );
      if (entity == null) {
      	return null;
      }
      return new mir.routines.model2Model2.DeleteEntityRoutine.Match.RetrievedValues(entity);
    }
  }

  private static class Update extends AbstractRoutine.Update {
    public Update(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public void updateModels(final Component component, final Entity entity, @Extension final Model2Model2RoutinesFacade _routinesFacade) {
      this.removeObject(entity);
      this.removeCorrespondenceBetween(entity, component);
    }
  }

  public DeleteEntityRoutine(final Model2Model2RoutinesFacade routinesFacade, final ReactionExecutionState reactionExecutionState, final CallHierarchyHaving calledBy, final Component component) {
    super(routinesFacade, reactionExecutionState, calledBy);
    this.inputValues = new DeleteEntityRoutine.InputValues(component);
  }

  protected boolean executeRoutine() throws IOException {
    if (getLogger().isTraceEnabled()) {
    	getLogger().trace("Called routine DeleteEntityRoutine with input:");
    	getLogger().trace("   inputValues.component: " + inputValues.component);
    }
    retrievedValues = new mir.routines.model2Model2.DeleteEntityRoutine.Match(getExecutionState()).match(inputValues.component);
    if (retrievedValues == null) {
    	return false;
    }
    // This execution step is empty
    new mir.routines.model2Model2.DeleteEntityRoutine.Update(getExecutionState()).updateModels(inputValues.component, retrievedValues.entity, getRoutinesFacade());
    return true;
  }
}
