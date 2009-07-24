/*
 * Copyright (c) OSGi Alliance (2008, 2009). All Rights Reserved.
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
package org.osgi.service.blueprint.reflect;

/**
 * Metadata for a registration listener interested in service registration and
 * unregistration events for a service.
 * 
 * <p>
 * The registration listener is called with the initial state of the service
 * when the registration listener is actuated.
 * 
 * @ThreadSafe
 * @version $Revision: 7563 $
 */
public interface RegistrationListener {

	/**
	 * Return the Metadata for the component that will receive registration and
	 * unregistration events.
	 * 
	 * This is specified by the <code>ref</code> attribute or via an inlined
	 * component.
	 * 
	 * @return The Metadata for the component that will receive registration and
	 *         unregistration events.
	 */
	Target getListenerComponent();

	/**
	 * Return the name of the registration method. The registration method will
	 * be invoked when the associated service is registered with the service
	 * registry.
	 * 
	 * This is specified by the <code>registration-method</code> attribute of
	 * the registration listener.
	 * 
	 * @return The name of the registration method.
	 */
	String getRegistrationMethod();

	/**
	 * Return the name of the unregistration method. The unregistration method
	 * will be invoked when the associated service is unregistered from the
	 * service registry.
	 * 
	 * This is specified by the <code>unregistration-method</code> attribute of
	 * the registration listener.
	 * 
	 * @return The name of the unregistration method.
	 */
	String getUnregistrationMethod();
}
