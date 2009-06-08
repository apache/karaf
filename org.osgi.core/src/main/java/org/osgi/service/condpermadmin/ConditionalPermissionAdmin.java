/*
 * Copyright (c) OSGi Alliance (2005, 2009). All Rights Reserved.
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
 * Conditional Permissions are conceptually managed in an ordered table called
 * the Conditional Permission Table.
 * 
 * @ThreadSafe
 * @version $Revision: 6782 $
 */
public interface ConditionalPermissionAdmin {
	/**
	 * Create a new Conditional Permission Info in the Conditional Permission
	 * Table.
	 * <p>
	 * The Conditional Permission Info will be given a unique, never reused
	 * name. This entry will be added at the beginning of the Conditional
	 * Permission Table with an access decision of
	 * {@link ConditionalPermissionInfo#ALLOW ALLOW}.
	 * <p>
	 * Since this method changes the Conditional Permission Table any
	 * {@link ConditionalPermissionUpdate}s that were created prior to calling
	 * this method can no longer be committed.
	 * 
	 * @param conditions The conditions that need to be satisfied to enable the
	 *        specified permissions. This argument can be <code>null</code> or
	 *        an empty array indicating the specified permissions are not
	 *        guarded by any conditions.
	 * @param permissions The permissions that are enabled when the specified
	 *        conditions, if any, are satisfied. This argument must not be
	 *        <code>null</code> and must specify at least one permission.
	 * @return The ConditionalPermissionInfo for the specified Conditions and
	 *         Permissions.
	 * @throws IllegalArgumentException If no permissions are specified.
	 * @throws SecurityException If the caller does not have
	 *         <code>AllPermission</code>.
	 * @deprecated Since 1.1. Use {@link #newConditionalPermissionUpdate()}
	 *             instead.
	 */
	ConditionalPermissionInfo addConditionalPermissionInfo(
			ConditionInfo conditions[], PermissionInfo permissions[]);

	/**
	 * Set or create a Conditional Permission Info with a specified name in the
	 * Conditional Permission Table.
	 * <p>
	 * If the specified name is <code>null</code>, a new Conditional Permission
	 * Info must be created and will be given a unique, never reused name. If
	 * there is currently no Conditional Permission Info with the specified
	 * name, a new Conditional Permission Info must be created with the
	 * specified name. Otherwise, the Conditional Permission Info with the
	 * specified name must be updated with the specified Conditions and
	 * Permissions. If a new entry was created in the Conditional Permission
	 * Table it will be added at the beginning of the table with an access
	 * decision of {@link ConditionalPermissionInfo#ALLOW ALLOW}.
	 * <p>
	 * Since this method changes the underlying permission table any
	 * {@link ConditionalPermissionUpdate}s that were created prior to calling
	 * this method can no longer be committed.
	 * 
	 * @param name The name of the Conditional Permission Info, or
	 *        <code>null</code>.
	 * @param conditions The conditions that need to be satisfied to enable the
	 *        specified permissions. This argument can be <code>null</code> or
	 *        an empty array indicating the specified permissions are not
	 *        guarded by any conditions.
	 * @param permissions The permissions that are enabled when the specified
	 *        conditions, if any, are satisfied. This argument must not be
	 *        <code>null</code> and must specify at least one permission.
	 * @return The ConditionalPermissionInfo for the specified name, Conditions
	 *         and Permissions.
	 * @throws IllegalArgumentException If no permissions are specified.
	 * @throws SecurityException If the caller does not have
	 *         <code>AllPermission</code>.
	 * @deprecated Since 1.1. Use {@link #newConditionalPermissionUpdate()}
	 *             instead.
	 */
	ConditionalPermissionInfo setConditionalPermissionInfo(String name,
			ConditionInfo conditions[], PermissionInfo permissions[]);

	/**
	 * Returns the Conditional Permission Infos from the Conditional Permission
	 * Table.
	 * <p>
	 * The returned Enumeration will return elements in the order they are kept
	 * in the Conditional Permission Table.
	 * <p>
	 * The Enumeration returned is based on a copy of the Conditional Permission
	 * Table and therefore will not throw exceptions if the Conditional
	 * Permission Table is changed during the course of reading elements from
	 * the Enumeration.
	 * 
	 * @return An enumeration of the Conditional Permission Infos that are
	 *         currently in the Conditional Permission Table.
	 * @deprecated Since 1.1. Use {@link #newConditionalPermissionUpdate()}
	 *             instead.
	 */
	Enumeration/* <ConditionalPermissionInfo> */getConditionalPermissionInfos();

	/**
	 * Return the Conditional Permission Info with the specified name.
	 * 
	 * @param name The name of the Conditional Permission Info to be returned.
	 * @return The Conditional Permission Info with the specified name or
	 *         <code>null</code> if no Conditional Permission Info with the
	 *         specified name exists in the Conditional Permission Table.
	 * @deprecated Since 1.1. Use {@link #newConditionalPermissionUpdate()}
	 *             instead.
	 */
	ConditionalPermissionInfo getConditionalPermissionInfo(String name);

	/**
	 * Returns the Access Control Context that corresponds to the specified
	 * signers.
	 * 
	 * The returned Access Control Context must act as if its protection domain
	 * came from a bundle that has the following characteristics:
	 * <ul>
	 * <li>It is signed by all of the given signers</li>
	 * <li>It has a bundle id of -1</li>
	 * <li>Its location is the empty string</li>
	 * <li>Its state is UNINSTALLED</li>
	 * <li>It has no headers</li>
	 * <li>It has the empty version (0.0.0)</li>
	 * <li>Its last modified time=0</li>
	 * <li>Many methods will throw <code>IllegalStateException</code> because the state is UNINSTALLED</li>
	 * <li>All other methods return a <code>null</code></li>
	 * </ul> 
	 * @param signers The signers for which to return an Access Control Context.
	 * @return An <code>AccessControlContext</code> that has the Permissions
	 *         associated with the signer.
	 */
	AccessControlContext getAccessControlContext(String[] signers);

	/**
	 * Creates a new update for the Conditional Permission Table. The update is
	 * a working copy of the current Conditional Permission Table. If the
	 * running Conditional Permission Table is modified before commit is called
	 * on the returned update, then the call to commit on the returned update
	 * will fail. That is, the commit method will return false and no change
	 * will be made to the running Conditional Permission Table. There is no
	 * requirement that commit is eventually called on the returned update.
	 * 
	 * @return A new update for the Conditional Permission Table.
	 * @since 1.1
	 */
	ConditionalPermissionUpdate newConditionalPermissionUpdate();

	/**
	 * Creates a new ConditionalPermissionInfo with the specified fields
	 * suitable for insertion into a {@link ConditionalPermissionUpdate}. The
	 * <code>delete</code> method on <code>ConditionalPermissionInfo</code>
	 * objects created with this method must throw
	 * UnsupportedOperationException.
	 * 
	 * @param name The name of the created
	 *        <code>ConditionalPermissionInfo</code> or <code>null</code> to
	 *        have a unique name generated when the returned
	 *        <code>ConditionalPermissionInfo</code> is committed in an update
	 *        to the Conditional Permission Table.
	 * @param conditions The conditions that need to be satisfied to enable the
	 *        specified permissions. This argument can be <code>null</code> or
	 *        an empty array indicating the specified permissions are not
	 *        guarded by any conditions.
	 * @param permissions The permissions that are enabled when the specified
	 *        conditions, if any, are satisfied. This argument must not be
	 *        <code>null</code> and must specify at least one permission.
	 * @param access Access decision. Must be one of the following values:
	 *        <ul>
	 *        <li>{@link ConditionalPermissionInfo#ALLOW allow}</li>
	 *        <li>{@link ConditionalPermissionInfo#DENY deny}</li>
	 *        </ul>
	 *        The specified access decision value must be evaluated case
	 *        insensitively.
	 * @return A <code>ConditionalPermissionInfo</code> object suitable for
	 *         insertion into a {@link ConditionalPermissionUpdate}.
	 * @throws IllegalArgumentException If no permissions are specified or if
	 *         the specified access decision is not a valid value.
	 * @since 1.1
	 */
	ConditionalPermissionInfo newConditionalPermissionInfo(String name,
			ConditionInfo conditions[], PermissionInfo permissions[],
			String access);

	/**
	 * Creates a new <code>ConditionalPermissionInfo</code> from the specified
	 * encoded <code>ConditionalPermissionInfo</code> string suitable for
	 * insertion into a {@link ConditionalPermissionUpdate}. The
	 * <code>delete</code> method on <code>ConditionalPermissionInfo</code>
	 * objects created with this method must throw
	 * UnsupportedOperationException.
	 * 
	 * @param encodedConditionalPermissionInfo The encoded
	 *        <code>ConditionalPermissionInfo</code>. White space in the encoded
	 *        <code>ConditionalPermissionInfo</code> is ignored. The access
	 *        decision value in the encoded
	 *        <code>ConditionalPermissionInfo</code> must be evaluated case
	 *        insensitively. If the encoded
	 *        <code>ConditionalPermissionInfo</code> does not contain the
	 *        optional name, <code>null</code> must be used for the name and a
	 *        unique name will be generated when the returned
	 *        <code>ConditionalPermissionInfo</code> is committed in an update
	 *        to the Conditional Permission Table.
	 * @return A <code>ConditionalPermissionInfo</code> object suitable for
	 *         insertion into a {@link ConditionalPermissionUpdate}.
	 * @throws IllegalArgumentException If the specified
	 *         <code>encodedConditionalPermissionInfo</code> is not properly
	 *         formatted.
	 * @see ConditionalPermissionInfo#getEncoded
	 * @since 1.1
	 */
	ConditionalPermissionInfo newConditionalPermissionInfo(
			String encodedConditionalPermissionInfo);
}
