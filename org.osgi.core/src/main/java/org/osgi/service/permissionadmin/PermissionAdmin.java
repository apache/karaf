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

package org.osgi.service.permissionadmin;

/**
 * The Permission Admin service allows management agents to manage the
 * permissions of bundles. There is at most one Permission Admin service present
 * in the OSGi environment.
 * <p>
 * Access to the Permission Admin service is protected by corresponding
 * <code>ServicePermission</code>. In addition <code>AdminPermission</code>
 * is required to actually set permissions.
 * 
 * <p>
 * Bundle permissions are managed using a permission table. A bundle's location
 * serves as the key into this permission table. The value of a table entry is
 * the set of permissions (of type <code>PermissionInfo</code>) granted to
 * the bundle named by the given location. A bundle may have an entry in the
 * permission table prior to being installed in the Framework.
 * 
 * <p>
 * The permissions specified in <code>setDefaultPermissions</code> are used as
 * the default permissions which are granted to all bundles that do not have an
 * entry in the permission table.
 * 
 * <p>
 * Any changes to a bundle's permissions in the permission table will take
 * effect no later than when bundle's
 * <code>java.security.ProtectionDomain</code> is next involved in a
 * permission check, and will be made persistent.
 * 
 * <p>
 * Only permission classes on the system classpath or from an exported package
 * are considered during a permission check. Additionally, only permission
 * classes that are subclasses of <code>java.security.Permission</code> and
 * define a 2-argument constructor that takes a <i>name </i> string and an
 * <i>actions </i> string can be used.
 * <p>
 * Permissions implicitly granted by the Framework (for example, a bundle's
 * permission to access its persistent storage area) cannot be changed, and are
 * not reflected in the permissions returned by <code>getPermissions</code>
 * and <code>getDefaultPermissions</code>.
 * 
 * @ThreadSafe
 * @version $Revision: 5673 $
 */
public interface PermissionAdmin {
	/**
	 * Gets the permissions assigned to the bundle with the specified location.
	 * 
	 * @param location The location of the bundle whose permissions are to be
	 *        returned.
	 * 
	 * @return The permissions assigned to the bundle with the specified
	 *         location, or <code>null</code> if that bundle has not been
	 *         assigned any permissions.
	 */
	PermissionInfo[] getPermissions(String location);

	/**
	 * Assigns the specified permissions to the bundle with the specified
	 * location.
	 * 
	 * @param location The location of the bundle that will be assigned the
	 *        permissions.
	 * @param permissions The permissions to be assigned, or <code>null</code>
	 *        if the specified location is to be removed from the permission
	 *        table.
	 * @throws SecurityException If the caller does not have
	 *         <code>AllPermission</code>.
	 */
	void setPermissions(String location, PermissionInfo[] permissions);

	/**
	 * Returns the bundle locations that have permissions assigned to them, that
	 * is, bundle locations for which an entry exists in the permission table.
	 * 
	 * @return The locations of bundles that have been assigned any permissions,
	 *         or <code>null</code> if the permission table is empty.
	 */
	String[] getLocations();

	/**
	 * Gets the default permissions.
	 * 
	 * <p>
	 * These are the permissions granted to any bundle that does not have
	 * permissions assigned to its location.
	 * 
	 * @return The default permissions, or <code>null</code> if no default
	 *         permissions are set.
	 */
	PermissionInfo[] getDefaultPermissions();

	/**
	 * Sets the default permissions.
	 * 
	 * <p>
	 * These are the permissions granted to any bundle that does not have
	 * permissions assigned to its location.
	 * 
	 * @param permissions The default permissions, or <code>null</code> if the
	 *        default permissions are to be removed from the permission table.
	 * @throws SecurityException If the caller does not have
	 *         <code>AllPermission</code>.
	 */
	void setDefaultPermissions(PermissionInfo[] permissions);
}
