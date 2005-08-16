/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/PackagePermission.java,v 1.10 2005/05/13 20:32:55 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2000, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.framework;

import java.io.IOException;
import java.security.*;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * A bundle's authority to import or export a package.
 * 
 * <p>
 * A package is a dot-separated string that defines a fully qualified Java
 * package.
 * <p>
 * For example:
 * 
 * <pre>
 * <code>
 * org.osgi.service.http
 * </code>
 * </pre>
 * 
 * <p>
 * <code>PackagePermission</code> has two actions: <code>EXPORT</code> and
 * <code>IMPORT</code>. The <code>EXPORT</code> action implies the <code>IMPORT</code>
 * action.
 * 
 * @version $Revision: 1.10 $
 */

public final class PackagePermission extends BasicPermission {
	static final long			serialVersionUID	= -5107705877071099135L;
	/**
	 * The action string <code>export</code>.
	 */
	public final static String	EXPORT				= "export";

	/**
	 * The action string <code>import</code>.
	 */
	public final static String	IMPORT				= "import";

	private final static int	ACTION_EXPORT		= 0x00000001;
	private final static int	ACTION_IMPORT		= 0x00000002;
	private final static int	ACTION_ALL			= ACTION_EXPORT
															| ACTION_IMPORT;
	private final static int	ACTION_NONE			= 0;
	private final static int	ACTION_ERROR		= 0x80000000;

	/**
	 * The actions mask.
	 */
	private transient int		action_mask			= ACTION_NONE;

	/**
	 * The actions in canonical form.
	 * 
	 * @serial
	 */
	private String				actions				= null;

	/**
	 * Defines the authority to import and/or export a package within the OSGi
	 * environment.
	 * <p>
	 * The name is specified as a normal Java package name: a dot-separated
	 * string. Wildcards may be used. For example:
	 * 
	 * <pre>
	 * 
	 *  org.osgi.service.http
	 *  javax.servlet.*
	 *  *
	 *  
	 * </pre>
	 * 
	 * <p>
	 * Package Permissions are granted over all possible versions of a package.
	 * 
	 * A bundle that needs to export a package must have the appropriate
	 * <code>PackagePermission</code> for that package; similarly, a bundle that
	 * needs to import a package must have the appropriate
	 * <code>PackagePermssion</code> for that package.
	 * <p>
	 * Permission is granted for both classes and resources.
	 * 
	 * @param name Package name.
	 * @param actions <code>EXPORT</code>,<code>IMPORT</code> (canonical order).
	 */

	public PackagePermission(String name, String actions) {
		this(name, getMask(actions));
	}

	/**
	 * Package private constructor used by PackagePermissionCollection.
	 * 
	 * @param name class name
	 * @param mask action mask
	 */
	PackagePermission(String name, int mask) {
		super(name);
		init(mask);
	}

	/**
	 * Called by constructors and when deserialized.
	 * 
	 * @param mask action mask
	 */
	private void init(int mask) {
		if ((mask == ACTION_NONE) || ((mask & ACTION_ALL) != mask)) {
			throw new IllegalArgumentException("invalid action string");
		}

		action_mask = mask;
	}

	/**
	 * Parse action string into action mask.
	 * 
	 * @param actions Action string.
	 * @return action mask.
	 */
	private static int getMask(String actions) {
		boolean seencomma = false;

		int mask = ACTION_NONE;

		if (actions == null) {
			return (mask);
		}

		char[] a = actions.toCharArray();

		int i = a.length - 1;
		if (i < 0)
			return (mask);

		while (i != -1) {
			char c;

			// skip whitespace
			while ((i != -1)
					&& ((c = a[i]) == ' ' || c == '\r' || c == '\n'
							|| c == '\f' || c == '\t'))
				i--;

			// check for the known strings
			int matchlen;

			if (i >= 5 && (a[i - 5] == 'i' || a[i - 5] == 'I')
					&& (a[i - 4] == 'm' || a[i - 4] == 'M')
					&& (a[i - 3] == 'p' || a[i - 3] == 'P')
					&& (a[i - 2] == 'o' || a[i - 2] == 'O')
					&& (a[i - 1] == 'r' || a[i - 1] == 'R')
					&& (a[i] == 't' || a[i] == 'T')) {
				matchlen = 6;
				mask |= ACTION_IMPORT;

			}
			else
				if (i >= 5 && (a[i - 5] == 'e' || a[i - 5] == 'E')
						&& (a[i - 4] == 'x' || a[i - 4] == 'X')
						&& (a[i - 3] == 'p' || a[i - 3] == 'P')
						&& (a[i - 2] == 'o' || a[i - 2] == 'O')
						&& (a[i - 1] == 'r' || a[i - 1] == 'R')
						&& (a[i] == 't' || a[i] == 'T')) {
					matchlen = 6;
					mask |= ACTION_EXPORT | ACTION_IMPORT;

				}
				else {
					// parse error
					throw new IllegalArgumentException("invalid permission: "
							+ actions);
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

		return (mask);
	}

	/**
	 * Determines if the specified permission is implied by this object.
	 * 
	 * <p>
	 * This method checks that the package name of the target is implied by the
	 * package name of this object. The list of <code>PackagePermission</code>
	 * actions must either match or allow for the list of the target object to
	 * imply the target <code>PackagePermission</code> action.
	 * <p>
	 * The permission to export a package implies the permission to import the
	 * named package.
	 * 
	 * <pre>
	 *  x.y.*,&quot;export&quot; -&gt; x.y.z,&quot;export&quot; is true
	 *  *,&quot;import&quot; -&gt; x.y, &quot;import&quot;      is true
	 *  *,&quot;export&quot; -&gt; x.y, &quot;import&quot;      is true
	 *  x.y,&quot;export&quot; -&gt; x.y.z, &quot;export&quot;  is false
	 * </pre>
	 * 
	 * @param p The target permission to interrogate.
	 * @return <code>true</code> if the specified <code>PackagePermission</code>
	 *         action is implied by this object; <code>false</code> otherwise.
	 */

	public boolean implies(Permission p) {
		if (p instanceof PackagePermission) {
			PackagePermission target = (PackagePermission) p;

			return (((action_mask & target.action_mask) == target.action_mask) && super
					.implies(p));
		}

		return (false);
	}

	/**
	 * Returns the canonical string representation of the
	 * <code>PackagePermission</code> actions.
	 * 
	 * <p>
	 * Always returns present <code>PackagePermission</code> actions in the
	 * following order: <code>EXPORT</code>,<code>IMPORT</code>.
	 * 
	 * @return Canonical string representation of the <code>PackagePermission</code>
	 *         actions.
	 */

	public String getActions() {
		if (actions == null) {
			StringBuffer sb = new StringBuffer();
			boolean comma = false;

			if ((action_mask & ACTION_EXPORT) == ACTION_EXPORT) {
				sb.append(EXPORT);
				comma = true;
			}

			if ((action_mask & ACTION_IMPORT) == ACTION_IMPORT) {
				if (comma)
					sb.append(',');
				sb.append(IMPORT);
			}

			actions = sb.toString();
		}

		return (actions);
	}

	/**
	 * Returns a new <code>PermissionCollection</code> object suitable for storing
	 * <code>PackagePermission</code> objects.
	 * 
	 * @return A new <code>PermissionCollection</code> object.
	 */
	public PermissionCollection newPermissionCollection() {
		return (new PackagePermissionCollection());
	}

	/**
	 * Determines the equality of two <code>PackagePermission</code> objects.
	 * 
	 * This method checks that specified package has the same package name and
	 * <code>PackagePermission</code> actions as this <code>PackagePermission</code>
	 * object.
	 * 
	 * @param obj The object to test for equality with this
	 *        <code>PackagePermission</code> object.
	 * @return <code>true</code> if <code>obj</code> is a <code>PackagePermission</code>,
	 *         and has the same package name and actions as this
	 *         <code>PackagePermission</code> object; <code>false</code> otherwise.
	 */
	public boolean equals(Object obj) {
		if (obj == this) {
			return (true);
		}

		if (!(obj instanceof PackagePermission)) {
			return (false);
		}

		PackagePermission p = (PackagePermission) obj;

		return ((action_mask == p.action_mask) && getName().equals(p.getName()));
	}

	/**
	 * Returns the hash code value for this object.
	 * 
	 * @return A hash code value for this object.
	 */

	public int hashCode() {
		return (getName().hashCode() ^ getActions().hashCode());
	}

	/**
	 * Returns the current action mask.
	 * <p>
	 * Used by the PackagePermissionCollection class.
	 * 
	 * @return Current action mask.
	 */
	int getMask() {
		return (action_mask);
	}

	/**
	 * WriteObject is called to save the state of this permission object to a
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
	 * readObject is called to restore the state of this permission from a
	 * stream.
	 */
	private synchronized void readObject(java.io.ObjectInputStream s)
			throws IOException, ClassNotFoundException {
		// Read in the action, then initialize the rest
		s.defaultReadObject();
		init(getMask(actions));
	}
}

/**
 * Stores a set of <code>PackagePermission</code> permissions.
 * 
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 */

final class PackagePermissionCollection extends PermissionCollection {
	static final long	serialVersionUID	= -3350758995234427603L;
	/**
	 * Table of permissions.
	 * 
	 * @serial
	 */
	private Hashtable	permissions;

	/**
	 * Boolean saying if "*" is in the collection.
	 * 
	 * @serial
	 */
	private boolean		all_allowed;

	/**
	 * Create an empty PackagePermissions object.
	 *  
	 */

	public PackagePermissionCollection() {
		permissions = new Hashtable();
		all_allowed = false;
	}

	/**
	 * Adds a permission to the <code>PackagePermission</code> objects. The key
	 * for the hash is the name.
	 * 
	 * @param permission The <code>PackagePermission</code> object to add.
	 * 
	 * @exception IllegalArgumentException If the permission is not a
	 *            <code>PackagePermission</code> instance.
	 * 
	 * @exception SecurityException If this <code>PackagePermissionCollection</code>
	 *            object has been marked read-only.
	 */

	public void add(Permission permission) {
		if (!(permission instanceof PackagePermission))
			throw new IllegalArgumentException("invalid permission: "
					+ permission);
		if (isReadOnly())
			throw new SecurityException("attempt to add a Permission to a "
					+ "readonly PermissionCollection");

		PackagePermission pp = (PackagePermission) permission;
		String name = pp.getName();

		PackagePermission existing = (PackagePermission) permissions.get(name);

		if (existing != null) {
			int oldMask = existing.getMask();
			int newMask = pp.getMask();
			if (oldMask != newMask) {
				permissions.put(name, new PackagePermission(name, oldMask
						| newMask));

			}
		}
		else {
			permissions.put(name, permission);
		}

		if (!all_allowed) {
			if (name.equals("*"))
				all_allowed = true;
		}
	}

	/**
	 * Determines if the specified permissions implies the permissions expressed
	 * in <code>permission</code>.
	 * 
	 * @param permission The Permission object to compare with this
	 *        <code>PackagePermission</code> object.
	 * 
	 * @return <code>true</code> if <code>permission</code> is a proper subset of a
	 *         permission in the set; <code>false</code> otherwise.
	 */

	public boolean implies(Permission permission) {
		if (!(permission instanceof PackagePermission))
			return (false);

		PackagePermission pp = (PackagePermission) permission;
		PackagePermission x;

		int desired = pp.getMask();
		int effective = 0;

		// short circuit if the "*" Permission was added
		if (all_allowed) {
			x = (PackagePermission) permissions.get("*");
			if (x != null) {
				effective |= x.getMask();
				if ((effective & desired) == desired)
					return (true);
			}
		}

		// strategy:
		// Check for full match first. Then work our way up the
		// name looking for matches on a.b.*

		String name = pp.getName();

		x = (PackagePermission) permissions.get(name);

		if (x != null) {
			// we have a direct hit!
			effective |= x.getMask();
			if ((effective & desired) == desired)
				return (true);
		}

		// work our way up the tree...
		int last, offset;

		offset = name.length() - 1;

		while ((last = name.lastIndexOf(".", offset)) != -1) {

			name = name.substring(0, last + 1) + "*";
			x = (PackagePermission) permissions.get(name);

			if (x != null) {
				effective |= x.getMask();
				if ((effective & desired) == desired)
					return (true);
			}
			offset = last - 1;
		}

		// we don't have to check for "*" as it was already checked
		// at the top (all_allowed), so we just return false
		return (false);
	}

	/**
	 * Returns an enumeration of all <code>PackagePermission</code> objects in the
	 * container.
	 * 
	 * @return Enumeration of all <code>PackagePermission</code> objects.
	 */

	public Enumeration elements() {
		return (permissions.elements());
	}
}

