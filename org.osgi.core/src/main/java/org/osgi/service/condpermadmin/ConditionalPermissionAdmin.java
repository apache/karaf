/*
 * $Header: /cvshome/build/org.osgi.service.condpermadmin/src/org/osgi/service/condpermadmin/ConditionalPermissionAdmin.java,v 1.13 2006/06/16 16:31:37 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2005, 2006). All Rights Reserved.
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

package org.osgi.service.condpermadmin;

import java.security.AccessControlContext;
import java.util.Enumeration;

import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * Framework service to administer Conditional Permissions. Conditional
 * Permissions can be added to, retrieved from, and removed from the framework.
 * 
 * @version $Revision: 1.13 $
 */
public interface ConditionalPermissionAdmin {
	/**
	 * Create a new Conditional Permission Info.
	 * 
	 * The Conditional Permission Info will be given a unique, never reused
	 * name.
	 * 
	 * @param conds The Conditions that need to be satisfied to enable the
	 *        corresponding Permissions.
	 * @param perms The Permissions that are enable when the corresponding
	 *        Conditions are satisfied.
	 * @return The ConditionalPermissionInfo for the specified Conditions and
	 *         Permissions.
	 * @throws SecurityException If the caller does not have
	 *         <code>AllPermission</code>.
	 */
	public ConditionalPermissionInfo addConditionalPermissionInfo(
			ConditionInfo conds[], PermissionInfo perms[]);

	/**
	 * Set or create a Conditional Permission Info with a specified name.
	 * 
	 * If the specified name is <code>null</code>, a new Conditional
	 * Permission Info must be created and will be given a unique, never reused
	 * name. If there is currently no Conditional Permission Info with the
	 * specified name, a new Conditional Permission Info must be created with
	 * the specified name. Otherwise, the Conditional Permission Info with the
	 * specified name must be updated with the specified Conditions and
	 * Permissions.
	 * 
	 * @param name The name of the Conditional Permission Info, or
	 *        <code>null</code>.
	 * @param conds The Conditions that need to be satisfied to enable the
	 *        corresponding Permissions.
	 * @param perms The Permissions that are enable when the corresponding
	 *        Conditions are satisfied.
	 * @return The ConditionalPermissionInfo that for the specified name,
	 *         Conditions and Permissions.
	 * @throws SecurityException If the caller does not have
	 *         <code>AllPermission</code>.
	 */
	public ConditionalPermissionInfo setConditionalPermissionInfo(String name,
			ConditionInfo conds[], PermissionInfo perms[]);

	/**
	 * Returns the Conditional Permission Infos that are currently managed by
	 * Conditional Permission Admin. Calling
	 * {@link ConditionalPermissionInfo#delete()} will remove the Conditional
	 * Permission Info from Conditional Permission Admin.
	 * 
	 * @return An enumeration of the Conditional Permission Infos that are
	 *         currently managed by Conditional Permission Admin.
	 */
	public Enumeration getConditionalPermissionInfos();

	/**
	 * Return the Conditional Permission Info with the specified name.
	 * 
	 * @param name The name of the Conditional Permission Info to be returned.
	 * @return The Conditional Permission Info with the specified name.
	 */
	public ConditionalPermissionInfo getConditionalPermissionInfo(String name);

	/**
	 * Returns the Access Control Context that corresponds to the specified
	 * signers.
	 * 
	 * @param signers The signers for which to return an Access Control Context.
	 * @return An <code>AccessControlContext</code> that has the Permissions
	 *         associated with the signer.
	 */
	public AccessControlContext getAccessControlContext(String[] signers);
}
