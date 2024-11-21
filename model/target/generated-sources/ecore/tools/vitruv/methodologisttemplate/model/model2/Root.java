/**
 */
package tools.vitruv.methodologisttemplate.model.model2;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Root</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link tools.vitruv.methodologisttemplate.model.model2.Root#getEntities <em>Entities</em>}</li>
 * </ul>
 *
 * @see tools.vitruv.methodologisttemplate.model.model2.Model2Package#getRoot()
 * @model
 * @generated
 */
public interface Root extends EObject
{
	/**
	 * Returns the value of the '<em><b>Entities</b></em>' containment reference list.
	 * The list contents are of type {@link tools.vitruv.methodologisttemplate.model.model2.Entity}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Entities</em>' containment reference list.
	 * @see tools.vitruv.methodologisttemplate.model.model2.Model2Package#getRoot_Entities()
	 * @model containment="true"
	 * @generated
	 */
	EList<Entity> getEntities();

} // Root
