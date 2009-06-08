/*
 * Copyright (c) OSGi Alliance (2000, 2009). All Rights Reserved.
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

import java.util.Dictionary;
import java.util.EventObject;

/**
 * An event from the Framework describing a service lifecycle change.
 * <p>
 * <code>ServiceEvent</code> objects are delivered to
 * <code>ServiceListener</code>s and <code>AllServiceListener</code>s when a
 * change occurs in this service's lifecycle. A type code is used to identify
 * the event type for future extendability.
 * 
 * <p>
 * OSGi Alliance reserves the right to extend the set of types.
 * 
 * @Immutable
 * @see ServiceListener
 * @see AllServiceListener
 * @version $Revision: 6542 $
 */

public class ServiceEvent extends EventObject {
	static final long				serialVersionUID	= 8792901483909409299L;
	/**
	 * Reference to the service that had a change occur in its lifecycle.
	 */
	private final ServiceReference	reference;

	/**
	 * Type of service lifecycle change.
	 */
	private final int				type;

	/**
	 * This service has been registered.
	 * <p>
	 * This event is synchronously delivered <strong>after</strong> the service
	 * has been registered with the Framework.
	 * 
	 * @see BundleContext#registerService(String[],Object,Dictionary)
	 */
	public final static int			REGISTERED			= 0x00000001;

	/**
	 * The properties of a registered service have been modified.
	 * <p>
	 * This event is synchronously delivered <strong>after</strong> the service
	 * properties have been modified.
	 * 
	 * @see ServiceRegistration#setProperties
	 */
	public final static int			MODIFIED			= 0x00000002;

	/**
	 * This service is in the process of being unregistered.
	 * <p>
	 * This event is synchronously delivered <strong>before</strong> the service
	 * has completed unregistering.
	 * 
	 * <p>
	 * If a bundle is using a service that is <code>UNREGISTERING</code>, the
	 * bundle should release its use of the service when it receives this event.
	 * If the bundle does not release its use of the service when it receives
	 * this event, the Framework will automatically release the bundle's use of
	 * the service while completing the service unregistration operation.
	 * 
	 * @see ServiceRegistration#unregister
	 * @see BundleContext#ungetService
	 */
	public final static int			UNREGISTERING		= 0x00000004;

	/**
	 * The properties of a registered service have been modified and the new
	 * properties no longer match the listener's filter.
	 * <p>
	 * This event is synchronously delivered <strong>after</strong> the service
	 * properties have been modified. This event is only delivered to listeners
	 * which were added with a non-<code>null</code> filter where the filter
	 * matched the service properties prior to the modification but the filter
	 * does not match the modified service properties.
	 * 
	 * @see ServiceRegistration#setProperties
	 * @since 1.5
	 */
	public final static int			MODIFIED_ENDMATCH	= 0x00000008;

	/**
	 * Creates a new service event object.
	 * 
	 * @param type The event type.
	 * @param reference A <code>ServiceReference</code> object to the service
	 * 	that had a lifecycle change.
	 */
	public ServiceEvent(int type, ServiceReference reference) {
		super(reference);
		this.reference = reference;
		this.type = type;
	}

	/**
	 * Returns a reference to the service that had a change occur in its
	 * lifecycle.
	 * <p>
	 * This reference is the source of the event.
	 * 
	 * @return Reference to the service that had a lifecycle change.
	 */
	public ServiceReference getServiceReference() {
		return reference;
	}

	/**
	 * Returns the type of event. The event type values are:
	 * <ul>
	 * <li>{@link #REGISTERED} </li> 
	 * <li>{@link #MODIFIED} </li> 
	 * <li>{@link #MODIFIED_ENDMATCH} </li> 
	 * <li>{@link #UNREGISTERING} </li>
	 * </ul>
	 * 
	 * @return Type of service lifecycle change.
	 */

	public int getType() {
		return type;
	}
}
