package mir.routines.model2Model2;

import java.io.IOException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.lib.Extension;
import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutine;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.dsls.reactions.runtime.structure.CallHierarchyHaving;
import tools.vitruv.methodologisttemplate.model.model.Component;
import tools.vitruv.methodologisttemplate.model.model2.Entity;
import tools.vitruv.methodologisttemplate.model.model2.Root;

@SuppressWarnings("all")
public class CreateAndInsertEntityRoutine extends AbstractRoutine {
  private CreateAndInsertEntityRoutine.InputValues inputValues;

  private CreateAndInsertEntityRoutine.Match.RetrievedValues retrievedValues;

  private CreateAndInsertEntityRoutine.Create.CreatedValues createdValues;

  public class InputValues {
    public final tools.vitruv.methodologisttemplate.model.model.System system;

    public final Component component;

    public InputValues(final tools.vitruv.methodologisttemplate.model.model.System system, final Component component) {
      this.system = system;
      this.component = component;
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

    public EObject getCorrepondenceSource1(final tools.vitruv.methodologisttemplate.model.model.System system, final Component component) {
      return component;
    }

    public EObject getCorrepondenceSourceMRoot(final tools.vitruv.methodologisttemplate.model.model.System system, final Component component) {
      return system;
    }

    public CreateAndInsertEntityRoutine.Match.RetrievedValues match(final tools.vitruv.methodologisttemplate.model.model.System system, final Component component) throws IOException {
      if (hasCorrespondingElements(
      	getCorrepondenceSource1(system, component), // correspondence source supplier
      	tools.vitruv.methodologisttemplate.model.model2.Entity.class,
      	null, // correspondence precondition checker
      	null
      )) {
      	return null;
      }
      tools.vitruv.methodologisttemplate.model.model2.Root mRoot = getCorrespondingElement(
      	getCorrepondenceSourceMRoot(system, component), // correspondence source supplier
      	tools.vitruv.methodologisttemplate.model.model2.Root.class,
      	null, // correspondence precondition checker
      	null, 
      	false // asserted
      );
      if (mRoot == null) {
      	return null;
      }
      return new mir.routines.model2Model2.CreateAndInsertEntityRoutine.Match.RetrievedValues(mRoot);
    }
  }

  private static class Create extends AbstractRoutine.Create {
    public class CreatedValues {
      public final Entity entity;

      public CreatedValues(final Entity entity) {
        this.entity = entity;
      }
    }

    public Create(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public CreateAndInsertEntityRoutine.Create.CreatedValues createElements() {
      tools.vitruv.methodologisttemplate.model.model2.Entity entity = createObject(() -> {
      	return org.eclipse.emf.ecore.impl.EFactoryImpl.eINSTANCE.createEntity();
      });
      return new CreateAndInsertEntityRoutine.Create.CreatedValues(entity);
    }
  }

  private static class Update extends AbstractRoutine.Update {
    public Update(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public void updateModels(final tools.vitruv.methodologisttemplate.model.model.System system, final Component component, final Root mRoot, final Entity entity, @Extension final Model2Model2RoutinesFacade _routinesFacade) {
      entity.setName(component.getName());
      mRoot.getEntities().add(entity);
      this.addCorrespondenceBetween(component, entity);
    }
  }

  public CreateAndInsertEntityRoutine(final Model2Model2RoutinesFacade routinesFacade, final ReactionExecutionState reactionExecutionState, final CallHierarchyHaving calledBy, final tools.vitruv.methodologisttemplate.model.model.System system, final Component component) {
    super(routinesFacade, reactionExecutionState, calledBy);
    this.inputValues = new CreateAndInsertEntityRoutine.InputValues(system, component);
  }

  protected boolean executeRoutine() throws IOException {
    if (getLogger().isTraceEnabled()) {
    	getLogger().trace("Called routine CreateAndInsertEntityRoutine with input:");
    	getLogger().trace("   inputValues.system: " + inputValues.system);
    	getLogger().trace("   inputValues.component: " + inputValues.component);
    }
    retrievedValues = new mir.routines.model2Model2.CreateAndInsertEntityRoutine.Match(getExecutionState()).match(inputValues.system, inputValues.component);
    if (retrievedValues == null) {
    	return false;
    }
    createdValues = new mir.routines.model2Model2.CreateAndInsertEntityRoutine.Create(getExecutionState()).createElements();
    new mir.routines.model2Model2.CreateAndInsertEntityRoutine.Update(getExecutionState()).updateModels(inputValues.system, inputValues.component, retrievedValues.mRoot, createdValues.entity, getRoutinesFacade());
    return true;
  }
}
