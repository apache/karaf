/*
 * Copyright (c) OSGi Alliance (2001, 2009). All Rights Reserved.
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

import java.io.IOException;
import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Permission to configure and access the {@link Role} objects managed by a User
 * Admin service.
 * 
 * <p>
 * This class represents access to the <code>Role</code> objects managed by a
 * User Admin service and their properties and credentials (in the case of
 * {@link User} objects).
 * <p>
 * The permission name is the name (or name prefix) of a property or credential.
 * The naming convention follows the hierarchical property naming convention.
 * Also, an asterisk may appear at the end of the name, following a
 * &quot;.&quot;, or by itself, to signify a wildcard match. For example:
 * &quot;org.osgi.security.protocol.*&quot; or &quot;*&quot; is valid, but
 * &quot;*protocol&quot; or &quot;a*b&quot; are not valid.
 * 
 * <p>
 * The <code>UserAdminPermission</code> with the reserved name &quot;admin&quot;
 * represents the permission required for creating and removing
 * <code>Role</code> objects in the User Admin service, as well as adding and
 * removing members in a <code>Group</code> object. This
 * <code>UserAdminPermission</code> does not have any actions associated with
 * it.
 * 
 * <p>
 * The actions to be granted are passed to the constructor in a string
 * containing a list of one or more comma-separated keywords. The possible
 * keywords are: <code>changeProperty</code>,<code>changeCredential</code>, and
 * <code>getCredential</code>. Their meaning is defined as follows:
 * 
 * <pre>
 * 
 *  action
 *  changeProperty    Permission to change (i.e., add and remove)
 *                    Role object properties whose names start with
 *                    the name argument specified in the constructor.
 *  changeCredential  Permission to change (i.e., add and remove)
 *                    User object credentials whose names start
 *                    with the name argument specified in the constructor.
 *  getCredential     Permission to retrieve and check for the
 *                    existence of User object credentials whose names
 *                    start with the name argument specified in the
 *                    constructor.
 * 
 * </pre>
 * 
 * The action string is converted to lowercase before processing.
 * 
 * <p>
 * Following is a PermissionInfo style policy entry which grants a user
 * administration bundle a number of <code>UserAdminPermission</code> object:
 * 
 * <pre>
 * 
 *  (org.osgi.service.useradmin.UserAdminPermission &quot;admin&quot;)
 *  (org.osgi.service.useradmin.UserAdminPermission &quot;com.foo.*&quot; &quot;changeProperty,getCredential,changeCredential&quot;)
 *  (org.osgi.service.useradmin.UserAdminPermission &quot;user.*&quot;, &quot;changeProperty,changeCredential&quot;)
 * 
 * </pre>
 * 
 * The first permission statement grants the bundle the permission to perform
 * any User Admin service operations of type "admin", that is, create and remove
 * roles and configure <code>Group</code> objects.
 * 
 * <p>
 * The second permission statement grants the bundle the permission to change
 * any properties as well as get and change any credentials whose names start
 * with <code>com.foo.</code>.
 * 
 * <p>
 * The third permission statement grants the bundle the permission to change any
 * properties and credentials whose names start with <code>user.</code>. This
 * means that the bundle is allowed to change, but not retrieve any credentials
 * with the given prefix.
 * 
 * <p>
 * The following policy entry empowers the Http Service bundle to perform user
 * authentication:
 * 
 * <pre>
 * 
 *  grant codeBase &quot;${jars}http.jar&quot; {
 *    permission org.osgi.service.useradmin.UserAdminPermission
 *      &quot;user.password&quot;, &quot;getCredential&quot;;
 *  };
 * 
 * </pre>
 * 
 * <p>
 * The permission statement grants the Http Service bundle the permission to
 * validate any password credentials (for authentication purposes), but the
 * bundle is not allowed to change any properties or credentials.
 * 
 * @ThreadSafe
 * @version $Revision: 6381 $
 */
public final class UserAdminPermission extends BasicPermission {
	static final long			serialVersionUID			= -1179971692401603789L;
	/**
	 * The permission name &quot;admin&quot;.
	 */
	public static final String	ADMIN						= "admin";
	/**
	 * The action string &quot;changeProperty&quot;.
	 */
	public static final String	CHANGE_PROPERTY				= "changeProperty";
	private static final int	ACTION_CHANGE_PROPERTY		= 0x1;
	/**
	 * The action string &quot;changeCredential&quot;.
	 */
	public static final String	CHANGE_CREDENTIAL			= "changeCredential";
	private static final int	ACTION_CHANGE_CREDENTIAL	= 0x2;
	/**
	 * The action string &quot;getCredential&quot;.
	 */
	public static final String	GET_CREDENTIAL				= "getCredential";
	private static final int	ACTION_GET_CREDENTIAL		= 0x4;
	/**
	 * All actions
	 */
	private static final int	ACTION_ALL					= ACTION_CHANGE_PROPERTY
																	| ACTION_CHANGE_CREDENTIAL
																	| ACTION_GET_CREDENTIAL;
	/**
	 * No actions.
	 */
	static final int			ACTION_NONE					= 0;
	/**
	 * The actions in canonical form.
	 * 
	 * @serial
	 */
	private volatile String		actions						= null;
	/**
	 * The actions mask.
	 */
	private transient int		action_mask;

	/**
	 * Creates a new <code>UserAdminPermission</code> with the specified name
	 * and actions. <code>name</code> is either the reserved string
	 * &quot;admin&quot; or the name of a credential or property, and
	 * <code>actions</code> contains a comma-separated list of the actions
	 * granted on the specified name. Valid actions are
	 * <code>changeProperty</code>,<code>changeCredential</code>, and
	 * getCredential.
	 * 
	 * @param name the name of this <code>UserAdminPermission</code>
	 * @param actions the action string.
	 * 
	 * @throws IllegalArgumentException If <code>name</code> equals
	 *         &quot;admin&quot; and <code>actions</code> are specified.
	 */
	public UserAdminPermission(String name, String actions) {
		this(name, parseActions(actions));
	}

	/**
	 * Package private constructor used by
	 * <code>UserAdminPermissionCollection</code>.
	 * 
	 * @param name class name
	 * @param mask action mask
	 */
	UserAdminPermission(String name, int mask) {
		super(name);
		setTransients(mask);
	}

	/**
	 * Called by constructors and when deserialized.
	 * 
	 * @param mask action mask
	 */
	private synchronized void setTransients(int mask) {
		if (getName().equals(ADMIN)) {
			if (mask != ACTION_NONE) {
				throw new IllegalArgumentException("Actions specified for "
						+ "no-action " + "UserAdminPermission");
			}
		}
		else {
			if ((mask == ACTION_NONE) || ((mask & ACTION_ALL) != mask)) {
				throw new IllegalArgumentException("Invalid action string");
			}
		}
		action_mask = mask;
	}

	/**
	 * Returns the current action mask.
	 * <p>
	 * Used by the UserAdminPermissionCollection class.
	 * 
	 * @return Current action mask.
	 */
	synchronized int getActionsMask() {
		return action_mask;
	}

	/**
	 * Parse action string into action mask.
	 * 
	 * @param actions Action string.
	 * @return action mask.
	 */
	private static int parseActions(String actions) {
		boolean seencomma = false;
		int mask = ACTION_NONE;
		if (actions == null) {
			return mask;
		}
		char[] a = actions.toCharArray();
		int i = a.length - 1;
		if (i < 0)
			return mask;
		while (i != -1) {
			char c;
			// skip whitespace
			while ((i != -1)
					&& ((c = a[i]) == ' ' || c == '\r' || c == '\n'
							|| c == '\f' || c == '\t'))
				i--;
			// check for the known strings
			int matchlen;
			if (i >= 12 && match_get(a, i - 10) && match_credential(a, i)) {
				matchlen = 13;
				mask |= ACTION_GET_CREDENTIAL;
			}
			else
				if (i >= 13 && match_change(a, i - 8) && match_property(a, i)) {
					matchlen = 14;
					mask |= ACTION_CHANGE_PROPERTY;
				}
				else
					if (i >= 15 && match_change(a, i - 10)
							&& match_credential(a, i)) {
						matchlen = 16;
						mask |= ACTION_CHANGE_CREDENTIAL;
					}
					else {
						// parse error
						throw new IllegalArgumentException(
								"invalid permission: " + actions);
					}
			// make sure we didn't just match the tail of a word
			// like "ackbarfimport". Also, skip to the comma.
			seencomma = false;
			while (i >= matchlen && !seencomma) {
				switch (a[i - matchlen]) {
					case ',' :
						seencomma = true;
						/* FALLTHROUGH */
					case ' ' :
					case '\r' :
					case '\n' :
					case '\f' :
					case '\t' :
						break;
					default :
						throw new IllegalArgumentException(
								"invalid permission: " + actions);
				}
				i--;
			}
			// point i at the location of the comma minus one (or -1).
			i -= matchlen;
		}
		if (seencomma) {
			throw new IllegalArgumentException("invalid permission: " + actions);
		}
		return mask;
	}

	private static boolean match_change(char[] a, int i) {
		return ((a[i - 5] == 'c' || a[i - 5] == 'C')
				&& (a[i - 4] == 'h' || a[i - 4] == 'H')
				&& (a[i - 3] == 'a' || a[i - 3] == 'A')
				&& (a[i - 2] == 'n' || a[i - 2] == 'N')
				&& (a[i - 1] == 'g' || a[i - 1] == 'G') && (a[i - 0] == 'e' || a[i - 0] == 'E'));
	}

	private static boolean match_get(char[] a, int i) {
		return ((a[i - 2] == 'g' || a[i - 2] == 'G')
				&& (a[i - 1] == 'e' || a[i - 1] == 'E') && (a[i - 0] == 't' || a[i - 0] == 'T'));
	}

	private static boolean match_property(char[] a, int i) {
		return ((a[i - 7] == 'p' || a[i - 7] == 'P')
				&& (a[i - 6] == 'r' || a[i - 6] == 'R')
				&& (a[i - 5] == 'o' || a[i - 5] == 'O')
				&& (a[i - 4] == 'p' || a[i - 4] == 'P')
				&& (a[i - 3] == 'e' || a[i - 3] == 'E')
				&& (a[i - 2] == 'r' || a[i - 2] == 'R')
				&& (a[i - 1] == 't' || a[i - 1] == 'T') && (a[i - 0] == 'y' || a[i - 0] == 'Y'));
	}

	private static boolean match_credential(char[] a, int i) {
		return ((a[i - 9] == 'c' || a[i - 9] == 'C')
				&& (a[i - 8] == 'r' || a[i - 8] == 'R')
				&& (a[i - 7] == 'e' || a[i - 7] == 'E')
				&& (a[i - 6] == 'd' || a[i - 6] == 'D')
				&& (a[i - 5] == 'e' || a[i - 5] == 'E')
				&& (a[i - 4] == 'n' || a[i - 4] == 'N')
				&& (a[i - 3] == 't' || a[i - 3] == 'T')
				&& (a[i - 2] == 'i' || a[i - 2] == 'I')
				&& (a[i - 1] == 'a' || a[i - 1] == 'A') && (a[i - 0] == 'l' || a[i - 0] == 'L'));
	}

	/**
	 * Checks if this <code>UserAdminPermission</code> object
	 * &quot;implies&quot; the specified permission.
	 * <P>
	 * More specifically, this method returns <code>true</code> if:
	 * <p>
	 * <ul>
	 * <li><i>p </i> is an instanceof <code>UserAdminPermission</code>,
	 * <li><i>p </i>'s actions are a proper subset of this object's actions, and
	 * <li><i>p </i>'s name is implied by this object's name. For example,
	 * &quot;java.*&quot; implies &quot;java.home&quot;.
	 * </ul>
	 * 
	 * @param p the permission to check against.
	 * 
	 * @return <code>true</code> if the specified permission is implied by this
	 *         object; <code>false</code> otherwise.
	 */
	public boolean implies(Permission p) {
		if (p instanceof UserAdminPermission) {
			UserAdminPermission requested = (UserAdminPermission) p;
			int mask = getActionsMask();
			int targetMask = requested.getActionsMask();
			return // Check that the we have the requested action
			((targetMask & mask) == targetMask) &&
			// If the target action mask is ACTION_NONE, it must be an
					// admin permission, and then we must be that too
					(targetMask != ACTION_NONE || mask == ACTION_NONE) &&
					// Check that name name matches
					super.implies(p);
		}
		return false;
	}

	/**
	 * Returns the canonical string representation of the actions, separated by
	 * comma.
	 * 
	 * @return the canonical string representation of the actions.
	 */
	public String getActions() {
		String result = actions;
		if (result == null) {
			StringBuffer sb = new StringBuffer();
			boolean comma = false;
			int mask = getActionsMask();
			if ((mask & ACTION_CHANGE_CREDENTIAL) == ACTION_CHANGE_CREDENTIAL) {
				sb.append(CHANGE_CREDENTIAL);
				comma = true;
			}
			if ((mask & ACTION_CHANGE_PROPERTY) == ACTION_CHANGE_PROPERTY) {
				if (comma)
					sb.append(',');
				sb.append(CHANGE_PROPERTY);
				comma = true;
			}
			if ((mask & ACTION_GET_CREDENTIAL) == ACTION_GET_CREDENTIAL) {
				if (comma)
					sb.append(',');
				sb.append(GET_CREDENTIAL);
			}
			actions = result = sb.toString();
		}
		return result;
	}

	/**
	 * Returns a new <code>PermissionCollection</code> object for storing
	 * <code>UserAdminPermission</code> objects.
	 * 
	 * @return a new <code>PermissionCollection</code> object suitable for
	 *         storing <code>UserAdminPermission</code> objects.
	 */
	public PermissionCollection newPermissionCollection() {
		return new UserAdminPermissionCollection();
	}

	/**
	 * Checks two <code>UserAdminPermission</code> objects for equality. Checks
	 * that <code>obj</code> is a <code>UserAdminPermission</code>, and has the
	 * same name and actions as this object.
	 * 
	 * @param obj the object to be compared for equality with this object.
	 * 
	 * @return <code>true</code> if <code>obj</code> is a
	 *         <code>UserAdminPermission</code> object, and has the same name
	 *         and actions as this <code>UserAdminPermission</code> object.
	 */
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof UserAdminPermission)) {
			return false;
		}

		UserAdminPermission uap = (UserAdminPermission) obj;

		return (getActionsMask() == uap.getActionsMask())
				&& getName().equals(uap.getName());
	}

	/**
	 * Returns the hash code value for this object.
	 * 
	 * @return A hash code value for this object.
	 */
	public int hashCode() {
		int h = 31 * 17 + getName().hashCode();
		h = 31 * h + getActions().hashCode();
		return h;
	}

	/**
	 * writeObject is called to save the state of this object to a stream. The
	 * actions are serialized, and the superclass takes care of the name.
	 */
	private synchronized void writeObject(java.io.ObjectOutputStream s)
			throws IOException {
		// Write out the actions. The superclass takes care of the name
		// call getActions to make sure actions field is initialized
		if (actions == null)
			getActions();
		s.defaultWriteObject();
	}

	/*
	 * Restores this object from a stream (i.e., deserializes it).
	 */
	private synchronized void readObject(java.io.ObjectInputStream s)
			throws IOException, ClassNotFoundException {
		// Read in the action, then initialize the rest
		s.defaultReadObject();
		setTransients(parseActions(actions));
	}

	/**
	 * Returns a string describing this <code>UserAdminPermission</code> object.
	 * This string must be in <code>PermissionInfo</code> encoded format.
	 * 
	 * @return The <code>PermissionInfo</code> encoded string for this
	 *         <code>UserAdminPermission</code> object.
	 * @see "<code>org.osgi.service.permissionadmin.PermissionInfo.getEncoded</code>"
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append('(');
		sb.append(getClass().getName());
		sb.append(" \"");
		sb.append(getName());
		String a = getActions();
		if (a.length() > 0) {
			sb.append("\" \"");
			sb.append(a);
		}
		sb.append("\")");
		return sb.toString();
	}
}

/**
 * A <code>UserAdminPermissionCollection</code> stores a set of
 * <code>UserAdminPermission</code> permissions.
 */

final class UserAdminPermissionCollection extends PermissionCollection {
	static final long		serialVersionUID	= -7222111885230120581L;
	/**
	 * Table of permissions.
	 * 
	 * @serial
	 * @GuardedBy this
	 */
	private final Hashtable	permissions;
	/**
	 * Boolean saying if "*" is in the collection.
	 * 
	 * @serial
	 * @GuardedBy this
	 */
	private boolean			all_allowed;

	/**
	 * Creates an empty <code>UserAdminPermissionCollection</code> object.
	 */
	public UserAdminPermissionCollection() {
		permissions = new Hashtable();
		all_allowed = false;
	}

	/**
	 * Adds the given permission to this
	 * <code>UserAdminPermissionCollection</code>. The key for the hash is the
	 * name.
	 * 
	 * @param permission the <code>Permission</code> object to add.
	 * 
	 * @throws IllegalArgumentException If the given permission is not a
	 *         <code>UserAdminPermission</code>
	 * @throws SecurityException If this
	 *         <code>UserAdminPermissionCollection</code> object has been marked
	 *         readonly
	 */
	public void add(Permission permission) {
		if (!(permission instanceof UserAdminPermission))
			throw new IllegalArgumentException("Invalid permission: "
					+ permission);
		if (isReadOnly()) {
			throw new SecurityException("Attempt to add a Permission to a "
					+ "readonly PermissionCollection");
		}
		final UserAdminPermission uap = (UserAdminPermission) permission;
		final String name = uap.getName();
		synchronized (this) {
			final UserAdminPermission existing = (UserAdminPermission) permissions
					.get(name);
			if (existing != null) {
				int oldMask = existing.getActionsMask();
				int newMask = uap.getActionsMask();
				if (oldMask != newMask) {
					permissions.put(name, new UserAdminPermission(name, oldMask
							| newMask));
				}
			}
			else {
				permissions.put(name, uap);
			}
			if (!all_allowed) {
				if (name.equals("*")) {
					all_allowed = true;
				}
			}
		}
	}

	/**
	 * Checks to see if this <code>PermissionCollection</code> implies the given
	 * permission.
	 * 
	 * @param permission the <code>Permission</code> object to check against
	 * 
	 * @return true if the given permission is implied by this
	 *         <code>PermissionCollection</code>, false otherwise.
	 */
	public boolean implies(Permission permission) {
		if (!(permission instanceof UserAdminPermission)) {
			return false;
		}
		final UserAdminPermission requested = (UserAdminPermission) permission;
		String name = requested.getName();
		final int desired = requested.getActionsMask();
		UserAdminPermission x;
		int effective = 0;
		synchronized (this) {
			// Short circuit if the "*" Permission was added.
			// desired can only be ACTION_NONE when name is "admin".
			if (all_allowed && (desired != UserAdminPermission.ACTION_NONE)) {
				x = (UserAdminPermission) permissions.get("*");
				if (x != null) {
					effective |= x.getActionsMask();
					if ((effective & desired) == desired) {
						return true;
					}
				}
			}
			// strategy:
			// Check for full match first. Then work our way up the
			// name looking for matches on a.b.*

			x = (UserAdminPermission) permissions.get(name);
		}
		if (x != null) {
			// we have a direct hit!
			effective |= x.getActionsMask();
			if ((effective & desired) == desired) {
				return true;
			}
		}
		// work our way up the tree...
		int last;
		int offset = name.length() - 1;
		while ((last = name.lastIndexOf(".", offset)) != -1) {
			name = name.substring(0, last + 1) + "*";
			synchronized (this) {
				x = (UserAdminPermission) permissions.get(name);
			}
			if (x != null) {
				effective |= x.getActionsMask();
				if ((effective & desired) == desired) {
					return true;
				}
			}
			offset = last - 1;
		}
		// we don't have to check for "*" as it was already checked
		// at the top (all_allowed), so we just return false
		return false;
	}

	/**
	 * Returns an enumeration of all the <code>UserAdminPermission</code>
	 * objects in the container.
	 * 
	 * @return an enumeration of all the <code>UserAdminPermission</code>
	 *         objects.
	 */
	public Enumeration elements() {
		return permissions.elements();
	}
}
