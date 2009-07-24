/*
 * Copyright (c) OSGi Alliance (2005, 2008). All Rights Reserved.
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

package org.osgi.application;

import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;

/**
 * An event from the Framework describing a service lifecycle change.
 * <p>
 * <code>ApplicationServiceEvent</code> objects are delivered to a
 * <code>ApplicationServiceListener</code> objects when a change occurs in this service's
 * lifecycle. The delivery of an <code>ApplicationServiceEvent</code> is 
 * always triggered by a {@link org.osgi.framework.ServiceEvent}.
 * <code>ApplicationServiceEvent</code> extends the content of <code>ServiceEvent</code>
 * with the service object the event is referring to as applications has no means to
 * find the corresponding service object for a {@link org.osgi.framework.ServiceReference}.
 * A type code is used to identify the event type for future
 * extendability. The available type codes are defined in {@link org.osgi.framework.ServiceEvent}.
 * 
 * <p>
 * OSGi Alliance reserves the right to extend the set of types.
 * 
 * @see org.osgi.framework.ServiceEvent
 * @see ApplicationServiceListener
 * 
 * @version $Revision: 5673 $
 */
public class ApplicationServiceEvent extends ServiceEvent {

	private static final long serialVersionUID = -4762149286971897323L;
	final Object serviceObject;

	/**
	 * Creates a new application service event object.
	 * 
	 * @param type The event type. Available type codes are defines in 
	 *        {@link org.osgi.framework.ServiceEvent}
	 * @param reference A <code>ServiceReference</code> object to the service
	 *        that had a lifecycle change. This reference will be used as the <code>source</code>
	 *        in the {@link java.util.EventObject} baseclass, therefore, it must not be
	 *        null.
	 * @param serviceObject The service object bound to this application instance. It can
	 *    be <code>null</code> if this application is not bound to this service yet.
	 * @throws IllegalArgumentException if the specified <code>reference</code> is null. 
	 */
	public ApplicationServiceEvent(int type, ServiceReference reference, Object serviceObject) {
		super(type, reference);
		this.serviceObject = serviceObject;
	}
	
	/**
	 * This method returns the service object of this service bound to the listener
	 * application instace. A service object becomes bound to the application when it
	 * first obtains a service object reference to that service by calling the
	 * <code>ApplicationContext.locateService</code> or <code>locateServices</code>
	 * methods. If the application is not bound to the service yet, this method returns
	 * <code>null</code>.
	 * 
	 * @return the service object bound to the listener application or <code>null</code>
	 *   if it isn't bound to this service yet.
	 */
	public Object getServiceObject() {
		return this.serviceObject;
	}

}
