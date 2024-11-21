/**
 */
package tools.vitruv.methodologisttemplate.model.model;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>System</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link tools.vitruv.methodologisttemplate.model.model.System#getLinks <em>Links</em>}</li>
 *   <li>{@link tools.vitruv.methodologisttemplate.model.model.System#getComponents <em>Components</em>}</li>
 *   <li>{@link tools.vitruv.methodologisttemplate.model.model.System#getProtocols <em>Protocols</em>}</li>
 * </ul>
 *
 * @see tools.vitruv.methodologisttemplate.model.model.ModelPackage#getSystem()
 * @model
 * @generated
 */
public interface System extends EObject
{
	/**
	 * Returns the value of the '<em><b>Links</b></em>' containment reference list.
	 * The list contents are of type {@link tools.vitruv.methodologisttemplate.model.model.Link}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Links</em>' containment reference list.
	 * @see tools.vitruv.methodologisttemplate.model.model.ModelPackage#getSystem_Links()
	 * @model containment="true"
	 * @generated
	 */
	EList<Link> getLinks();

	/**
	 * Returns the value of the '<em><b>Components</b></em>' containment reference list.
	 * The list contents are of type {@link tools.vitruv.methodologisttemplate.model.model.Component}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Components</em>' containment reference list.
	 * @see tools.vitruv.methodologisttemplate.model.model.ModelPackage#getSystem_Components()
	 * @model containment="true"
	 * @generated
	 */
	EList<Component> getComponents();

	/**
	 * Returns the value of the '<em><b>Protocols</b></em>' containment reference list.
	 * The list contents are of type {@link tools.vitruv.methodologisttemplate.model.model.Protocol}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Protocols</em>' containment reference list.
	 * @see tools.vitruv.methodologisttemplate.model.model.ModelPackage#getSystem_Protocols()
	 * @model containment="true"
	 * @generated
	 */
	EList<Protocol> getProtocols();

} // System
