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

import java.util.Dictionary;

/**
 * A <code>User</code> role managed by a User Admin service.
 * 
 * <p>
 * In this context, the term &quot;user&quot; is not limited to just human
 * beings. Instead, it refers to any entity that may have any number of
 * credentials associated with it that it may use to authenticate itself.
 * <p>
 * In general, <code>User</code> objects are associated with a specific User Admin
 * service (namely the one that created them), and cannot be used with other
 * User Admin services.
 * <p>
 * A <code>User</code> object may have credentials (and properties, inherited from
 * the {@link Role} class) associated with it. Specific
 * {@link UserAdminPermission} objects are required to read or change a
 * <code>User</code> object's credentials.
 * <p>
 * Credentials are <code>Dictionary</code> objects and have semantics that are
 * similar to the properties in the <code>Role</code> class.
 * 
 * @version $Revision: 5673 $
 */
public interface User extends Role {
	/**
	 * Returns a <code>Dictionary</code> of the credentials of this <code>User</code>
	 * object. Any changes to the returned <code>Dictionary</code> object will
	 * change the credentials of this <code>User</code> object. This will cause a
	 * <code>UserAdminEvent</code> object of type
	 * {@link UserAdminEvent#ROLE_CHANGED} to be broadcast to any
	 * <code>UserAdminListeners</code> objects.
	 * 
	 * <p>
	 * Only objects of type <code>String</code> may be used as credential keys,
	 * and only objects of type <code>String</code> or of type <code>byte[]</code>
	 * may be used as credential values. Any other types will cause an exception
	 * of type <code>IllegalArgumentException</code> to be raised.
	 * 
	 * <p>
	 * In order to retrieve a credential from the returned <code>Dictionary</code>
	 * object, a {@link UserAdminPermission} named after the credential name (or
	 * a prefix of it) with action <code>getCredential</code> is required.
	 * <p>
	 * In order to add or remove a credential from the returned
	 * <code>Dictionary</code> object, a {@link UserAdminPermission} named after
	 * the credential name (or a prefix of it) with action
	 * <code>changeCredential</code> is required.
	 * 
	 * @return <code>Dictionary</code> object containing the credentials of this
	 *         <code>User</code> object.
	 */
	public Dictionary getCredentials();

	/**
	 * Checks to see if this <code>User</code> object has a credential with the
	 * specified <code>key</code> set to the specified <code>value</code>.
	 * 
	 * <p>
	 * If the specified credential <code>value</code> is not of type
	 * <code>String</code> or <code>byte[]</code>, it is ignored, that is,
	 * <code>false</code> is returned (as opposed to an
	 * <code>IllegalArgumentException</code> being raised).
	 * 
	 * @param key The credential <code>key</code>.
	 * @param value The credential <code>value</code>.
	 * 
	 * @return <code>true</code> if this user has the specified credential;
	 *         <code>false</code> otherwise.
	 * 
	 * @throws SecurityException If a security manager exists and the caller
	 *         does not have the <code>UserAdminPermission</code> named after the
	 *         credential key (or a prefix of it) with action
	 *         <code>getCredential</code>.
	 */
	public boolean hasCredential(String key, Object value);
}
