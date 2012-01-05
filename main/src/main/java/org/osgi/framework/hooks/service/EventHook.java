/*
 * Copyright (c) OSGi Alliance (2008, 2010). All Rights Reserved.
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

package org.osgi.framework.hooks.service;

import java.util.Collection;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;

/**
 * OSGi Framework Service Event Hook Service.
 * 
 * <p>
 * Bundles registering this service will be called during framework service
 * (register, modify, and unregister service) operations.
 * 
 * @ThreadSafe
 * @deprecated As of 1.1. Replaced by {@link EventListenerHook}.
 * @version $Id: 8fb8cfa2c8847f99fd84711e12f02a57bf06932e $
 */

public interface EventHook {
	/**
	 * Event hook method. This method is called prior to service event delivery
	 * when a publishing bundle registers, modifies or unregisters a service.
	 * This method can filter the bundles which receive the event.
	 * 
	 * @param event The service event to be delivered.
	 * @param contexts A collection of Bundle Contexts for bundles which have
	 *        listeners to which the specified event will be delivered. The
	 *        implementation of this method may remove bundle contexts from the
	 *        collection to prevent the event from being delivered to the
	 *        associated bundles. The collection supports all the optional
	 *        {@code Collection} operations except {@code add} and
	 *        {@code addAll}. Attempting to add to the collection will
	 *        result in an {@code UnsupportedOperationException}. The
	 *        collection is not synchronized.
	 */
	void event(ServiceEvent event, Collection<BundleContext> contexts);
}
