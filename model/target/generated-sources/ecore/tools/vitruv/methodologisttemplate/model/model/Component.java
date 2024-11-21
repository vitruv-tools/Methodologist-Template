/**
 */
package tools.vitruv.methodologisttemplate.model.model;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Component</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link tools.vitruv.methodologisttemplate.model.model.Component#getName <em>Name</em>}</li>
 *   <li>{@link tools.vitruv.methodologisttemplate.model.model.Component#getSupportedProtocols <em>Supported Protocols</em>}</li>
 * </ul>
 *
 * @see tools.vitruv.methodologisttemplate.model.model.ModelPackage#getComponent()
 * @model
 * @generated
 */
public interface Component extends EObject
{
	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see tools.vitruv.methodologisttemplate.model.model.ModelPackage#getComponent_Name()
	 * @model required="true"
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link tools.vitruv.methodologisttemplate.model.model.Component#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Supported Protocols</b></em>' reference list.
	 * The list contents are of type {@link tools.vitruv.methodologisttemplate.model.model.Protocol}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Supported Protocols</em>' reference list.
	 * @see tools.vitruv.methodologisttemplate.model.model.ModelPackage#getComponent_SupportedProtocols()
	 * @model required="true"
	 * @generated
	 */
	EList<Protocol> getSupportedProtocols();

} // Component
