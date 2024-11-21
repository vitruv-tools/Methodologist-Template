/**
 */
package tools.vitruv.methodologisttemplate.model.model2;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Link</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link tools.vitruv.methodologisttemplate.model.model2.Link#getEntitys <em>Entitys</em>}</li>
 * </ul>
 *
 * @see tools.vitruv.methodologisttemplate.model.model2.Model2Package#getLink()
 * @model
 * @generated
 */
public interface Link extends EObject
{
	/**
	 * Returns the value of the '<em><b>Entitys</b></em>' reference list.
	 * The list contents are of type {@link tools.vitruv.methodologisttemplate.model.model2.Entity}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Entitys</em>' reference list.
	 * @see tools.vitruv.methodologisttemplate.model.model2.Model2Package#getLink_Entitys()
	 * @model lower="2"
	 * @generated
	 */
	EList<Entity> getEntitys();

} // Link
