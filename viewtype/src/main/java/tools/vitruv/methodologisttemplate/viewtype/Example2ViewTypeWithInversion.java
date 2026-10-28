package tools.vitruv.methodologisttemplate.viewtype;

import neojoin.viewtypes.example2.Example2ViewType;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.FeatureOriginBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;
import tools.vitruv.compmodelcons.views.bindings.ValueUpdateBinding;
import tools.vitruv.framework.views.ViewTypeProvider;
import tools.vitruv.methodologisttemplate.model.model.Component;

public class Example2ViewTypeWithInversion extends Example2ViewType {
  public Example2ViewTypeWithInversion(ViewTypeProvider viewTypeProvider) {
    super(viewTypeProvider);
  }

  @Override
  protected FeatureOriginBinding doPut_Thing_feature_0(EChange<EObject> viewChange, FeatureOriginBinding feature, ObjectBinding subjectBinding, ValueUpdateBinding value, PutContext context) {
    Component component = (Component) subjectBinding.originObjects().getFirst();
    if (value instanceof ValueUpdateBinding.Replace(String newValue)) {
      component.setName(newValue.substring(0, newValue.length() - "Suffix".length()));
      return doGet_Thing_feature_0(subjectBinding, context);
    } else if (value instanceof ValueUpdateBinding.Unset ignore) {
      component.setName(null);
      return doGet_Thing_feature_0(subjectBinding, context);
    }
    return null;
  }
}
