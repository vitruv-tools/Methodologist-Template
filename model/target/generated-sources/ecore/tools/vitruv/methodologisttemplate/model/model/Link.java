/**
 */
package tools.vitruv.methodologisttemplate.model.model;

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
 *   <li>{@link tools.vitruv.methodologisttemplate.model.model.Link#getComponents <em>Components</em>}</li>
 *   <li>{@link tools.vitruv.methodologisttemplate.model.model.Link#getProtocol <em>Protocol</em>}</li>
 * </ul>
 *
 * @see tools.vitruv.methodologisttemplate.model.model.ModelPackage#getLink()
 * @model
 * @generated
 */
public interface Link extends EObject
{
	/**
	 * Returns the value of the '<em><b>Components</b></em>' reference list.
	 * The list contents are of type {@link tools.vitruv.methodologisttemplate.model.model.Component}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Components</em>' reference list.
	 * @see tools.vitruv.methodologisttemplate.model.model.ModelPackage#getLink_Components()
	 * @model lower="2"
	 * @generated
	 */
	EList<Component> getComponents();

	/**
	 * Returns the value of the '<em><b>Protocol</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Protocol</em>' reference.
	 * @see #setProtocol(Protocol)
	 * @see tools.vitruv.methodologisttemplate.model.model.ModelPackage#getLink_Protocol()
	 * @model required="true"
	 * @generated
	 */
	Protocol getProtocol();

	/**
	 * Sets the value of the '{@link tools.vitruv.methodologisttemplate.model.model.Link#getProtocol <em>Protocol</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Protocol</em>' reference.
	 * @see #getProtocol()
	 * @generated
	 */
	void setProtocol(Protocol value);

} // Link
