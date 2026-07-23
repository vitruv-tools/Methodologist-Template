package mir.routines.model2Model2;

import java.io.IOException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.lib.Extension;
import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutine;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.dsls.reactions.runtime.structure.CallHierarchyHaving;
import tools.vitruv.methodologisttemplate.model.model.Link;
import tools.vitruv.methodologisttemplate.model.model.Protocol;
import tools.vitruv.methodologisttemplate.model.model2.CommunicationStandard;

@SuppressWarnings("all")
public class UpdateLinkProtocolsRoutine extends AbstractRoutine {
  private UpdateLinkProtocolsRoutine.InputValues inputValues;

  private UpdateLinkProtocolsRoutine.Match.RetrievedValues retrievedValues;

  public class InputValues {
    public final Link link;

    public final Protocol protocol;

    public InputValues(final Link link, final Protocol protocol) {
      this.link = link;
      this.protocol = protocol;
    }
  }

  private static class Match extends AbstractRoutine.Match {
    public class RetrievedValues {
      public final CommunicationStandard mStandard;

      public final tools.vitruv.methodologisttemplate.model.model2.Link mLink;

      public RetrievedValues(final CommunicationStandard mStandard, final tools.vitruv.methodologisttemplate.model.model2.Link mLink) {
        this.mStandard = mStandard;
        this.mLink = mLink;
      }
    }

    public Match(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public EObject getCorrepondenceSourceMStandard(final Link link, final Protocol protocol) {
      return protocol;
    }

    public EObject getCorrepondenceSourceMLink(final Link link, final Protocol protocol, final CommunicationStandard mStandard) {
      return link;
    }

    public UpdateLinkProtocolsRoutine.Match.RetrievedValues match(final Link link, final Protocol protocol) throws IOException {
      tools.vitruv.methodologisttemplate.model.model2.CommunicationStandard mStandard = getCorrespondingElement(
      	getCorrepondenceSourceMStandard(link, protocol), // correspondence source supplier
      	tools.vitruv.methodologisttemplate.model.model2.CommunicationStandard.class,
      	null, // correspondence precondition checker
      	null, 
      	false // asserted
      );
      if (mStandard == null) {
      	return null;
      }
      tools.vitruv.methodologisttemplate.model.model2.Link mLink = getCorrespondingElement(
      	getCorrepondenceSourceMLink(link, protocol, mStandard), // correspondence source supplier
      	tools.vitruv.methodologisttemplate.model.model2.Link.class,
      	null, // correspondence precondition checker
      	null, 
      	false // asserted
      );
      if (mLink == null) {
      	return null;
      }
      return new mir.routines.model2Model2.UpdateLinkProtocolsRoutine.Match.RetrievedValues(mStandard, mLink);
    }
  }

  private static class Update extends AbstractRoutine.Update {
    public Update(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public void updateModels(final Link link, final Protocol protocol, final CommunicationStandard mStandard, final tools.vitruv.methodologisttemplate.model.model2.Link mLink, @Extension final Model2Model2RoutinesFacade _routinesFacade) {
      mLink.setStandard(mStandard);
    }
  }

  public UpdateLinkProtocolsRoutine(final Model2Model2RoutinesFacade routinesFacade, final ReactionExecutionState reactionExecutionState, final CallHierarchyHaving calledBy, final Link link, final Protocol protocol) {
    super(routinesFacade, reactionExecutionState, calledBy);
    this.inputValues = new UpdateLinkProtocolsRoutine.InputValues(link, protocol);
  }

  protected boolean executeRoutine() throws IOException {
    if (getLogger().isTraceEnabled()) {
    	getLogger().trace("Called routine UpdateLinkProtocolsRoutine with input:");
    	getLogger().trace("   inputValues.link: " + inputValues.link);
    	getLogger().trace("   inputValues.protocol: " + inputValues.protocol);
    }
    retrievedValues = new mir.routines.model2Model2.UpdateLinkProtocolsRoutine.Match(getExecutionState()).match(inputValues.link, inputValues.protocol);
    if (retrievedValues == null) {
    	return false;
    }
    // This execution step is empty
    new mir.routines.model2Model2.UpdateLinkProtocolsRoutine.Update(getExecutionState()).updateModels(inputValues.link, inputValues.protocol, retrievedValues.mStandard, retrievedValues.mLink, getRoutinesFacade());
    return true;
  }
}
