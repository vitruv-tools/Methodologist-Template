package mir.routines.model2Model2;

import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutinesFacade;
import tools.vitruv.dsls.reactions.runtime.routines.RoutinesFacadesProvider;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.dsls.reactions.runtime.structure.CallHierarchyHaving;
import tools.vitruv.dsls.reactions.runtime.structure.ReactionsImportPath;
import tools.vitruv.methodologisttemplate.model.model.Component;
import tools.vitruv.methodologisttemplate.model.model.Link;
import tools.vitruv.methodologisttemplate.model.model.Protocol;

@SuppressWarnings("all")
public class Model2Model2RoutinesFacade extends AbstractRoutinesFacade {
  public Model2Model2RoutinesFacade(final RoutinesFacadesProvider routinesFacadesProvider, final ReactionsImportPath reactionsImportPath) {
    super(routinesFacadesProvider, reactionsImportPath);
  }

  public boolean createAndRegisterRoot(final tools.vitruv.methodologisttemplate.model.model.System system) {
    Model2Model2RoutinesFacade _routinesFacade = this;
    ReactionExecutionState _executionState = getExecutionState();
    CallHierarchyHaving _caller = this.getCurrentCaller();
    CreateAndRegisterRootRoutine routine = new CreateAndRegisterRootRoutine(_routinesFacade, _executionState, _caller, system);
    return routine.execute();
  }

  public boolean createAndInsertEntity(final tools.vitruv.methodologisttemplate.model.model.System system, final Component component) {
    Model2Model2RoutinesFacade _routinesFacade = this;
    ReactionExecutionState _executionState = getExecutionState();
    CallHierarchyHaving _caller = this.getCurrentCaller();
    CreateAndInsertEntityRoutine routine = new CreateAndInsertEntityRoutine(_routinesFacade, _executionState, _caller, system, component);
    return routine.execute();
  }

  public boolean renameEntity(final Component component) {
    Model2Model2RoutinesFacade _routinesFacade = this;
    ReactionExecutionState _executionState = getExecutionState();
    CallHierarchyHaving _caller = this.getCurrentCaller();
    RenameEntityRoutine routine = new RenameEntityRoutine(_routinesFacade, _executionState, _caller, component);
    return routine.execute();
  }

  public boolean deleteEntity(final Component component) {
    Model2Model2RoutinesFacade _routinesFacade = this;
    ReactionExecutionState _executionState = getExecutionState();
    CallHierarchyHaving _caller = this.getCurrentCaller();
    DeleteEntityRoutine routine = new DeleteEntityRoutine(_routinesFacade, _executionState, _caller, component);
    return routine.execute();
  }

  public boolean createAndInsertProtocol(final tools.vitruv.methodologisttemplate.model.model.System system, final Protocol protocol) {
    Model2Model2RoutinesFacade _routinesFacade = this;
    ReactionExecutionState _executionState = getExecutionState();
    CallHierarchyHaving _caller = this.getCurrentCaller();
    CreateAndInsertProtocolRoutine routine = new CreateAndInsertProtocolRoutine(_routinesFacade, _executionState, _caller, system, protocol);
    return routine.execute();
  }

  public boolean createAndInsertLink(final tools.vitruv.methodologisttemplate.model.model.System system, final Link link) {
    Model2Model2RoutinesFacade _routinesFacade = this;
    ReactionExecutionState _executionState = getExecutionState();
    CallHierarchyHaving _caller = this.getCurrentCaller();
    CreateAndInsertLinkRoutine routine = new CreateAndInsertLinkRoutine(_routinesFacade, _executionState, _caller, system, link);
    return routine.execute();
  }

  public boolean updateLinkEntities(final Link link, final Component component) {
    Model2Model2RoutinesFacade _routinesFacade = this;
    ReactionExecutionState _executionState = getExecutionState();
    CallHierarchyHaving _caller = this.getCurrentCaller();
    UpdateLinkEntitiesRoutine routine = new UpdateLinkEntitiesRoutine(_routinesFacade, _executionState, _caller, link, component);
    return routine.execute();
  }

  public boolean updateLinkProtocols(final Link link, final Protocol protocol) {
    Model2Model2RoutinesFacade _routinesFacade = this;
    ReactionExecutionState _executionState = getExecutionState();
    CallHierarchyHaving _caller = this.getCurrentCaller();
    UpdateLinkProtocolsRoutine routine = new UpdateLinkProtocolsRoutine(_routinesFacade, _executionState, _caller, link, protocol);
    return routine.execute();
  }
}
