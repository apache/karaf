/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/BundleContext.java,v 1.22 2007/02/21 16:49:05 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2000, 2007). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.framework;

import java.io.File;
import java.io.InputStream;
import java.util.Dictionary;

/**
 * A bundle's execution context within the Framework. The context is used to
 * grant access to other methods so that this bundle can interact with the
 * Framework.
 * 
 * <p>
 * <code>BundleContext</code> methods allow a bundle to:
 * <ul>
 * <li>Subscribe to events published by the Framework.
 * <li>Register service objects with the Framework service registry.
 * <li>Retrieve <code>ServiceReferences</code> from the Framework service
 * registry.
 * <li>Get and release service objects for a referenced service.
 * <li>Install new bundles in the Framework.
 * <li>Get the list of bundles installed in the Framework.
 * <li>Get the {@link Bundle} object for a bundle.
 * <li>Create <code>File</code> objects for files in a persistent storage
 * area provided for the bundle by the Framework.
 * </ul>
 * 
 * <p>
 * A <code>BundleContext</code> object will be created and provided to the
 * bundle associated with this context when it is started using the
 * {@link BundleActivator#start} method. The same <code>BundleContext</code>
 * object will be passed to the bundle associated with this context when it is
 * stopped using the {@link BundleActivator#stop} method. A
 * <code>BundleContext</code> object is generally for the private use of its
 * associated bundle and is not meant to be shared with other bundles in the
 * OSGi environment.
 * 
 * <p>
 * The <code>Bundle</code> object associated with a <code>BundleContext</code>
 * object is called the <em>context bundle</em>.
 * 
 * <p>
 * The <code>BundleContext</code> object is only valid during the execution of
 * its context bundle; that is, during the period from when the context bundle
 * is in the <code>STARTING</code>, <code>STOPPING</code>, and
 * <code>ACTIVE</code> bundle states. If the <code>BundleContext</code>
 * object is used subsequently, an <code>IllegalStateException</code> must be
 * thrown. The <code>BundleContext</code> object must never be reused after
 * its context bundle is stopped.
 * 
 * <p>
 * The Framework is the only entity that can create <code>BundleContext</code>
 * objects and they are only valid within the Framework that created them.
 * 
 * @ThreadSafe
 * @version $Revision: 1.22 $
 */

public interface BundleContext {
	/**
	 * Returns the value of the specified property. If the key is not found in
	 * the Framework properties, the system properties are then searched. The
	 * method returns <code>null</code> if the property is not found.
	 * 
	 * <p>
	 * The Framework defines the following standard property keys:
	 * </p>
	 * <ul>
	 * <li>{@link Constants#FRAMEWORK_VERSION} - The OSGi Framework version.
	 * </li>
	 * <li>{@link Constants#FRAMEWORK_VENDOR} - The Framework implementation
	 * vendor.</li>
	 * <li>{@link Constants#FRAMEWORK_LANGUAGE} - The language being used. See
	 * ISO 639 for possible values.</li>
	 * <li>{@link Constants#FRAMEWORK_OS_NAME} - The host computer operating
	 * system.</li>
	 * <li>{@link Constants#FRAMEWORK_OS_VERSION} - The host computer
	 * operating system version number.</li>
	 * <li>{@link Constants#FRAMEWORK_PROCESSOR} - The host computer processor
	 * name.</li>
	 * </ul>
	 * <p>
	 * All bundles must have permission to read these properties.
	 * 
	 * <p>
	 * Note: The last four standard properties are used by the
	 * {@link Constants#BUNDLE_NATIVECODE} <code>Manifest</code> header's
	 * matching algorithm for selecting native language code.
	 * 
	 * @param key The name of the requested property.
	 * @return The value of the requested property, or <code>null</code> if
	 *         the property is undefined.
	 * @throws java.lang.SecurityException If the caller does not have the
	 *         appropriate <code>PropertyPermission</code> to read the
	 *         property, and the Java Runtime Environment supports permissions.
	 */
	public String getProperty(String key);

	/**
	 * Returns the <code>Bundle</code> object associated with this
	 * <code>BundleContext</code>. This bundle is called the context bundle.
	 * 
	 * @return The <code>Bundle</code> object associated with this
	 *         <code>BundleContext</code>.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 */
	public Bundle getBundle();

	/**
	 * Installs a bundle from the specified location string. A bundle is
	 * obtained from <code>location</code> as interpreted by the Framework in
	 * an implementation dependent manner.
	 * <p>
	 * Every installed bundle is uniquely identified by its location string,
	 * typically in the form of a URL.
	 * 
	 * <p>
	 * The following steps are required to install a bundle:
	 * <ol>
	 * <li>If a bundle containing the same location string is already
	 * installed, the <code>Bundle</code> object for that bundle is returned.
	 * 
	 * <li>The bundle's content is read from the location string. If this
	 * fails, a {@link BundleException} is thrown.
	 * 
	 * <li>The bundle's <code>Bundle-NativeCode</code> dependencies are
	 * resolved. If this fails, a <code>BundleException</code> is thrown.
	 * 
	 * <li>The bundle's associated resources are allocated. The associated
	 * resources minimally consist of a unique identifier and a persistent
	 * storage area if the platform has file system support. If this step fails,
	 * a <code>BundleException</code> is thrown.
	 * 
	 * <li>If the bundle has declared an Bundle-RequiredExecutionEnvironment
	 * header, then the listed execution environments must be verified against
	 * the installed execution environments. If none of the listed execution 
	 * environments match an installed execution environment, a
	 * <code>BundleException</code> must be thrown.
	 * 
	 * <li>The bundle's state is set to <code>INSTALLED</code>.
	 * 
	 * <li>A bundle event of type {@link BundleEvent#INSTALLED} is fired.
	 * 
	 * <li>The <code>Bundle</code> object for the newly or previously
	 * installed bundle is returned.
	 * </ol>
	 * 
	 * <b>Postconditions, no exceptions thrown </b>
	 * <ul>
	 * <li><code>getState()</code> in {<code>INSTALLED</code>,<code>RESOLVED</code>}.
	 * <li>Bundle has a unique ID.
	 * </ul>
	 * <b>Postconditions, when an exception is thrown </b>
	 * <ul>
	 * <li>Bundle is not installed and no trace of the bundle exists.
	 * </ul>
	 * 
	 * @param location The location identifier of the bundle to install.
	 * @return The <code>Bundle</code> object of the installed bundle.
	 * @throws BundleException If the installation failed.
	 * @throws java.lang.SecurityException If the caller does not have the
	 *         appropriate <code>AdminPermission[installed bundle,LIFECYCLE]</code>, and the
	 *         Java Runtime Environment supports permissions.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 */
	public Bundle installBundle(String location)
			throws BundleException;

	/**
	 * Installs a bundle from the specified <code>InputStream</code> object.
	 * 
	 * <p>
	 * This method performs all of the steps listed in
	 * <code>BundleContext.installBundle(String location)</code>, except that
	 * the bundle's content will be read from the <code>InputStream</code>
	 * object. The location identifier string specified will be used as the
	 * identity of the bundle.
	 * 
	 * <p>
	 * This method must always close the <code>InputStream</code> object, even
	 * if an exception is thrown.
	 * 
	 * @param location The location identifier of the bundle to install.
	 * @param input The <code>InputStream</code> object from which this bundle
	 *        will be read.
	 * @return The <code>Bundle</code> object of the installed bundle.
	 * @throws BundleException If the provided stream cannot be read or the
	 *         installation failed.
	 * @throws java.lang.SecurityException If the caller does not have the
	 *         appropriate <code>AdminPermission[installed bundle,LIFECYCLE]</code>, and the
	 *         Java Runtime Environment supports permissions.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 * @see #installBundle(java.lang.String)
	 */
	public Bundle installBundle(String location, InputStream input)
			throws BundleException;

	/**
	 * Returns the bundle with the specified identifier.
	 * 
	 * @param id The identifier of the bundle to retrieve.
	 * @return A <code>Bundle</code> object or <code>null</code> if the
	 *         identifier does not match any installed bundle.
	 */
	public Bundle getBundle(long id);

	/**
	 * Returns a list of all installed bundles.
	 * <p>
	 * This method returns a list of all bundles installed in the OSGi
	 * environment at the time of the call to this method. However, since the
	 * Framework is a very dynamic environment, bundles can be installed or
	 * uninstalled at anytime.
	 * 
	 * @return An array of <code>Bundle</code> objects, one object per
	 *         installed bundle.
	 */
	public Bundle[] getBundles();

	/**
	 * Adds the specified <code>ServiceListener</code> object with the
	 * specified <code>filter</code> to the context bundle's list of
	 * listeners. See {@link Filter} for a description of the filter syntax.
	 * <code>ServiceListener</code> objects are notified when a service has a
	 * lifecycle state change.
	 * 
	 * <p>
	 * If the context bundle's list of listeners already contains a listener
	 * <code>l</code> such that <code>(l==listener)</code>, then this
	 * method replaces that listener's filter (which may be <code>null</code>)
	 * with the specified one (which may be <code>null</code>).
	 * 
	 * <p>
	 * The listener is called if the filter criteria is met. To filter based
	 * upon the class of the service, the filter should reference the
	 * {@link Constants#OBJECTCLASS} property. If <code>filter</code> is
	 * <code>null</code>, all services are considered to match the filter.
	 * 
	 * <p>
	 * When using a <code>filter</code>, it is possible that the
	 * <code>ServiceEvent</code>s for the complete lifecycle of a service
	 * will not be delivered to the listener. For example, if the
	 * <code>filter</code> only matches when the property <code>x</code> has
	 * the value <code>1</code>, the listener will not be called if the
	 * service is registered with the property <code>x</code> not set to the
	 * value <code>1</code>. Subsequently, when the service is modified
	 * setting property <code>x</code> to the value <code>1</code>, the
	 * filter will match and the listener will be called with a
	 * <code>ServiceEvent</code> of type <code>MODIFIED</code>. Thus, the
	 * listener will not be called with a <code>ServiceEvent</code> of type
	 * <code>REGISTERED</code>.
	 * 
	 * <p>
	 * If the Java Runtime Environment supports permissions, the
	 * <code>ServiceListener</code> object will be notified of a service event
	 * only if the bundle that is registering it has the
	 * <code>ServicePermission</code> to get the service using at least one of
	 * the named classes the service was registered under.
	 * 
	 * @param listener The <code>ServiceListener</code> object to be added.
	 * @param filter The filter criteria.
	 * 
	 * @throws InvalidSyntaxException If <code>filter</code> contains an
	 *         invalid filter string that cannot be parsed.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 * 
	 * @see ServiceEvent
	 * @see ServiceListener
	 * @see ServicePermission
	 */
	public void addServiceListener(ServiceListener listener,
			String filter) throws InvalidSyntaxException;

	/**
	 * Adds the specified <code>ServiceListener</code> object to the context
	 * bundle's list of listeners.
	 * 
	 * <p>
	 * This method is the same as calling
	 * <code>BundleContext.addServiceListener(ServiceListener listener,
	 * String filter)</code>
	 * with <code>filter</code> set to <code>null</code>.
	 * 
	 * @param listener The <code>ServiceListener</code> object to be added.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 * 
	 * @see #addServiceListener(ServiceListener, String)
	 */
	public void addServiceListener(ServiceListener listener);

	/**
	 * Removes the specified <code>ServiceListener</code> object from the
	 * context bundle's list of listeners.
	 * 
	 * <p>
	 * If <code>listener</code> is not contained in this context bundle's list
	 * of listeners, this method does nothing.
	 * 
	 * @param listener The <code>ServiceListener</code> to be removed.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 */
	public void removeServiceListener(ServiceListener listener);

	/**
	 * Adds the specified <code>BundleListener</code> object to the context
	 * bundle's list of listeners if not already present. BundleListener objects
	 * are notified when a bundle has a lifecycle state change.
	 * 
	 * <p>
	 * If the context bundle's list of listeners already contains a listener
	 * <code>l</code> such that <code>(l==listener)</code>, this method
	 * does nothing.
	 * 
	 * @param listener The <code>BundleListener</code> to be added.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 * @throws java.lang.SecurityException If listener is a
	 *         <code>SynchronousBundleListener</code> and the caller does not
	 *         have the appropriate <code>AdminPermission[context bundle,LISTENER]</code>,
	 *         and the Java Runtime Environment supports permissions.
	 * 
	 * @see BundleEvent
	 * @see BundleListener
	 */
	public void addBundleListener(BundleListener listener);

	/**
	 * Removes the specified <code>BundleListener</code> object from the
	 * context bundle's list of listeners.
	 * 
	 * <p>
	 * If <code>listener</code> is not contained in the context bundle's list
	 * of listeners, this method does nothing.
	 * 
	 * @param listener The <code>BundleListener</code> object to be removed.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 * @throws java.lang.SecurityException If listener is a
	 *         <code>SynchronousBundleListener</code> and the caller does not
	 *         have the appropriate <code>AdminPermission[context bundle,LISTENER]</code>,
	 *         and the Java Runtime Environment supports permissions.
	 */
	public void removeBundleListener(BundleListener listener);

	/**
	 * Adds the specified <code>FrameworkListener</code> object to the context
	 * bundle's list of listeners if not already present. FrameworkListeners are
	 * notified of general Framework events.
	 * 
	 * <p>
	 * If the context bundle's list of listeners already contains a listener
	 * <code>l</code> such that <code>(l==listener)</code>, this method
	 * does nothing.
	 * 
	 * @param listener The <code>FrameworkListener</code> object to be added.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 * 
	 * @see FrameworkEvent
	 * @see FrameworkListener
	 */
	public void addFrameworkListener(FrameworkListener listener);

	/**
	 * Removes the specified <code>FrameworkListener</code> object from the
	 * context bundle's list of listeners.
	 * 
	 * <p>
	 * If <code>listener</code> is not contained in the context bundle's list
	 * of listeners, this method does nothing.
	 * 
	 * @param listener The <code>FrameworkListener</code> object to be
	 *        removed.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 */
	public void removeFrameworkListener(FrameworkListener listener);

	/**
	 * Registers the specified service object with the specified properties
	 * under the specified class names into the Framework. A
	 * <code>ServiceRegistration</code> object is returned. The
	 * <code>ServiceRegistration</code> object is for the private use of the
	 * bundle registering the service and should not be shared with other
	 * bundles. The registering bundle is defined to be the context bundle.
	 * Other bundles can locate the service by using either the
	 * {@link #getServiceReferences} or {@link #getServiceReference} method.
	 * 
	 * <p>
	 * A bundle can register a service object that implements the
	 * {@link ServiceFactory} interface to have more flexibility in providing
	 * service objects to other bundles.
	 * 
	 * <p>
	 * The following steps are required to register a service:
	 * <ol>
	 * <li>If <code>service</code> is not a <code>ServiceFactory</code>,
	 * an <code>IllegalArgumentException</code> is thrown if
	 * <code>service</code> is not an <code>instanceof</code> all the
	 * classes named.
	 * <li>The Framework adds these service properties to the specified
	 * <code>Dictionary</code> (which may be <code>null</code>): a property
	 * named {@link Constants#SERVICE_ID} identifying the registration number of
	 * the service and a property named {@link Constants#OBJECTCLASS} containing
	 * all the specified classes. If any of these properties have already been
	 * specified by the registering bundle, their values will be overwritten by
	 * the Framework.
	 * <li>The service is added to the Framework service registry and may now
	 * be used by other bundles.
	 * <li>A service event of type {@link ServiceEvent#REGISTERED} is
	 * fired.
	 * <li>A <code>ServiceRegistration</code> object for this registration is
	 * returned.
	 * </ol>
	 * 
	 * @param clazzes The class names under which the service can be located.
	 *        The class names in this array will be stored in the service's
	 *        properties under the key {@link Constants#OBJECTCLASS}.
	 * @param service The service object or a <code>ServiceFactory</code>
	 *        object.
	 * @param properties The properties for this service. The keys in the
	 *        properties object must all be <code>String</code> objects. See
	 *        {@link Constants} for a list of standard service property keys.
	 *        Changes should not be made to this object after calling this
	 *        method. To update the service's properties the
	 *        {@link ServiceRegistration#setProperties} method must be called.
	 *        The set of properties may be <code>null</code> if the service
	 *        has no properties.
	 * 
	 * @return A <code>ServiceRegistration</code> object for use by the bundle
	 *         registering the service to update the service's properties or to
	 *         unregister the service.
	 * 
	 * @throws java.lang.IllegalArgumentException If one of the following is
	 *         true:
	 *         <ul>
	 *         <li><code>service</code> is <code>null</code>.
	 *         <li><code>service</code> is not a <code>ServiceFactory</code>
	 *         object and is not an instance of all the named classes in
	 *         <code>clazzes</code>.
	 *         <li><code>properties</code> contains case variants of the same
	 *         key name.
	 *         </ul>
	 * 
	 * @throws java.lang.SecurityException If the caller does not have the
	 *         <code>ServicePermission</code> to register the service for all
	 *         the named classes and the Java Runtime Environment supports
	 *         permissions.
	 * 
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 * 
	 * @see ServiceRegistration
	 * @see ServiceFactory
	 */
	public ServiceRegistration registerService(String[] clazzes,
			Object service, Dictionary properties);

	/**
	 * Registers the specified service object with the specified properties
	 * under the specified class name with the Framework.
	 * 
	 * <p>
	 * This method is otherwise identical to
	 * {@link #registerService(java.lang.String[], java.lang.Object,
	 * java.util.Dictionary)} and is provided as a convenience when
	 * <code>service</code> will only be registered under a single class name.
	 * Note that even in this case the value of the service's
	 * {@link Constants#OBJECTCLASS} property will be an array of strings,
	 * rather than just a single string.
	 * 
	 * @param clazz The class name under which the service can be located.
	 * @param service The service object or a <code>ServiceFactory</code>
	 *        object.
	 * @param properties The properties for this service. 
	 * 
	 * @return A <code>ServiceRegistration</code> object for use by the bundle
	 *         registering the service to update the service's properties or to
	 *         unregister the service.
	 *         
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 * @see #registerService(java.lang.String[], java.lang.Object,
	 *      java.util.Dictionary)
	 */
	public ServiceRegistration registerService(String clazz,
			Object service, Dictionary properties);

	/**
	 * Returns an array of <code>ServiceReference</code> objects. The returned
	 * array of <code>ServiceReference</code> objects contains services that
	 * were registered under the specified class, match the specified filter
	 * criteria, and the packages for the class names under which the services
	 * were registered match the context bundle's packages as defined in
	 * {@link ServiceReference#isAssignableTo(Bundle, String)}.
	 * 
	 * <p>
	 * The list is valid at the time of the call to this method, however since
	 * the Framework is a very dynamic environment, services can be modified or
	 * unregistered at anytime.
	 * 
	 * <p>
	 * <code>filter</code> is used to select the registered service whose
	 * properties objects contain keys and values which satisfy the filter. See
	 * {@link Filter} for a description of the filter string syntax.
	 * 
	 * <p>
	 * If <code>filter</code> is <code>null</code>, all registered services
	 * are considered to match the filter. If <code>filter</code> cannot be
	 * parsed, an {@link InvalidSyntaxException} will be thrown with a human
	 * readable message where the filter became unparsable.
	 * 
	 * <p>
	 * The following steps are required to select a set of
	 * <code>ServiceReference</code> objects:
	 * <ol>
	 * <li>If the filter string is not <code>null</code>, the filter string
	 * is parsed and the set <code>ServiceReference</code> objects of
	 * registered services that satisfy the filter is produced. If the filter
	 * string is <code>null</code>, then all registered services are
	 * considered to satisfy the filter.
	 * <li>If the Java Runtime Environment supports permissions, the set of
	 * <code>ServiceReference</code> objects produced by the previous step is
	 * reduced by checking that the caller has the
	 * <code>ServicePermission</code> to get at least one of the class names
	 * under which the service was registered. If the caller does not have the
	 * correct permission for a particular <code>ServiceReference</code>
	 * object, then it is removed from the set.
	 * <li>If <code>clazz</code> is not <code>null</code>, the set is
	 * further reduced to those services that are an <code>instanceof</code>
	 * and were registered under the specified class. The complete list of
	 * classes of which a service is an instance and which were specified when
	 * the service was registered is available from the service's
	 * {@link Constants#OBJECTCLASS} property.
	 * <li>The set is reduced one final time by cycling through each
	 * <code>ServiceReference</code> object and calling
	 * {@link ServiceReference#isAssignableTo(Bundle, String)} with the context
	 * bundle and each class name under which the <code>ServiceReference</code>
	 * object was registered. For any given <code>ServiceReference</code>
	 * object, if any call to
	 * {@link ServiceReference#isAssignableTo(Bundle, String)} returns
	 * <code>false</code>, then it is removed from the set of
	 * <code>ServiceReference</code> objects.
	 * <li>An array of the remaining <code>ServiceReference</code> objects is
	 * returned.
	 * </ol>
	 * 
	 * @param clazz The class name with which the service was registered or
	 *        <code>null</code> for all services.
	 * @param filter The filter criteria.
	 * @return An array of <code>ServiceReference</code> objects or
	 *         <code>null</code> if no services are registered which satisfy
	 *         the search.
	 * @throws InvalidSyntaxException If <code>filter</code> contains an
	 *         invalid filter string that cannot be parsed.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 */
	public ServiceReference[] getServiceReferences(String clazz,
			String filter) throws InvalidSyntaxException;

	/**
	 * Returns an array of <code>ServiceReference</code> objects. The returned
	 * array of <code>ServiceReference</code> objects contains services that
	 * were registered under the specified class and match the specified filter
	 * criteria.
	 * 
	 * <p>
	 * The list is valid at the time of the call to this method, however since
	 * the Framework is a very dynamic environment, services can be modified or
	 * unregistered at anytime.
	 * 
	 * <p>
	 * <code>filter</code> is used to select the registered service whose
	 * properties objects contain keys and values which satisfy the filter. See
	 * {@link Filter} for a description of the filter string syntax.
	 * 
	 * <p>
	 * If <code>filter</code> is <code>null</code>, all registered services
	 * are considered to match the filter. If <code>filter</code> cannot be
	 * parsed, an {@link InvalidSyntaxException} will be thrown with a human
	 * readable message where the filter became unparsable.
	 * 
	 * <p>
	 * The following steps are required to select a set of
	 * <code>ServiceReference</code> objects:
	 * <ol>
	 * <li>If the filter string is not <code>null</code>, the filter string
	 * is parsed and the set <code>ServiceReference</code> objects of
	 * registered services that satisfy the filter is produced. If the filter
	 * string is <code>null</code>, then all registered services are
	 * considered to satisfy the filter.
	 * <li>If the Java Runtime Environment supports permissions, the set of
	 * <code>ServiceReference</code> objects produced by the previous step is
	 * reduced by checking that the caller has the
	 * <code>ServicePermission</code> to get at least one of the class names
	 * under which the service was registered. If the caller does not have the
	 * correct permission for a particular <code>ServiceReference</code>
	 * object, then it is removed from the set.
	 * <li>If <code>clazz</code> is not <code>null</code>, the set is
	 * further reduced to those services that are an <code>instanceof</code>
	 * and were registered under the specified class. The complete list of
	 * classes of which a service is an instance and which were specified when
	 * the service was registered is available from the service's
	 * {@link Constants#OBJECTCLASS} property.
	 * <li>An array of the remaining <code>ServiceReference</code> objects is
	 * returned.
	 * </ol>
	 * 
	 * @param clazz The class name with which the service was registered or
	 *        <code>null</code> for all services.
	 * @param filter The filter criteria.
	 * @return An array of <code>ServiceReference</code> objects or
	 *         <code>null</code> if no services are registered which satisfy
	 *         the search.
	 * @throws InvalidSyntaxException If <code>filter</code> contains an
	 *         invalid filter string that cannot be parsed.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 * @since 1.3
	 */
	public ServiceReference[] getAllServiceReferences(String clazz,
			String filter) throws InvalidSyntaxException;

	/**
	 * Returns a <code>ServiceReference</code> object for a service that
	 * implements and was registered under the specified class.
	 * 
	 * <p>
	 * This <code>ServiceReference</code> object is valid at the time of the
	 * call to this method, however as the Framework is a very dynamic
	 * environment, services can be modified or unregistered at anytime.
	 * 
	 * <p>
	 * This method is the same as calling
	 * {@link BundleContext#getServiceReferences(String, String)} with a
	 * <code>null</code> filter string. It is provided as a convenience for
	 * when the caller is interested in any service that implements the
	 * specified class.
	 * <p>
	 * If multiple such services exist, the service with the highest ranking (as
	 * specified in its {@link Constants#SERVICE_RANKING} property) is returned.
	 * <p>
	 * If there is a tie in ranking, the service with the lowest service ID (as
	 * specified in its {@link Constants#SERVICE_ID} property); that is, the
	 * service that was registered first is returned.
	 * 
	 * @param clazz The class name with which the service was registered.
	 * @return A <code>ServiceReference</code> object, or <code>null</code>
	 *         if no services are registered which implement the named class.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 * @see #getServiceReferences(String, String)
	 */
	public ServiceReference getServiceReference(String clazz);

	/**
	 * Returns the specified service object for a service.
	 * <p>
	 * A bundle's use of a service is tracked by the bundle's use count of that
	 * service. Each time a service's service object is returned by
	 * {@link #getService(ServiceReference)} the context bundle's use count for
	 * that service is incremented by one. Each time the service is released by
	 * {@link #ungetService(ServiceReference)} the context bundle's use count
	 * for that service is decremented by one.
	 * <p>
	 * When a bundle's use count for a service drops to zero, the bundle should
	 * no longer use that service.
	 * 
	 * <p>
	 * This method will always return <code>null</code> when the service
	 * associated with this <code>reference</code> has been unregistered.
	 * 
	 * <p>
	 * The following steps are required to get the service object:
	 * <ol>
	 * <li>If the service has been unregistered, <code>null</code> is
	 * returned.
	 * <li>The context bundle's use count for this service is incremented by
	 * one.
	 * <li>If the context bundle's use count for the service is currently one
	 * and the service was registered with an object implementing the
	 * <code>ServiceFactory</code> interface, the
	 * {@link ServiceFactory#getService(Bundle, ServiceRegistration)} method is
	 * called to create a service object for the context bundle. This service
	 * object is cached by the Framework. While the context bundle's use count
	 * for the service is greater than zero, subsequent calls to get the
	 * services's service object for the context bundle will return the cached
	 * service object. <br>
	 * If the service object returned by the <code>ServiceFactory</code>
	 * object is not an <code>instanceof</code> all the classes named when the
	 * service was registered or the <code>ServiceFactory</code> object throws
	 * an exception, <code>null</code> is returned and a Framework event of
	 * type {@link FrameworkEvent#ERROR} is fired.
	 * <li>The service object for the service is returned.
	 * </ol>
	 * 
	 * @param reference A reference to the service.
	 * @return A service object for the service associated with
	 *         <code>reference</code> or <code>null</code> if the service is
	 *         not registered or does not implement the classes under which it
	 *         was registered in the case of a <code>ServiceFactory</code>.
	 * @throws java.lang.SecurityException If the caller does not have the
	 *         <code>ServicePermission</code> to get the service using at
	 *         least one of the named classes the service was registered under
	 *         and the Java Runtime Environment supports permissions.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 * @see #ungetService(ServiceReference)
	 * @see ServiceFactory
	 */
	public Object getService(ServiceReference reference);

	/**
	 * Releases the service object referenced by the specified
	 * <code>ServiceReference</code> object. If the context bundle's use count
	 * for the service is zero, this method returns <code>false</code>.
	 * Otherwise, the context bundle's use count for the service is decremented
	 * by one.
	 * 
	 * <p>
	 * The service's service object should no longer be used and all references
	 * to it should be destroyed when a bundle's use count for the service drops
	 * to zero.
	 * 
	 * <p>
	 * The following steps are required to unget the service object:
	 * <ol>
	 * <li>If the context bundle's use count for the service is zero or the
	 * service has been unregistered, <code>false</code> is returned.
	 * <li>The context bundle's use count for this service is decremented by
	 * one.
	 * <li>If the context bundle's use count for the service is currently zero
	 * and the service was registered with a <code>ServiceFactory</code>
	 * object, the
	 * {@link ServiceFactory#ungetService(Bundle, ServiceRegistration, Object)}
	 * method is called to release the service object for the context bundle.
	 * <li><code>true</code> is returned.
	 * </ol>
	 * 
	 * @param reference A reference to the service to be released.
	 * @return <code>false</code> if the context bundle's use count for the
	 *         service is zero or if the service has been unregistered;
	 *         <code>true</code> otherwise.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 * @see #getService
	 * @see ServiceFactory
	 */
	public boolean ungetService(ServiceReference reference);

	/**
	 * Creates a <code>File</code> object for a file in the persistent storage
	 * area provided for the bundle by the Framework. This method will return
	 * <code>null</code> if the platform does not have file system support.
	 * 
	 * <p>
	 * A <code>File</code> object for the base directory of the persistent
	 * storage area provided for the context bundle by the Framework can be
	 * obtained by calling this method with an empty string as
	 * <code>filename</code>.
	 * 
	 * <p>
	 * If the Java Runtime Environment supports permissions, the Framework will
	 * ensure that the bundle has the <code>java.io.FilePermission</code> with
	 * actions <code>read</code>,<code>write</code>,<code>delete</code>
	 * for all files (recursively) in the persistent storage area provided for
	 * the context bundle.
	 * 
	 * @param filename A relative name to the file to be accessed.
	 * @return A <code>File</code> object that represents the requested file
	 *         or <code>null</code> if the platform does not have file system
	 *         support.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 */
	public File getDataFile(String filename);

	/**
	 * Creates a <code>Filter</code> object. This <code>Filter</code> object
	 * may be used to match a <code>ServiceReference</code> object or a
	 * <code>Dictionary</code> object.
	 * 
	 * <p>
	 * If the filter cannot be parsed, an {@link InvalidSyntaxException} will be
	 * thrown with a human readable message where the filter became unparsable.
	 * 
	 * @param filter The filter string.
	 * @return A <code>Filter</code> object encapsulating the filter string.
	 * @throws InvalidSyntaxException If <code>filter</code> contains an
	 *         invalid filter string that cannot be parsed.
	 * @throws NullPointerException If <code>filter</code> is null.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 * 
	 * @since 1.1
	 * @see "Framework specification for a description of the filter string syntax."
	 * @see FrameworkUtil#createFilter(String)
	 */
	public Filter createFilter(String filter)
			throws InvalidSyntaxException;
}
