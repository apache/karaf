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
 * A named grouping of roles (<code>Role</code> objects).
 * <p>
 * Whether or not a given <code>Authorization</code> context implies a
 * <code>Group</code> object depends on the members of that <code>Group</code>
 * object.
 * <p>
 * A <code>Group</code> object can have two kinds of members: <i>basic </i> and
 * <i>required </i>. A <code>Group</code> object is implied by an
 * <code>Authorization</code> context if all of its required members are implied
 * and at least one of its basic members is implied.
 * <p>
 * A <code>Group</code> object must contain at least one basic member in order to
 * be implied. In other words, a <code>Group</code> object without any basic
 * member roles is never implied by any <code>Authorization</code> context.
 * <p>
 * A <code>User</code> object always implies itself.
 * <p>
 * No loop detection is performed when adding members to <code>Group</code>
 * objects, which means that it is possible to create circular implications.
 * Loop detection is instead done when roles are checked. The semantics is that
 * if a role depends on itself (i.e., there is an implication loop), the role is
 * not implied.
 * <p>
 * The rule that a <code>Group</code> object must have at least one basic member
 * to be implied is motivated by the following example:
 * 
 * <pre>
 * 
 *  group foo
 *    required members: marketing
 *    basic members: alice, bob
 *  
 * </pre>
 * 
 * Privileged operations that require membership in "foo" can be performed only
 * by "alice" and "bob", who are in marketing.
 * <p>
 * If "alice" and "bob" ever transfer to a different department, anybody in
 * marketing will be able to assume the "foo" role, which certainly must be
 * prevented. Requiring that "foo" (or any <code>Group</code> object for that
 * matter) must have at least one basic member accomplishes that.
 * <p>
 * However, this would make it impossible for a <code>Group</code> object to be
 * implied by just its required members. An example where this implication might
 * be useful is the following declaration: "Any citizen who is an adult is
 * allowed to vote." An intuitive configuration of "voter" would be:
 * 
 * <pre>
 * 
 *  group voter
 *    required members: citizen, adult
 *       basic members:
 *  
 * </pre>
 * 
 * However, according to the above rule, the "voter" role could never be assumed
 * by anybody, since it lacks any basic members. In order to address this issue
 * a predefined role named "user.anyone" can be specified, which is always
 * implied. The desired implication of the "voter" group can then be achieved by
 * specifying "user.anyone" as its basic member, as follows:
 * 
 * <pre>
 * 
 *  group voter
 *    required members: citizen, adult
 *       basic members: user.anyone
 *  
 * </pre>
 * 
 * @version $Revision: 5673 $
 */
public interface Group extends User {
	/**
	 * Adds the specified <code>Role</code> object as a basic member to this
	 * <code>Group</code> object.
	 * 
	 * @param role The role to add as a basic member.
	 * 
	 * @return <code>true</code> if the given role could be added as a basic
	 *         member, and <code>false</code> if this <code>Group</code> object
	 *         already contains a <code>Role</code> object whose name matches that
	 *         of the specified role.
	 * 
	 * @throws SecurityException If a security manager exists and the caller
	 *         does not have the <code>UserAdminPermission</code> with name
	 *         <code>admin</code>.
	 */
	public boolean addMember(Role role);

	/**
	 * Adds the specified <code>Role</code> object as a required member to this
	 * <code>Group</code> object.
	 * 
	 * @param role The <code>Role</code> object to add as a required member.
	 * 
	 * @return <code>true</code> if the given <code>Role</code> object could be
	 *         added as a required member, and <code>false</code> if this
	 *         <code>Group</code> object already contains a <code>Role</code> object
	 *         whose name matches that of the specified role.
	 * 
	 * @throws SecurityException If a security manager exists and the caller
	 *         does not have the <code>UserAdminPermission</code> with name
	 *         <code>admin</code>.
	 */
	public boolean addRequiredMember(Role role);

	/**
	 * Removes the specified <code>Role</code> object from this <code>Group</code>
	 * object.
	 * 
	 * @param role The <code>Role</code> object to remove from this <code>Group</code>
	 *        object.
	 * 
	 * @return <code>true</code> if the <code>Role</code> object could be removed,
	 *         otherwise <code>false</code>.
	 * 
	 * @throws SecurityException If a security manager exists and the caller
	 *         does not have the <code>UserAdminPermission</code> with name
	 *         <code>admin</code>.
	 */
	public boolean removeMember(Role role);

	/**
	 * Gets the basic members of this <code>Group</code> object.
	 * 
	 * @return The basic members of this <code>Group</code> object, or
	 *         <code>null</code> if this <code>Group</code> object does not contain
	 *         any basic members.
	 */
	public Role[] getMembers();

	/**
	 * Gets the required members of this <code>Group</code> object.
	 * 
	 * @return The required members of this <code>Group</code> object, or
	 *         <code>null</code> if this <code>Group</code> object does not contain
	 *         any required members.
	 */
	public Role[] getRequiredMembers();
}
