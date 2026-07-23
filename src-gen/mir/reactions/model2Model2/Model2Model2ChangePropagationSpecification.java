package mir.reactions.model2Model2;

import java.util.Set;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.dsls.reactions.runtime.reactions.AbstractReactionsChangePropagationSpecification;
import tools.vitruv.dsls.reactions.runtime.routines.RoutinesFacadesProvider;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.dsls.reactions.runtime.structure.ReactionsImportPath;

@SuppressWarnings("all")
public class Model2Model2ChangePropagationSpecification extends AbstractReactionsChangePropagationSpecification implements ChangePropagationSpecification {
  public Model2Model2ChangePropagationSpecification() {
    super(MetamodelDescriptor.with(Set.of("http://vitruv.tools/methodologisttemplate/model")), 
    	MetamodelDescriptor.with(Set.of("http://vitruv.tools/methodologisttemplate/model2")));
  }

  protected RoutinesFacadesProvider createRoutinesFacadesProvider(final ReactionExecutionState executionState) {
    return new mir.routines.model2Model2.Model2Model2RoutinesFacadesProvider(executionState);
  }

  protected void setup() {
    addReaction(new mir.reactions.model2Model2.SystemInsertedAsRootReaction((executionState) -> createRoutinesFacadesProvider(executionState).getRoutinesFacade(ReactionsImportPath.fromPathString("model2Model2"))));
    addReaction(new mir.reactions.model2Model2.ComponentInsertedIntoSystemReaction((executionState) -> createRoutinesFacadesProvider(executionState).getRoutinesFacade(ReactionsImportPath.fromPathString("model2Model2"))));
    addReaction(new mir.reactions.model2Model2.ComponentRenamedReaction((executionState) -> createRoutinesFacadesProvider(executionState).getRoutinesFacade(ReactionsImportPath.fromPathString("model2Model2"))));
    addReaction(new mir.reactions.model2Model2.ComponentDeletedReaction((executionState) -> createRoutinesFacadesProvider(executionState).getRoutinesFacade(ReactionsImportPath.fromPathString("model2Model2"))));
    addReaction(new mir.reactions.model2Model2.ProtocolInsertedIntoSystemReaction((executionState) -> createRoutinesFacadesProvider(executionState).getRoutinesFacade(ReactionsImportPath.fromPathString("model2Model2"))));
    addReaction(new mir.reactions.model2Model2.LinkInsertedIntoSystemReaction((executionState) -> createRoutinesFacadesProvider(executionState).getRoutinesFacade(ReactionsImportPath.fromPathString("model2Model2"))));
    addReaction(new mir.reactions.model2Model2.ComponentInsertedIntoLinkReaction((executionState) -> createRoutinesFacadesProvider(executionState).getRoutinesFacade(ReactionsImportPath.fromPathString("model2Model2"))));
    addReaction(new mir.reactions.model2Model2.ProtocolInsertedIntoLinkReaction((executionState) -> createRoutinesFacadesProvider(executionState).getRoutinesFacade(ReactionsImportPath.fromPathString("model2Model2"))));
  }
}
