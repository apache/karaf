/*
 * Copyright (c) OSGi Alliance (2004, 2009). All Rights Reserved.
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
package org.osgi.service.cm;

import java.util.Dictionary;

import org.osgi.framework.ServiceReference;

/**
 * A Configuration Event.
 * 
 * <p>
 * <code>ConfigurationEvent</code> objects are delivered to all registered
 * <code>ConfigurationListener</code> service objects. ConfigurationEvents
 * must be asynchronously delivered in chronological order with respect to each
 * listener.
 * 
 * <p>
 * A type code is used to identify the type of event. The following event types
 * are defined:
 * <ul>
 * <li>{@link #CM_UPDATED}
 * <li>{@link #CM_DELETED}
 * </ul>
 * Additional event types may be defined in the future.
 * 
 * <p>
 * Security Considerations. <code>ConfigurationEvent</code> objects do not
 * provide <code>Configuration</code> objects, so no sensitive configuration
 * information is available from the event. If the listener wants to locate the
 * <code>Configuration</code> object for the specified pid, it must use
 * <code>ConfigurationAdmin</code>.
 * 
 * @see ConfigurationListener
 * 
 * @version $Revision: 6180 $
 * @since 1.2
 */
public class ConfigurationEvent {
	/**
	 * A <code>Configuration</code> has been updated.
	 * 
	 * <p>
	 * This <code>ConfigurationEvent</code> type that indicates that a
	 * <code>Configuration</code> object has been updated with new properties.
	 * 
	 * An event is fired when a call to {@link Configuration#update(Dictionary)}
	 * successfully changes a configuration.
	 * 
	 * <p>
	 * The value of <code>CM_UPDATED</code> is 1.
	 */
	public static final int			CM_UPDATED	= 1;
	/**
	 * A <code>Configuration</code> has been deleted.
	 * 
	 * <p>
	 * This <code>ConfigurationEvent</code> type that indicates that a
	 * <code>Configuration</code> object has been deleted.
	 * 
	 * An event is fired when a call to {@link Configuration#delete()}
	 * successfully deletes a configuration.
	 * 
	 * <p>
	 * The value of <code>CM_DELETED</code> is 2.
	 */
	public static final int			CM_DELETED	= 2;
	/**
	 * Type of this event.
	 * 
	 * @see #getType
	 */
	private final int				type;
	/**
	 * The factory pid associated with this event.
	 */
	private final String			factoryPid;
	/**
	 * The pid associated with this event.
	 */
	private final String			pid;
	/**
	 * The ConfigurationAdmin service which created this event.
	 */
	private final ServiceReference	reference;

	/**
	 * Constructs a <code>ConfigurationEvent</code> object from the given
	 * <code>ServiceReference</code> object, event type, and pids.
	 * 
	 * @param reference The <code>ServiceReference</code> object of the
	 *        Configuration Admin service that created this event.
	 * @param type The event type. See {@link #getType}.
	 * @param factoryPid The factory pid of the associated configuration if the
	 *        target of the configuration is a ManagedServiceFactory. Otherwise
	 *        <code>null</code> if the target of the configuration is a
	 *        ManagedService.
	 * @param pid The pid of the associated configuration.
	 */
	public ConfigurationEvent(ServiceReference reference, int type,
			String factoryPid, String pid) {
		this.reference = reference;
		this.type = type;
		this.factoryPid = factoryPid;
		this.pid = pid;
		if ((reference == null) || (pid == null)) {
			throw new NullPointerException("reference and pid must not be null");
		}
	}

	/**
	 * Returns the factory pid of the associated configuration.
	 * 
	 * @return Returns the factory pid of the associated configuration if the
	 *         target of the configuration is a ManagedServiceFactory. Otherwise
	 *         <code>null</code> if the target of the configuration is a
	 *         ManagedService.
	 */
	public String getFactoryPid() {
		return factoryPid;
	}

	/**
	 * Returns the pid of the associated configuration.
	 * 
	 * @return Returns the pid of the associated configuration.
	 */
	public String getPid() {
		return pid;
	}

	/**
	 * Return the type of this event.
	 * <p>
	 * The type values are:
	 * <ul>
	 * <li>{@link #CM_UPDATED}
	 * <li>{@link #CM_DELETED}
	 * </ul>
	 * 
	 * @return The type of this event.
	 */
	public int getType() {
		return type;
	}

	/**
	 * Return the <code>ServiceReference</code> object of the Configuration
	 * Admin service that created this event.
	 * 
	 * @return The <code>ServiceReference</code> object for the Configuration
	 *         Admin service that created this event.
	 */
	public ServiceReference getReference() {
		return reference;
	}
}
