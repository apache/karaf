/*
 * Copyright (c) OSGi Alliance (2001, 2008). All Rights Reserved.
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
package org.osgi.service.useradmin;

import org.osgi.framework.ServiceReference;

/**
 * <code>Role</code> change event.
 * <p>
 * <code>UserAdminEvent</code> objects are delivered asynchronously to any
 * <code>UserAdminListener</code> objects when a change occurs in any of the
 * <code>Role</code> objects managed by a User Admin service.
 * 
 * <p>
 * A type code is used to identify the event. The following event types are
 * defined: {@link #ROLE_CREATED} type, {@link #ROLE_CHANGED} type, and
 * {@link #ROLE_REMOVED} type. Additional event types may be defined in the
 * future.
 * 
 * @see UserAdmin
 * @see UserAdminListener
 * 
 * @version $Revision: 5673 $
 */
public class UserAdminEvent {
	private ServiceReference	ref;
	private int					type;
	private Role				role;
	/**
	 * A <code>Role</code> object has been created.
	 * 
	 * <p>
	 * The value of <code>ROLE_CREATED</code> is 0x00000001.
	 */
	public static final int		ROLE_CREATED	= 0x00000001;
	/**
	 * A <code>Role</code> object has been modified.
	 * 
	 * <p>
	 * The value of <code>ROLE_CHANGED</code> is 0x00000002.
	 */
	public static final int		ROLE_CHANGED	= 0x00000002;
	/**
	 * A <code>Role</code> object has been removed.
	 * 
	 * <p>
	 * The value of <code>ROLE_REMOVED</code> is 0x00000004.
	 */
	public static final int		ROLE_REMOVED	= 0x00000004;

	/**
	 * Constructs a <code>UserAdminEvent</code> object from the given
	 * <code>ServiceReference</code> object, event type, and <code>Role</code>
	 * object.
	 * 
	 * @param ref The <code>ServiceReference</code> object of the User Admin
	 *        service that generated this event.
	 * @param type The event type.
	 * @param role The <code>Role</code> object on which this event occurred.
	 */
	public UserAdminEvent(ServiceReference ref, int type, Role role) {
		this.ref = ref;
		this.type = type;
		this.role = role;
	}

	/**
	 * Gets the <code>ServiceReference</code> object of the User Admin service
	 * that generated this event.
	 * 
	 * @return The User Admin service's <code>ServiceReference</code> object.
	 */
	public ServiceReference getServiceReference() {
		return ref;
	}

	/**
	 * Returns the type of this event.
	 * 
	 * <p>
	 * The type values are {@link #ROLE_CREATED} type, {@link #ROLE_CHANGED}
	 * type, and {@link #ROLE_REMOVED} type.
	 * 
	 * @return The event type.
	 */
	public int getType() {
		return type;
	}

	/**
	 * Gets the <code>Role</code> object this event was generated for.
	 * 
	 * @return The <code>Role</code> object this event was generated for.
	 */
	public Role getRole() {
		return role;
	}
}
