/*
 * Copyright (c) OSGi Alliance (2002, 2009). All Rights Reserved.
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
package org.osgi.service.wireadmin;

import java.io.IOException;
import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Permission for the scope of a <code>Wire</code> object. When a
 * <code>Envelope</code> object is used for communication with the
 * <code>poll</code> or <code>update</code> method, and the scope is set, then
 * the <code>Wire</code> object must verify that the Consumer service has
 * <code>WirePermission[name,CONSUME]</code> and the Producer service has
 * <code>WirePermission[name,PRODUCE]</code> for all names in the scope.
 * <p>
 * The names are compared with the normal rules for permission names. This means
 * that they may end with a "*" to indicate wildcards. E.g. Door.* indicates all
 * scope names starting with the string "Door". The last period is required due
 * to the implementations of the <code>BasicPermission</code> class.
 * 
 * @ThreadSafe
 * @version $Revision: 6381 $
 */
final public class WirePermission extends BasicPermission {
	static final long			serialVersionUID	= -5583709391516569321L;
	/**
	 * The action string for the <code>produce</code> action.
	 */
	public static final String	PRODUCE				= "produce";
	/**
	 * The action string for the <code>consume</code> action.
	 */
	public static final String	CONSUME				= "consume";
	private final static int	ACTION_PRODUCE		= 0x00000001;
	private final static int	ACTION_CONSUME		= 0x00000002;
	private final static int	ACTION_ALL			= ACTION_PRODUCE
															| ACTION_CONSUME;
	private final static int	ACTION_NONE			= 0;
	/**
	 * The actions mask.
	 */
	private transient int		action_mask;
	/**
	 * The actions in canonical form.
	 * 
	 * @serial
	 */
	private volatile String		actions				= null;

	/**
	 * Create a new WirePermission with the given name (may be wildcard) and
	 * actions.
	 * 
	 * @param name Wire name.
	 * @param actions <code>produce</code>, <code>consume</code> (canonical
	 *        order).
	 */
	public WirePermission(String name, String actions) {
		this(name, parseActions(actions));
	}

	/**
	 * Package private constructor used by WirePermissionCollection.
	 * 
	 * @param name class name
	 * @param mask action mask
	 */
	WirePermission(String name, int mask) {
		super(name);
		setTransients(mask);
	}

	/**
	 * Called by constructors and when deserialized.
	 * 
	 * @param mask action mask
	 */
	private synchronized void setTransients(int mask) {
		if ((mask == ACTION_NONE) || ((mask & ACTION_ALL) != mask)) {
			throw new IllegalArgumentException("invalid action string");
		}
		action_mask = mask;
	}

	/**
	 * Returns the current action mask. Used by the WirePermissionCollection
	 * object.
	 * 
	 * @return The actions mask.
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
			if (i >= 6 && (a[i - 6] == 'p' || a[i - 6] == 'P')
					&& (a[i - 5] == 'r' || a[i - 5] == 'R')
					&& (a[i - 4] == 'o' || a[i - 4] == 'O')
					&& (a[i - 3] == 'd' || a[i - 3] == 'D')
					&& (a[i - 2] == 'u' || a[i - 2] == 'U')
					&& (a[i - 1] == 'c' || a[i - 1] == 'C')
					&& (a[i] == 'e' || a[i] == 'E')) {
				matchlen = 7;
				mask |= ACTION_PRODUCE;
			}
			else
				if (i >= 6 && (a[i - 6] == 'c' || a[i - 6] == 'C')
						&& (a[i - 5] == 'o' || a[i - 5] == 'O')
						&& (a[i - 4] == 'n' || a[i - 4] == 'N')
						&& (a[i - 3] == 's' || a[i - 3] == 'S')
						&& (a[i - 2] == 'u' || a[i - 2] == 'U')
						&& (a[i - 1] == 'm' || a[i - 1] == 'M')
						&& (a[i] == 'e' || a[i] == 'E')) {
					matchlen = 7;
					mask |= ACTION_CONSUME;
				}
				else {
					// parse error
					throw new IllegalArgumentException("invalid permission: "
							+ actions);
				}
			// make sure we didn't just match the tail of a word
			// like "ackbarfregister". Also, skip to the comma.
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

	/**
	 * Checks if this <code>WirePermission</code> object <code>implies</code>
	 * the specified permission.
	 * <P>
	 * More specifically, this method returns <code>true</code> if:
	 * <p>
	 * <ul>
	 * <li><i>p </i> is an instanceof the <code>WirePermission</code> class,
	 * <li><i>p </i>'s actions are a proper subset of this object's actions, and
	 * <li><i>p </i>'s name is implied by this object's name. For example,
	 * <code>java.*</code> implies <code>java.home</code>.
	 * </ul>
	 * 
	 * @param p The permission to check against.
	 * 
	 * @return <code>true</code> if the specified permission is implied by this
	 *         object; <code>false</code> otherwise.
	 */
	public boolean implies(Permission p) {
		if (p instanceof WirePermission) {
			WirePermission requested = (WirePermission) p;
			int requestedMask = requested.getActionsMask();
			return ((getActionsMask() & requestedMask) == requestedMask)
					&& super.implies(p);
		}
		return false;
	}

	/**
	 * Returns the canonical string representation of the actions. Always
	 * returns present actions in the following order: <code>produce</code>,
	 * <code>consume</code>.
	 * 
	 * @return The canonical string representation of the actions.
	 */
	public String getActions() {
		String result = actions;
		if (result == null) {
			StringBuffer sb = new StringBuffer();
			boolean comma = false;
			int mask = getActionsMask();
			if ((mask & ACTION_PRODUCE) == ACTION_PRODUCE) {
				sb.append(PRODUCE);
				comma = true;
			}
			if ((mask & ACTION_CONSUME) == ACTION_CONSUME) {
				if (comma)
					sb.append(',');
				sb.append(CONSUME);
			}
			actions = result = sb.toString();
		}
		return result;
	}

	/**
	 * Returns a new <code>PermissionCollection</code> object for storing
	 * <code>WirePermission</code> objects.
	 * 
	 * @return A new <code>PermissionCollection</code> object suitable for
	 *         storing <code>WirePermission</code> objects.
	 */
	public PermissionCollection newPermissionCollection() {
		return new WirePermissionCollection();
	}

	/**
	 * Determines the equalty of two <code>WirePermission</code> objects.
	 * 
	 * Checks that specified object has the same name and actions as this
	 * <code>WirePermission</code> object.
	 * 
	 * @param obj The object to test for equality.
	 * @return true if <code>obj</code> is a <code>WirePermission</code>, and
	 *         has the same name and actions as this <code>WirePermission</code>
	 *         object; <code>false</code> otherwise.
	 */
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof WirePermission)) {
			return false;
		}
		WirePermission wp = (WirePermission) obj;
		return (getActionsMask() == wp.getActionsMask())
				&& getName().equals(wp.getName());
	}

	/**
	 * Returns the hash code value for this object.
	 * 
	 * @return Hash code value for this object.
	 */
	public int hashCode() {
		int h = 31 * 17 + getName().hashCode();
		h = 31 * h + getActions().hashCode();
		return h;
	}

	/**
	 * Returns a string describing this <code>WirePermission</code>. The
	 * convention is to specify the class name, the permission name, and the
	 * actions in the following format:
	 * '(org.osgi.service.wireadmin.WirePermission &quot;name&quot;
	 * &quot;actions&quot;)'.
	 * 
	 * @return information about this <code>Permission</code> object.
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

	/**
	 * WriteObject is called to save the state of the ServicePermission to a
	 * stream. The actions are serialized, and the superclass takes care of the
	 * name.
	 */
	private synchronized void writeObject(java.io.ObjectOutputStream s)
			throws IOException {
		// Write out the actions. The superclass takes care of the name
		// call getActions to make sure actions field is initialized
		if (actions == null)
			getActions();
		s.defaultWriteObject();
	}

	/**
	 * readObject is called to restore the state of the ServicePermission from a
	 * stream.
	 */
	private synchronized void readObject(java.io.ObjectInputStream s)
			throws IOException, ClassNotFoundException {
		// Read in the action, then initialize the rest
		s.defaultReadObject();
		setTransients(parseActions(actions));
	}
}

/**
 * A <code>WirePermissionCollection</code> stores a set of
 * <code>WirePermission</code> permissions.
 */

final class WirePermissionCollection extends PermissionCollection {
	static final long		serialVersionUID	= 2617521094909826016L;
	/**
	 * Table of permissions.
	 * 
	 * @GuardedBy this
	 * @serial
	 */
	private final Hashtable	permissions;
	/**
	 * Boolean saying if "*" is in the collection.
	 * 
	 * @GuardedBy this
	 * @serial
	 */
	private boolean			all_allowed;

	/**
	 * Creates an empty WirePermissionCollection object.
	 * 
	 */
	public WirePermissionCollection() {
		permissions = new Hashtable();
		all_allowed = false;
	}

	/**
	 * Adds a permission to this PermissionCollection.
	 * 
	 * @param permission The Permission object to add.
	 * 
	 * @throws IllegalArgumentException If the permission is not a
	 *         WirePermission object.
	 * 
	 * @throws SecurityException If this PermissionCollection has been marked
	 *         read-only.
	 */
	public void add(Permission permission) {
		if (!(permission instanceof WirePermission)) {
			throw new IllegalArgumentException("invalid permission: "
					+ permission);
		}
		if (isReadOnly()) {
			throw new SecurityException("attempt to add a Permission to a "
					+ "readonly PermissionCollection");
		}
		WirePermission wp = (WirePermission) permission;
		String name = wp.getName();
		synchronized (this) {
			WirePermission existing = (WirePermission) permissions.get(name);
			if (existing != null) {
				int oldMask = existing.getActionsMask();
				int newMask = wp.getActionsMask();
				if (oldMask != newMask) {
					permissions.put(name, new WirePermission(name, oldMask
							| newMask));
				}
			}
			else {
				permissions.put(name, wp);
			}
			if (!all_allowed) {
				if (name.equals("*")) {
					all_allowed = true;
				}
			}
		}
	}

	/**
	 * Determines if a set of permissions implies the permissions expressed in
	 * <code>permission</code>.
	 * 
	 * @param permission The Permission object to compare.
	 * 
	 * @return <code>true</code> if <code>permission</code> is a proper subset
	 *         of a permission in the set; <code>false</code> otherwise.
	 */
	public boolean implies(Permission permission) {
		if (!(permission instanceof WirePermission)) {
			return false;
		}
		WirePermission requested = (WirePermission) permission;
		WirePermission x;
		int desired = requested.getActionsMask();
		int effective = 0;
		String name = requested.getName();
		synchronized (this) {
			// short circuit if the "*" Permission was added
			if (all_allowed) {
				x = (WirePermission) permissions.get("*");
				if (x != null) {
					effective |= x.getActionsMask();
					if ((effective & desired) == desired)
						return true;
				}
			}
			// strategy:
			// Check for full match first. Then work our way up the
			// name looking for matches on a.b.*
			x = (WirePermission) permissions.get(name);
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
				x = (WirePermission) permissions.get(name);
			}
			if (x != null) {
				effective |= x.getActionsMask();
				if ((effective & desired) == desired) {
					return (true);
				}
			}
			offset = last - 1;
		}
		// we don't have to check for "*" as it was already checked
		// at the top (all_allowed), so we just return false
		return false;
	}

	/**
	 * Returns an enumeration of all the Permission objects in the container.
	 * 
	 * @return Enumeration of all the Permission objects.
	 */
	public Enumeration elements() {
		return permissions.elements();
	}
}
