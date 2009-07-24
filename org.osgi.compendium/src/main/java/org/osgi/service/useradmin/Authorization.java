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
 * The <code>Authorization</code> interface encapsulates an authorization context
 * on which bundles can base authorization decisions, where appropriate.
 * <p>
 * Bundles associate the privilege to access restricted resources or operations
 * with roles. Before granting access to a restricted resource or operation, a
 * bundle will check if the <code>Authorization</code> object passed to it possess
 * the required role, by calling its <code>hasRole</code> method.
 * <p>
 * Authorization contexts are instantiated by calling the
 * {@link UserAdmin#getAuthorization} method.
 * 
 * <p>
 * <i>Trusting Authorization objects </i>
 * <p>
 * There are no restrictions regarding the creation of <code>Authorization</code>
 * objects. Hence, a service must only accept <code>Authorization</code> objects
 * from bundles that has been authorized to use the service using code based (or
 * Java 2) permissions.
 * 
 * <p>
 * In some cases it is useful to use <code>ServicePermission</code> to do the code
 * based access control. A service basing user access control on
 * <code>Authorization</code> objects passed to it, will then require that a
 * calling bundle has the <code>ServicePermission</code> to get the service in
 * question. This is the most convenient way. The OSGi environment will do the
 * code based permission check when the calling bundle attempts to get the
 * service from the service registry.
 * <p>
 * Example: A servlet using a service on a user's behalf. The bundle with the
 * servlet must be given the <code>ServicePermission</code> to get the Http
 * Service.
 * <p>
 * However, in some cases the code based permission checks need to be more
 * fine-grained. A service might allow all bundles to get it, but require
 * certain code based permissions for some of its methods.
 * <p>
 * Example: A servlet using a service on a user's behalf, where some service
 * functionality is open to anyone, and some is restricted by code based
 * permissions. When a restricted method is called (e.g., one handing over an
 * <code>Authorization</code> object), the service explicitly checks that the
 * calling bundle has permission to make the call.
 * 
 * @version $Revision: 5673 $
 */
public interface Authorization {
	/**
	 * Gets the name of the {@link User} that this <code>Authorization</code>
	 * context was created for.
	 * 
	 * @return The name of the {@link User} object that this
	 *         <code>Authorization</code> context was created for, or
	 *         <code>null</code> if no user was specified when this
	 *         <code>Authorization</code> context was created.
	 */
	public String getName();

	/**
	 * Checks if the role with the specified name is implied by this
	 * <code>Authorization</code> context.
	 * <p>
	 * 
	 * Bundles must define globally unique role names that are associated with
	 * the privilege of accessing restricted resources or operations. Operators
	 * will grant users access to these resources, by creating a {@link Group}
	 * object for each role and adding {@link User} objects to it.
	 * 
	 * @param name The name of the role to check for.
	 * 
	 * @return <code>true</code> if this <code>Authorization</code> context implies
	 *         the specified role, otherwise <code>false</code>.
	 */
	public boolean hasRole(String name);

	/**
	 * Gets the names of all roles implied by this <code>Authorization</code>
	 * context.
	 * 
	 * @return The names of all roles implied by this
	 *         <code>Authorization</code> context, or <code>null</code> if no roles
	 *         are in the context. The predefined role <code>user.anyone</code>
	 *         will not be included in this list.
	 */
	public String[] getRoles();
}
