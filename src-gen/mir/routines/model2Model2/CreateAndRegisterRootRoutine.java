package mir.routines.model2Model2;

import java.io.File;
import java.io.IOException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.lib.Extension;
import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutine;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.dsls.reactions.runtime.structure.CallHierarchyHaving;
import tools.vitruv.methodologisttemplate.model.model2.Root;

@SuppressWarnings("all")
public class CreateAndRegisterRootRoutine extends AbstractRoutine {
  private CreateAndRegisterRootRoutine.InputValues inputValues;

  private CreateAndRegisterRootRoutine.Match.RetrievedValues retrievedValues;

  private CreateAndRegisterRootRoutine.Create.CreatedValues createdValues;

  public class InputValues {
    public final tools.vitruv.methodologisttemplate.model.model.System system;

    public InputValues(final tools.vitruv.methodologisttemplate.model.model.System system) {
      this.system = system;
    }
  }

  private static class Match extends AbstractRoutine.Match {
    public class RetrievedValues {
      public RetrievedValues() {
        
      }
    }

    public Match(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public EObject getCorrepondenceSource1(final tools.vitruv.methodologisttemplate.model.model.System system) {
      return system;
    }

    public CreateAndRegisterRootRoutine.Match.RetrievedValues match(final tools.vitruv.methodologisttemplate.model.model.System system) throws IOException {
      if (hasCorrespondingElements(
      	getCorrepondenceSource1(system), // correspondence source supplier
      	tools.vitruv.methodologisttemplate.model.model2.Root.class,
      	null, // correspondence precondition checker
      	null
      )) {
      	return null;
      }
      return new mir.routines.model2Model2.CreateAndRegisterRootRoutine.Match.RetrievedValues();
    }
  }

  private static class Create extends AbstractRoutine.Create {
    public class CreatedValues {
      public final Root mRoot;

      public CreatedValues(final Root mRoot) {
        this.mRoot = mRoot;
      }
    }

    public Create(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public CreateAndRegisterRootRoutine.Create.CreatedValues createElements() {
      tools.vitruv.methodologisttemplate.model.model2.Root mRoot = createObject(() -> {
      	return org.eclipse.emf.ecore.impl.EFactoryImpl.eINSTANCE.createRoot();
      });
      return new CreateAndRegisterRootRoutine.Create.CreatedValues(mRoot);
    }
  }

  private static class Update extends AbstractRoutine.Update {
    public Update(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public void updateModels(final tools.vitruv.methodologisttemplate.model.model.System system, final Root mRoot, @Extension final Model2Model2RoutinesFacade _routinesFacade) {
      String _string = new File("").toString();
      String _plus = (_string + "example.model2");
      this.persistProjectRelative(system, mRoot, _plus);
      this.addCorrespondenceBetween(system, mRoot);
    }
  }

  public CreateAndRegisterRootRoutine(final Model2Model2RoutinesFacade routinesFacade, final ReactionExecutionState reactionExecutionState, final CallHierarchyHaving calledBy, final tools.vitruv.methodologisttemplate.model.model.System system) {
    super(routinesFacade, reactionExecutionState, calledBy);
    this.inputValues = new CreateAndRegisterRootRoutine.InputValues(system);
  }

  protected boolean executeRoutine() throws IOException {
    if (getLogger().isTraceEnabled()) {
    	getLogger().trace("Called routine CreateAndRegisterRootRoutine with input:");
    	getLogger().trace("   inputValues.system: " + inputValues.system);
    }
    retrievedValues = new mir.routines.model2Model2.CreateAndRegisterRootRoutine.Match(getExecutionState()).match(inputValues.system);
    if (retrievedValues == null) {
    	return false;
    }
    createdValues = new mir.routines.model2Model2.CreateAndRegisterRootRoutine.Create(getExecutionState()).createElements();
    new mir.routines.model2Model2.CreateAndRegisterRootRoutine.Update(getExecutionState()).updateModels(inputValues.system, createdValues.mRoot, getRoutinesFacade());
    return true;
  }
}
