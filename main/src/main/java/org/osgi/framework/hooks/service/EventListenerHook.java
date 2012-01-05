/*
 * Copyright (c) OSGi Alliance (2010). All Rights Reserved.
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
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;

/**
 * OSGi Framework Service Event Listener Hook Service.
 * 
 * <p>
 * Bundles registering this service will be called during framework service
 * (register, modify, and unregister service) operations.
 * 
 * @ThreadSafe
 * @since 1.1
 * @version $Id: 61c6aa7e7d4c85b3e5a6a3a340155bcda0074505 $
 */

public interface EventListenerHook {
	/**
	 * Event listener hook method. This method is called prior to service event
	 * delivery when a publishing bundle registers, modifies or unregisters a
	 * service. This method can filter the listeners which receive the event.
	 * 
	 * @param event The service event to be delivered.
	 * @param listeners A map of Bundle Contexts to a collection of Listener
	 *        Infos for the bundle's listeners to which the specified event will
	 *        be delivered. The implementation of this method may remove bundle
	 *        contexts from the map and listener infos from the collection
	 *        values to prevent the event from being delivered to the associated
	 *        listeners. The map supports all the optional {@code Map}
	 *        operations except {@code put} and {@code putAll}. Attempting to
	 *        add to the map will result in an
	 *        {@code UnsupportedOperationException}. The collection values in
	 *        the map supports all the optional {@code Collection} operations
	 *        except {@code add} and {@code addAll}. Attempting to add to a
	 *        collection will result in an {@code UnsupportedOperationException}
	 *        . The map and the collections are not synchronized.
	 */
	void event(ServiceEvent event,
			Map<BundleContext, Collection<ListenerInfo>> listeners);
}
