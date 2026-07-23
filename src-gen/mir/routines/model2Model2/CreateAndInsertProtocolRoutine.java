package mir.routines.model2Model2;

import java.io.IOException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.lib.Extension;
import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutine;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.dsls.reactions.runtime.structure.CallHierarchyHaving;
import tools.vitruv.methodologisttemplate.model.model.Protocol;
import tools.vitruv.methodologisttemplate.model.model2.CommunicationStandard;
import tools.vitruv.methodologisttemplate.model.model2.Root;

@SuppressWarnings("all")
public class CreateAndInsertProtocolRoutine extends AbstractRoutine {
  private CreateAndInsertProtocolRoutine.InputValues inputValues;

  private CreateAndInsertProtocolRoutine.Match.RetrievedValues retrievedValues;

  private CreateAndInsertProtocolRoutine.Create.CreatedValues createdValues;

  public class InputValues {
    public final tools.vitruv.methodologisttemplate.model.model.System system;

    public final Protocol protocol;

    public InputValues(final tools.vitruv.methodologisttemplate.model.model.System system, final Protocol protocol) {
      this.system = system;
      this.protocol = protocol;
    }
  }

  private static class Match extends AbstractRoutine.Match {
    public class RetrievedValues {
      public final Root mRoot;

      public RetrievedValues(final Root mRoot) {
        this.mRoot = mRoot;
      }
    }

    public Match(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public EObject getCorrepondenceSource1(final tools.vitruv.methodologisttemplate.model.model.System system, final Protocol protocol) {
      return protocol;
    }

    public EObject getCorrepondenceSourceMRoot(final tools.vitruv.methodologisttemplate.model.model.System system, final Protocol protocol) {
      return system;
    }

    public CreateAndInsertProtocolRoutine.Match.RetrievedValues match(final tools.vitruv.methodologisttemplate.model.model.System system, final Protocol protocol) throws IOException {
      if (hasCorrespondingElements(
      	getCorrepondenceSource1(system, protocol), // correspondence source supplier
      	tools.vitruv.methodologisttemplate.model.model2.CommunicationStandard.class,
      	null, // correspondence precondition checker
      	null
      )) {
      	return null;
      }
      tools.vitruv.methodologisttemplate.model.model2.Root mRoot = getCorrespondingElement(
      	getCorrepondenceSourceMRoot(system, protocol), // correspondence source supplier
      	tools.vitruv.methodologisttemplate.model.model2.Root.class,
      	null, // correspondence precondition checker
      	null, 
      	false // asserted
      );
      if (mRoot == null) {
      	return null;
      }
      return new mir.routines.model2Model2.CreateAndInsertProtocolRoutine.Match.RetrievedValues(mRoot);
    }
  }

  private static class Create extends AbstractRoutine.Create {
    public class CreatedValues {
      public final CommunicationStandard mStandard;

      public CreatedValues(final CommunicationStandard mStandard) {
        this.mStandard = mStandard;
      }
    }

    public Create(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public CreateAndInsertProtocolRoutine.Create.CreatedValues createElements() {
      tools.vitruv.methodologisttemplate.model.model2.CommunicationStandard mStandard = createObject(() -> {
      	return org.eclipse.emf.ecore.impl.EFactoryImpl.eINSTANCE.createCommunicationStandard();
      });
      return new CreateAndInsertProtocolRoutine.Create.CreatedValues(mStandard);
    }
  }

  private static class Update extends AbstractRoutine.Update {
    public Update(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public void updateModels(final tools.vitruv.methodologisttemplate.model.model.System system, final Protocol protocol, final Root mRoot, final CommunicationStandard mStandard, @Extension final Model2Model2RoutinesFacade _routinesFacade) {
      mStandard.setName(protocol.getName());
      this.addCorrespondenceBetween(protocol, mStandard);
    }
  }

  public CreateAndInsertProtocolRoutine(final Model2Model2RoutinesFacade routinesFacade, final ReactionExecutionState reactionExecutionState, final CallHierarchyHaving calledBy, final tools.vitruv.methodologisttemplate.model.model.System system, final Protocol protocol) {
    super(routinesFacade, reactionExecutionState, calledBy);
    this.inputValues = new CreateAndInsertProtocolRoutine.InputValues(system, protocol);
  }

  protected boolean executeRoutine() throws IOException {
    if (getLogger().isTraceEnabled()) {
    	getLogger().trace("Called routine CreateAndInsertProtocolRoutine with input:");
    	getLogger().trace("   inputValues.system: " + inputValues.system);
    	getLogger().trace("   inputValues.protocol: " + inputValues.protocol);
    }
    retrievedValues = new mir.routines.model2Model2.CreateAndInsertProtocolRoutine.Match(getExecutionState()).match(inputValues.system, inputValues.protocol);
    if (retrievedValues == null) {
    	return false;
    }
    createdValues = new mir.routines.model2Model2.CreateAndInsertProtocolRoutine.Create(getExecutionState()).createElements();
    new mir.routines.model2Model2.CreateAndInsertProtocolRoutine.Update(getExecutionState()).updateModels(inputValues.system, inputValues.protocol, retrievedValues.mRoot, createdValues.mStandard, getRoutinesFacade());
    return true;
  }
}
