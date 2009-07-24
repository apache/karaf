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

/**
 * Listener for UserAdminEvents.
 * 
 * <p>
 * <code>UserAdminListener</code> objects are registered with the Framework
 * service registry and notified with a <code>UserAdminEvent</code> object when a
 * <code>Role</code> object has been created, removed, or modified.
 * <p>
 * <code>UserAdminListener</code> objects can further inspect the received
 * <code>UserAdminEvent</code> object to determine its type, the <code>Role</code>
 * object it occurred on, and the User Admin service that generated it.
 * 
 * @see UserAdmin
 * @see UserAdminEvent
 * 
 * @version $Revision: 5673 $
 */
public interface UserAdminListener {
	/**
	 * Receives notification that a <code>Role</code> object has been created,
	 * removed, or modified.
	 * 
	 * @param event The <code>UserAdminEvent</code> object.
	 */
	public void roleChanged(UserAdminEvent event);
}
