/*
 * Copyright (c) OSGi Alliance (2004, 2009). All Rights Reserved.
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

package org.osgi.service.cm;

import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * Indicates a bundle's authority to configure bundles.
 * 
 * This permission has only a single action: CONFIGURE.
 * 
 * @ThreadSafe
 * @version $Revision: 6381 $
 * @since 1.2
 */

public final class ConfigurationPermission extends BasicPermission {
	static final long			serialVersionUID	= 5716868734811965383L;
	/**
	 * The action string <code>configure</code>.
	 */
	public final static String	CONFIGURE			= "configure";

	/**
	 * Create a new ConfigurationPermission.
	 * 
	 * @param name Name must be &quot;*&quot;.
	 * @param actions <code>configure</code> (canonical order).
	 */

	public ConfigurationPermission(String name, String actions) {
		super(name);
		if (!name.equals("*")) {
			throw new IllegalArgumentException("name must be *");
		}
		actions = actions.trim();
		if (actions.equalsIgnoreCase(CONFIGURE)||actions.equals("*"))
			return;
		
		throw new IllegalArgumentException("actions must be " + CONFIGURE);
	}

	/**
	 * Determines if a <code>ConfigurationPermission</code> object "implies"
	 * the specified permission.
	 * 
	 * @param p The target permission to check.
	 * @return <code>true</code> if the specified permission is implied by
	 *         this object; <code>false</code> otherwise.
	 */

	public boolean implies(Permission p) {
		return p instanceof ConfigurationPermission;
	}

	/**
	 * Determines the equality of two <code>ConfigurationPermission</code>
	 * objects.
	 * <p>
	 * Two <code>ConfigurationPermission</code> objects are equal.
	 * 
	 * @param obj The object being compared for equality with this object.
	 * @return <code>true</code> if <code>obj</code> is equivalent to this
	 *         <code>ConfigurationPermission</code>; <code>false</code>
	 *         otherwise.
	 */
	public boolean equals(Object obj) {
		return obj instanceof ConfigurationPermission;
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
	 * Returns the canonical string representation of the
	 * <code>ConfigurationPermission</code> actions.
	 * 
	 * <p>
	 * Always returns present <code>ConfigurationPermission</code> actions in
	 * the following order: <code>CONFIGURE</code>
	 * 
	 * @return Canonical string representation of the
	 *         <code>ConfigurationPermission</code> actions.
	 */
	public String getActions() {
		return CONFIGURE;
	}

	/**
	 * Returns a new <code>PermissionCollection</code> object suitable for
	 * storing <code>ConfigurationPermission</code>s.
	 * 
	 * @return A new <code>PermissionCollection</code> object.
	 */
	public PermissionCollection newPermissionCollection() {
		return new ConfigurationPermissionCollection();
	}
}

/**
 * Stores a set of <code>ConfigurationPermission</code> permissions.
 * 
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 */
final class ConfigurationPermissionCollection extends PermissionCollection {
	static final long	serialVersionUID	= -6917638867081695839L;
	/**
	 * True if collection is non-empty.
	 * 
	 * @serial
	 */
	private volatile boolean	hasElement;

	/**
	 * Creates an empty <tt>ConfigurationPermissionCollection</tt> object.
	 * 
	 */
	public ConfigurationPermissionCollection() {
		hasElement = false;
	}

	/**
	 * Adds the specified permission to the
	 * <tt>ConfigurationPermissionCollection</tt>. The key for the hash is
	 * the interface name of the service.
	 * 
	 * @param permission The <tt>Permission</tt> object to add.
	 * 
	 * @exception IllegalArgumentException If the permission is not an
	 *            <tt>ConfigurationPermission</tt>.
	 * 
	 * @exception SecurityException If this ConfigurationPermissionCollection
	 *            object has been marked read-only.
	 */

	public void add(Permission permission) {
		if (!(permission instanceof ConfigurationPermission)) {
			throw new IllegalArgumentException("invalid permission: "
					+ permission);
		}

		if (isReadOnly())
			throw new SecurityException("attempt to add a Permission to a "
					+ "readonly PermissionCollection");

		hasElement = true;
	}

	/**
	 * Determines if the specified set of permissions implies the permissions
	 * expressed in the parameter <tt>permission</tt>.
	 * 
	 * @param p The Permission object to compare.
	 * 
	 * @return true if permission is a proper subset of a permission in the set;
	 *         false otherwise.
	 */

	public boolean implies(Permission p) {
		return hasElement && (p instanceof ConfigurationPermission);
	}

	/**
	 * Returns an enumeration of an <tt>ConfigurationPermission</tt> object.
	 * 
	 * @return Enumeration of an <tt>ConfigurationPermission</tt> object.
	 */

	public Enumeration elements() {
		final boolean nonEmpty = hasElement;
		return new Enumeration() {
			private boolean	more = nonEmpty;

			public boolean hasMoreElements() {
				return more;
			}

			public Object nextElement() {
				if (more) {
					more = false;

					return new ConfigurationPermission("*",
							ConfigurationPermission.CONFIGURE);
				}
				else {
					throw new NoSuchElementException();
				}
			}
		};
	}
}
