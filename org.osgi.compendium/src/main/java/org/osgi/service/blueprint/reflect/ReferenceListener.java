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
 * Metadata for a reference listener interested in the reference bind and unbind
 * events for a service reference.
 * 
 * @ThreadSafe
 * @version $Revision: 7563 $
 */
public interface ReferenceListener {

	/**
	 * Return the Metadata for the component that will receive bind and unbind
	 * events.
	 * 
	 * This is specified by the <code>ref</code> attribute or via an inlined
	 * component.
	 * 
	 * @return The Metadata for the component that will receive bind and unbind
	 *         events.
	 */
	Target getListenerComponent();

	/**
	 * Return the name of the bind method. The bind method will be invoked when
	 * a matching service is bound to the reference.
	 * 
	 * This is specified by the <code>bind-method</code> attribute of the
	 * reference listener.
	 * 
	 * @return The name of the bind method.
	 */
	String getBindMethod();

	/**
	 * Return the name of the unbind method. The unbind method will be invoked
	 * when a matching service is unbound from the reference.
	 * 
	 * This is specified by the <code>unbind-method</code> attribute of the
	 * reference listener.
	 * 
	 * @return The name of the unbind method.
	 */
	String getUnbindMethod();
}
