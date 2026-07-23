package mir.routines.model2Model2;

import java.io.IOException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.lib.Extension;
import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutine;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.dsls.reactions.runtime.structure.CallHierarchyHaving;
import tools.vitruv.methodologisttemplate.model.model.Component;
import tools.vitruv.methodologisttemplate.model.model.Link;
import tools.vitruv.methodologisttemplate.model.model2.Entity;

@SuppressWarnings("all")
public class UpdateLinkEntitiesRoutine extends AbstractRoutine {
  private UpdateLinkEntitiesRoutine.InputValues inputValues;

  private UpdateLinkEntitiesRoutine.Match.RetrievedValues retrievedValues;

  public class InputValues {
    public final Link link;

    public final Component component;

    public InputValues(final Link link, final Component component) {
      this.link = link;
      this.component = component;
    }
  }

  private static class Match extends AbstractRoutine.Match {
    public class RetrievedValues {
      public final Entity mEntity;

      public final tools.vitruv.methodologisttemplate.model.model2.Link mLink;

      public RetrievedValues(final Entity mEntity, final tools.vitruv.methodologisttemplate.model.model2.Link mLink) {
        this.mEntity = mEntity;
        this.mLink = mLink;
      }
    }

    public Match(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public EObject getCorrepondenceSourceMEntity(final Link link, final Component component) {
      return component;
    }

    public EObject getCorrepondenceSourceMLink(final Link link, final Component component, final Entity mEntity) {
      return link;
    }

    public UpdateLinkEntitiesRoutine.Match.RetrievedValues match(final Link link, final Component component) throws IOException {
      tools.vitruv.methodologisttemplate.model.model2.Entity mEntity = getCorrespondingElement(
      	getCorrepondenceSourceMEntity(link, component), // correspondence source supplier
      	tools.vitruv.methodologisttemplate.model.model2.Entity.class,
      	null, // correspondence precondition checker
      	null, 
      	false // asserted
      );
      if (mEntity == null) {
      	return null;
      }
      tools.vitruv.methodologisttemplate.model.model2.Link mLink = getCorrespondingElement(
      	getCorrepondenceSourceMLink(link, component, mEntity), // correspondence source supplier
      	tools.vitruv.methodologisttemplate.model.model2.Link.class,
      	null, // correspondence precondition checker
      	null, 
      	false // asserted
      );
      if (mLink == null) {
      	return null;
      }
      return new mir.routines.model2Model2.UpdateLinkEntitiesRoutine.Match.RetrievedValues(mEntity, mLink);
    }
  }

  private static class Update extends AbstractRoutine.Update {
    public Update(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public void updateModels(final Link link, final Component component, final Entity mEntity, final tools.vitruv.methodologisttemplate.model.model2.Link mLink, @Extension final Model2Model2RoutinesFacade _routinesFacade) {
      mLink.getEntities().add(mEntity);
    }
  }

  public UpdateLinkEntitiesRoutine(final Model2Model2RoutinesFacade routinesFacade, final ReactionExecutionState reactionExecutionState, final CallHierarchyHaving calledBy, final Link link, final Component component) {
    super(routinesFacade, reactionExecutionState, calledBy);
    this.inputValues = new UpdateLinkEntitiesRoutine.InputValues(link, component);
  }

  protected boolean executeRoutine() throws IOException {
    if (getLogger().isTraceEnabled()) {
    	getLogger().trace("Called routine UpdateLinkEntitiesRoutine with input:");
    	getLogger().trace("   inputValues.link: " + inputValues.link);
    	getLogger().trace("   inputValues.component: " + inputValues.component);
    }
    retrievedValues = new mir.routines.model2Model2.UpdateLinkEntitiesRoutine.Match(getExecutionState()).match(inputValues.link, inputValues.component);
    if (retrievedValues == null) {
    	return false;
    }
    // This execution step is empty
    new mir.routines.model2Model2.UpdateLinkEntitiesRoutine.Update(getExecutionState()).updateModels(inputValues.link, inputValues.component, retrievedValues.mEntity, retrievedValues.mLink, getRoutinesFacade());
    return true;
  }
}
