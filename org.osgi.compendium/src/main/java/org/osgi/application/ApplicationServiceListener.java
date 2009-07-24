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

import java.util.EventListener;

import org.osgi.framework.*;

/**
 * An <code>ApplicationServiceEvent</code> listener. When a 
 * <code>ServiceEvent</code> is
 * fired, it is converted to an <code>ApplictionServiceEvent</code>
 * and it is synchronously delivered to an <code>ApplicationServiceListener</code>.
 * 
 * <p>
 * <code>ApplicationServiceListener</code> is a listener interface that may be
 * implemented by an application developer.
 * <p>
 * An <code>ApplicationServiceListener</code> object is registered with the Framework
 * using the <code>ApplicationContext.addServiceListener</code> method.
 * <code>ApplicationServiceListener</code> objects are called with an
 * <code>ApplicationServiceEvent</code> object when a service is registered, modified, or
 * is in the process of unregistering.
 * 
 * <p>
 * <code>ApplicationServiceEvent</code> object delivery to 
 * <code>ApplicationServiceListener</code>
 * objects is filtered by the filter specified when the listener was registered.
 * If the Java Runtime Environment supports permissions, then additional
 * filtering is done. <code>ApplicationServiceEvent</code> objects are only delivered to
 * the listener if the application which defines the listener object's class has the
 * appropriate <code>ServicePermission</code> to get the service using at
 * least one of the named classes the service was registered under, and the application
 * specified its dependece on the corresponding service in the application metadata.
 * 
 * <p>
 * <code>ApplicationServiceEvent</code> object delivery to <code>ApplicationServiceListener</code>
 * objects is further filtered according to package sources as defined in
 * {@link ServiceReference#isAssignableTo(Bundle, String)}.
 * 
 * @version $Revision: 5673 $
 * @see ApplicationServiceEvent
 * @see ServicePermission
 */
public interface ApplicationServiceListener extends EventListener {
	/**
	 * Receives notification that a service has had a lifecycle change.
	 * 
	 * @param event The <code>ApplicationServiceEvent</code> object.
	 */
	public void serviceChanged(ApplicationServiceEvent event);
}
