/*
 * $Header: /cvshome/build/org.osgi.service.component/src/org/osgi/service/component/ComponentContext.java,v 1.20 2006/06/16 16:31:26 hargrave Exp $
 *
 * Copyright (c) OSGi Alliance (2004, 2006). All Rights Reserved.
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

package org.osgi.service.component;

import java.util.Dictionary;

import org.osgi.framework.*;

/**
 * A Component Context object is used by a component instance to interact with
 * its execution context including locating services by reference name. Each
 * component instance has a unique Component Context.
 * 
 * <p>
 * A component's implementation class may optionaly implement an activate
 * method:
 * 
 * <pre>
 * protected void activate(ComponentContext context);
 * </pre>
 * 
 * If a component implements this method, this method will be called when a
 * component configuration is activated to provide the component instance's
 * Component Context object.
 * 
 * <p>
 * A component's implementation class may optionaly implement a deactivate
 * method:
 * 
 * <pre>
 * protected void deactivate(ComponentContext context);
 * </pre>
 * 
 * If a component implements this method, this method will be called when the
 * component configuration is deactivated.
 * 
 * <p>
 * The activate and deactivate methods will be called using reflection and must
 * be protected or public accessible. These methods should not be public methods
 * so that they do not appear as public methods on the component instance when
 * used as a service object. These methods will be located by looking through
 * the component's implementation class hierarchy for the first declaration of
 * the method. If the method is found, if it is declared protected or public,
 * the method will be called. Otherwise, the method will not be called.
 * 
 * @version $Revision: 1.20 $
 */
public interface ComponentContext {
	/**
	 * Returns the component properties for this Component Context.
	 * 
	 * @return The properties for this Component Context. The Dictionary is read
	 *         only and cannot be modified.
	 */
	public Dictionary getProperties();

	/**
	 * Returns the service object for the specified reference name.
	 * 
	 * <p>
	 * If the cardinality of the reference is <code>0..n</code> or
	 * <code>1..n</code> and multiple services are bound to the reference, the
	 * service with the highest ranking (as specified in its
	 * <code>Constants.SERVICE_RANKING</code> property) is returned. If there
	 * is a tie in ranking, the service with the lowest service ID (as specified
	 * in its <code>Constants.SERVICE_ID</code> property); that is, the
	 * service that was registered first is returned.
	 * 
	 * @param name The name of a reference as specified in a
	 *        <code>reference</code> element in this component's description.
	 * @return A service object for the referenced service or <code>null</code>
	 *         if the reference cardinality is <code>0..1</code> or
	 *         <code>0..n</code> and no bound service is available.
	 * @throws ComponentException If the Service Component Runtime catches an
	 *         exception while activating the bound service.
	 */
	public Object locateService(String name);

	/**
	 * Returns the service object for the specified reference name and
	 * <code>ServiceReference</code>.
	 * 
	 * @param name The name of a reference as specified in a
	 *        <code>reference</code> element in this component's description.
	 * @param reference The <code>ServiceReference</code> to a bound service.
	 *        This must be a <code>ServiceReference</code> provided to the
	 *        component via the bind or unbind method for the specified
	 *        reference name.
	 * @return A service object for the referenced service or <code>null</code>
	 *         if the specified <code>ServiceReference</code> is not a bound
	 *         service for the specified reference name.
	 * @throws ComponentException If the Service Component Runtime catches an
	 *         exception while activating the bound service.
	 */
	public Object locateService(String name, ServiceReference reference);

	/**
	 * Returns the service objects for the specified reference name.
	 * 
	 * @param name The name of a reference as specified in a
	 *        <code>reference</code> element in this component's description.
	 * @return An array of service objects for the referenced service or
	 *         <code>null</code> if the reference cardinality is
	 *         <code>0..1</code> or <code>0..n</code> and no bound service
	 *         is available.
	 * @throws ComponentException If the Service Component Runtime catches an
	 *         exception while activating a bound service.
	 */
	public Object[] locateServices(String name);

	/**
	 * Returns the <code>BundleContext</code> of the bundle which contains
	 * this component.
	 * 
	 * @return The <code>BundleContext</code> of the bundle containing this
	 *         component.
	 */
	public BundleContext getBundleContext();

	/**
	 * If the component instance is registered as a service using the
	 * <code>servicefactory=&quot;true&quot;</code> attribute, then this
	 * method returns the bundle using the service provided by the component
	 * instance.
	 * <p>
	 * This method will return <code>null</code> if:
	 * <ul>
	 * <li>The component instance is not a service, then no bundle can be using
	 * it as a service.
	 * <li>The component instance is a service but did not specify the
	 * <code>servicefactory=&quot;true&quot;</code> attribute, then all
	 * bundles using the service provided by the component instance will share
	 * the same component instance.
	 * <li>The service provided by the component instance is not currently
	 * being used by any bundle.
	 * </ul>
	 * 
	 * @return The bundle using the component instance as a service or
	 *         <code>null</code>.
	 */
	public Bundle getUsingBundle();

	/**
	 * Returns the Component Instance object for the component instance
	 * associated with this Component Context.
	 * 
	 * @return The Component Instance object for the component instance.
	 */
	public ComponentInstance getComponentInstance();

	/**
	 * Enables the specified component name. The specified component name must
	 * be in the same bundle as this component.
	 * 
	 * @param name The name of a component or <code>null</code> to indicate
	 *        all components in the bundle.
	 */
	public void enableComponent(String name);

	/**
	 * Disables the specified component name. The specified component name must
	 * be in the same bundle as this component.
	 * 
	 * @param name The name of a component.
	 */
	public void disableComponent(String name);

	/**
	 * If the component instance is registered as a service using the
	 * <code>service</code> element, then this method returns the service
	 * reference of the service provided by this component instance.
	 * <p>
	 * This method will return <code>null</code> if the component instance is
	 * not registered as a service.
	 * 
	 * @return The <code>ServiceReference</code> object for the component
	 *         instance or <code>null</code> if the component instance is not
	 *         registered as a service.
	 */
	public ServiceReference getServiceReference();

}
