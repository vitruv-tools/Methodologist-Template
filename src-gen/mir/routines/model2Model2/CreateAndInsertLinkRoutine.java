package mir.routines.model2Model2;

import java.io.IOException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.lib.Extension;
import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutine;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.dsls.reactions.runtime.structure.CallHierarchyHaving;
import tools.vitruv.methodologisttemplate.model.model.Link;
import tools.vitruv.methodologisttemplate.model.model2.Root;

@SuppressWarnings("all")
public class CreateAndInsertLinkRoutine extends AbstractRoutine {
  private CreateAndInsertLinkRoutine.InputValues inputValues;

  private CreateAndInsertLinkRoutine.Match.RetrievedValues retrievedValues;

  private CreateAndInsertLinkRoutine.Create.CreatedValues createdValues;

  public class InputValues {
    public final tools.vitruv.methodologisttemplate.model.model.System system;

    public final Link link;

    public InputValues(final tools.vitruv.methodologisttemplate.model.model.System system, final Link link) {
      this.system = system;
      this.link = link;
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

    public EObject getCorrepondenceSource1(final tools.vitruv.methodologisttemplate.model.model.System system, final Link link) {
      return link;
    }

    public EObject getCorrepondenceSourceMRoot(final tools.vitruv.methodologisttemplate.model.model.System system, final Link link) {
      return system;
    }

    public CreateAndInsertLinkRoutine.Match.RetrievedValues match(final tools.vitruv.methodologisttemplate.model.model.System system, final Link link) throws IOException {
      if (hasCorrespondingElements(
      	getCorrepondenceSource1(system, link), // correspondence source supplier
      	tools.vitruv.methodologisttemplate.model.model2.Link.class,
      	null, // correspondence precondition checker
      	null
      )) {
      	return null;
      }
      tools.vitruv.methodologisttemplate.model.model2.Root mRoot = getCorrespondingElement(
      	getCorrepondenceSourceMRoot(system, link), // correspondence source supplier
      	tools.vitruv.methodologisttemplate.model.model2.Root.class,
      	null, // correspondence precondition checker
      	null, 
      	false // asserted
      );
      if (mRoot == null) {
      	return null;
      }
      return new mir.routines.model2Model2.CreateAndInsertLinkRoutine.Match.RetrievedValues(mRoot);
    }
  }

  private static class Create extends AbstractRoutine.Create {
    public class CreatedValues {
      public final tools.vitruv.methodologisttemplate.model.model2.Link mLink;

      public CreatedValues(final tools.vitruv.methodologisttemplate.model.model2.Link mLink) {
        this.mLink = mLink;
      }
    }

    public Create(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public CreateAndInsertLinkRoutine.Create.CreatedValues createElements() {
      tools.vitruv.methodologisttemplate.model.model2.Link mLink = createObject(() -> {
      	return org.eclipse.emf.ecore.impl.EFactoryImpl.eINSTANCE.createLink();
      });
      return new CreateAndInsertLinkRoutine.Create.CreatedValues(mLink);
    }
  }

  private static class Update extends AbstractRoutine.Update {
    public Update(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public void updateModels(final tools.vitruv.methodologisttemplate.model.model.System system, final Link link, final Root mRoot, final tools.vitruv.methodologisttemplate.model.model2.Link mLink, @Extension final Model2Model2RoutinesFacade _routinesFacade) {
      mRoot.getLinks().add(mLink);
      this.addCorrespondenceBetween(link, mLink);
    }
  }

  public CreateAndInsertLinkRoutine(final Model2Model2RoutinesFacade routinesFacade, final ReactionExecutionState reactionExecutionState, final CallHierarchyHaving calledBy, final tools.vitruv.methodologisttemplate.model.model.System system, final Link link) {
    super(routinesFacade, reactionExecutionState, calledBy);
    this.inputValues = new CreateAndInsertLinkRoutine.InputValues(system, link);
  }

  protected boolean executeRoutine() throws IOException {
    if (getLogger().isTraceEnabled()) {
    	getLogger().trace("Called routine CreateAndInsertLinkRoutine with input:");
    	getLogger().trace("   inputValues.system: " + inputValues.system);
    	getLogger().trace("   inputValues.link: " + inputValues.link);
    }
    retrievedValues = new mir.routines.model2Model2.CreateAndInsertLinkRoutine.Match(getExecutionState()).match(inputValues.system, inputValues.link);
    if (retrievedValues == null) {
    	return false;
    }
    createdValues = new mir.routines.model2Model2.CreateAndInsertLinkRoutine.Create(getExecutionState()).createElements();
    new mir.routines.model2Model2.CreateAndInsertLinkRoutine.Update(getExecutionState()).updateModels(inputValues.system, inputValues.link, retrievedValues.mRoot, createdValues.mLink, getRoutinesFacade());
    return true;
  }
}
