/*
 * Copyright (c) OSGi Alliance (2010, 2011). All Rights Reserved.
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
package org.osgi.framework;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.security.AccessController;
import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A bundle's authority to adapt an object to a type.
 * 
 * <p>
 * {@code AdaptPermission} has one action: {@code adapt}.
 * 
 * @ThreadSafe
 * @version $Id: bc4c5d392d2534a7744f6fc00f4665502f82033c $
 */
public class AdaptPermission extends BasicPermission {

	private static final long						serialVersionUID	= 1L;

	/**
	 * The action string {@code initiate}.
	 */
	public final static String						ADAPT				= "adapt";

	private final static int						ACTION_ADAPT		= 0x00000001;
	private final static int						ACTION_ALL			= ACTION_ADAPT;
	final static int								ACTION_NONE			= 0;

	/**
	 * The actions mask.
	 */
	transient int									action_mask;

	/**
	 * The actions in canonical form.
	 * 
	 * @serial
	 */
	private volatile String							actions				= null;

	/**
	 * The bundle used by this AdaptPermission.
	 */
	transient final Bundle							bundle;

	/**
	 * This holds a Filter matching object used to evaluate the filter in
	 * implies.
	 */
	transient Filter								filter;

	/**
	 * This map holds the properties of the permission, used to match a filter
	 * in implies. This is not initialized until necessary, and then cached in
	 * this object.
	 */
	private transient volatile Map<String, Object>	properties;

	/**
	 * Creates a new granted {@code AdaptPermission} object.
	 * 
	 * This constructor must only be used to create a permission that is going
	 * to be checked.
	 * <p>
	 * Examples:
	 * 
	 * <pre>
	 * (adaptClass=com.acme.*)
	 * (&amp;(signer=\*,o=ACME,c=US)(adaptClass=com.acme.*))
	 * (signer=\*,o=ACME,c=US)
	 * </pre>
	 * 
	 * <p>
	 * When a signer key is used within the filter expression the signer value
	 * must escape the special filter chars ('*', '(', ')').
	 * <p>
	 * The name is specified as a filter expression. The filter gives access to
	 * the following attributes:
	 * <ul>
	 * <li>signer - A Distinguished Name chain used to sign the exporting
	 * bundle. Wildcards in a DN are not matched according to the filter string
	 * rules, but according to the rules defined for a DN chain.</li>
	 * <li>location - The location of the exporting bundle.</li>
	 * <li>id - The bundle ID of the exporting bundle.</li>
	 * <li>name - The symbolic name of the exporting bundle.</li>
	 * <li>adaptClass - The name of the type to which an object can be adapted.</li>
	 * </ul>
	 * Filter attribute names are processed in a case sensitive manner.
	 * 
	 * @param filter A filter expression. Filter attribute names are processed
	 *        in a case sensitive manner. A special value of {@code "*"} can be
	 *        used to match all adaptations.
	 * @param actions {@code adapt}.
	 * @throws IllegalArgumentException If the filter has an invalid syntax.
	 */
	public AdaptPermission(String filter, String actions) {
		this(parseFilter(filter), parseActions(actions));
	}

	/**
	 * Creates a new requested {@code AdaptPermission} object to be used by the
	 * code that must perform {@code checkPermission}. {@code AdaptPermission}
	 * objects created with this constructor cannot be added to an
	 * {@code AdaptPermission} permission collection.
	 * 
	 * @param adaptClass The name of the type to which an object can be adapted.
	 * @param adaptableBundle The bundle associated with the object being
	 *        adapted.
	 * @param actions {@code adapt}.
	 */
	public AdaptPermission(String adaptClass, Bundle adaptableBundle,
			String actions) {
		super(adaptClass);
		setTransients(null, parseActions(actions));
		this.bundle = adaptableBundle;
		if (adaptClass == null) {
			throw new NullPointerException("adaptClass must not be null");
		}
		if (adaptableBundle == null) {
			throw new NullPointerException("adaptableBundle must not be null");
		}
	}

	/**
	 * Package private constructor used by AdaptPermissionCollection.
	 * 
	 * @param filter name filter
	 * @param mask action mask
	 */
	AdaptPermission(Filter filter, int mask) {
		super((filter == null) ? "*" : filter.toString());
		setTransients(filter, mask);
		this.bundle = null;
	}

	/**
	 * Called by constructors and when deserialized.
	 * 
	 * @param filter Permission's filter or {@code null} for wildcard.
	 * @param mask action mask
	 */
	private void setTransients(Filter filter, int mask) {
		this.filter = filter;
		if ((mask == ACTION_NONE) || ((mask & ACTION_ALL) != mask)) {
			throw new IllegalArgumentException("invalid action string");
		}
		this.action_mask = mask;
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

			if (i >= 4 && (a[i - 4] == 'a' || a[i - 4] == 'A')
					&& (a[i - 3] == 'd' || a[i - 3] == 'D')
					&& (a[i - 2] == 'a' || a[i - 2] == 'A')
					&& (a[i - 1] == 'p' || a[i - 1] == 'P')
					&& (a[i] == 't' || a[i] == 'T')) {
				matchlen = 5;
				mask |= ACTION_ADAPT;

			}
			else {
				// parse error
				throw new IllegalArgumentException("invalid actions: "
						+ actions);
			}

			// make sure we didn't just match the tail of a word
			// like "ackbarfadapt". Also, skip to the comma.
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
			throw new IllegalArgumentException("invalid actions: " + actions);
		}

		return mask;
	}

	/**
	 * Parse filter string into a Filter object.
	 * 
	 * @param filterString The filter string to parse.
	 * @return a Filter for this bundle.
	 * @throws IllegalArgumentException If the filter syntax is invalid.
	 */
	private static Filter parseFilter(String filterString) {
		filterString = filterString.trim();
		if (filterString.equals("*")) {
			return null;
		}
		try {
			return FrameworkUtil.createFilter(filterString);
		}
		catch (InvalidSyntaxException e) {
			IllegalArgumentException iae = new IllegalArgumentException(
					"invalid filter");
			iae.initCause(e);
			throw iae;
		}
	}

	/**
	 * Determines if the specified permission is implied by this object.
	 * 
	 * <p>
	 * This method checks that the filter of the target is implied by the adapt
	 * class name of this object. The list of {@code AdaptPermission} actions
	 * must either match or allow for the list of the target object to imply the
	 * target {@code AdaptPermission} action.
	 * <p>
	 * 
	 * @param p The requested permission.
	 * @return {@code true} if the specified permission is implied by this
	 *         object; {@code false} otherwise.
	 */
	public boolean implies(Permission p) {
		if (!(p instanceof AdaptPermission)) {
			return false;
		}
		AdaptPermission requested = (AdaptPermission) p;
		if (bundle != null) {
			return false;
		}
		// if requested permission has a filter, then it is an invalid argument
		if (requested.filter != null) {
			return false;
		}
		return implies0(requested, ACTION_NONE);
	}

	/**
	 * Internal implies method. Used by the implies and the permission
	 * collection implies methods.
	 * 
	 * @param requested The requested AdaptPermission which has already be
	 *        validated as a proper argument. The requested AdaptPermission must
	 *        not have a filter expression.
	 * @param effective The effective actions with which to start.
	 * @return {@code true} if the specified permission is implied by this
	 *         object; {@code false} otherwise.
	 */
	boolean implies0(AdaptPermission requested, int effective) {
		/* check actions first - much faster */
		effective |= action_mask;
		final int desired = requested.action_mask;
		if ((effective & desired) != desired) {
			return false;
		}
		/* Get filter */
		Filter f = filter;
		if (f == null) {
			// it's "*"
			return true;
		}
		return f.matches(requested.getProperties());
	}

	/**
	 * Returns the canonical string representation of the
	 * {@code AdaptPermission} actions.
	 * 
	 * <p>
	 * Always returns present {@code AdaptPermission} actions in the following
	 * order: {@code adapt}.
	 * 
	 * @return Canonical string representation of the {@code AdaptPermission}
	 *         actions.
	 */
	public String getActions() {
		String result = actions;
		if (result == null) {
			actions = result = ADAPT;
		}
		return result;
	}

	/**
	 * Returns a new {@code PermissionCollection} object suitable for storing
	 * {@code AdaptPermission} objects.
	 * 
	 * @return A new {@code PermissionCollection} object.
	 */
	public PermissionCollection newPermissionCollection() {
		return new AdaptPermissionCollection();
	}

	/**
	 * Determines the equality of two {@code AdaptPermission} objects.
	 * 
	 * This method checks that specified permission has the same name and
	 * {@code AdaptPermission} actions as this {@code AdaptPermission} object.
	 * 
	 * @param obj The object to test for equality with this
	 *        {@code AdaptPermission} object.
	 * @return {@code true} if {@code obj} is a {@code AdaptPermission}, and has
	 *         the same name and actions as this {@code AdaptPermission} object;
	 *         {@code false} otherwise.
	 */
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof AdaptPermission)) {
			return false;
		}

		AdaptPermission cp = (AdaptPermission) obj;

		return (action_mask == cp.action_mask)
				&& getName().equals(cp.getName())
				&& ((bundle == cp.bundle) || ((bundle != null) && bundle
						.equals(cp.bundle)));
	}

	/**
	 * Returns the hash code value for this object.
	 * 
	 * @return A hash code value for this object.
	 */
	public int hashCode() {
		int h = 31 * 17 + getName().hashCode();
		h = 31 * h + getActions().hashCode();
		if (bundle != null) {
			h = 31 * h + bundle.hashCode();
		}
		return h;
	}

	/**
	 * WriteObject is called to save the state of this permission object to a
	 * stream. The actions are serialized, and the superclass takes care of the
	 * name.
	 */
	private synchronized void writeObject(java.io.ObjectOutputStream s)
			throws IOException {
		if (bundle != null) {
			throw new NotSerializableException("cannot serialize");
		}
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
		setTransients(parseFilter(getName()), parseActions(actions));
	}

	/**
	 * Called by {@code <@link AdaptPermission#implies(Permission)>}. This
	 * method is only called on a requested permission which cannot have a
	 * filter set.
	 * 
	 * @return a map of properties for this permission.
	 */
	private Map<String, Object> getProperties() {
		Map<String, Object> result = properties;
		if (result != null) {
			return result;
		}
		final Map<String, Object> map = new HashMap<String, Object>(5);
		map.put("adaptClass", getName());
		if (bundle != null) {
			AccessController.doPrivileged(new PrivilegedAction<Object>() {
				public Object run() {
					map.put("id", new Long(bundle.getBundleId()));
					map.put("location", bundle.getLocation());
					String name = bundle.getSymbolicName();
					if (name != null) {
						map.put("name", name);
					}
					SignerProperty signer = new SignerProperty(bundle);
					if (signer.isBundleSigned()) {
						map.put("signer", signer);
					}
					return null;
				}
			});
		}
		return properties = map;
	}
}

/**
 * Stores a set of {@code AdaptPermission} permissions.
 * 
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 */

final class AdaptPermissionCollection extends PermissionCollection {
	static final long						serialVersionUID	= -3350758995234427603L;
	/**
	 * Collection of permissions.
	 * 
	 * @serial
	 * @GuardedBy this
	 */
	private Map<String, AdaptPermission>	permissions;

	/**
	 * Boolean saying if "*" is in the collection.
	 * 
	 * @serial
	 * @GuardedBy this
	 */
	private boolean							all_allowed;

	/**
	 * Create an empty AdaptPermissions object.
	 */
	public AdaptPermissionCollection() {
		permissions = new HashMap<String, AdaptPermission>();
		all_allowed = false;
	}

	/**
	 * Adds a permission to this permission collection.
	 * 
	 * @param permission The {@code AdaptPermission} object to add.
	 * @throws IllegalArgumentException If the specified permission is not a
	 *         {@code AdaptPermission} instance or was constructed with a Bundle
	 *         object.
	 * @throws SecurityException If this {@code AdaptPermissionCollection}
	 *         object has been marked read-only.
	 */
	public void add(final Permission permission) {
		if (!(permission instanceof AdaptPermission)) {
			throw new IllegalArgumentException("invalid permission: "
					+ permission);
		}
		if (isReadOnly()) {
			throw new SecurityException("attempt to add a Permission to a "
					+ "readonly PermissionCollection");
		}

		final AdaptPermission ap = (AdaptPermission) permission;
		if (ap.bundle != null) {
			throw new IllegalArgumentException("cannot add to collection: "
					+ ap);
		}

		final String name = ap.getName();
		synchronized (this) {
			Map<String, AdaptPermission> pc = permissions;
			final AdaptPermission existing = pc.get(name);
			if (existing != null) {
				final int oldMask = existing.action_mask;
				final int newMask = ap.action_mask;
				if (oldMask != newMask) {
					pc.put(name, new AdaptPermission(existing.filter, oldMask
							| newMask));

				}
			}
			else {
				pc.put(name, ap);
			}

			if (!all_allowed) {
				if (name.equals("*")) {
					all_allowed = true;
				}
			}
		}
	}

	/**
	 * Determines if the specified permissions implies the permissions expressed
	 * in {@code permission}.
	 * 
	 * @param permission The Permission object to compare with this
	 *        {@code AdaptPermission} object.
	 * @return {@code true} if {@code permission} is a proper subset of a
	 *         permission in the set; {@code false} otherwise.
	 */
	public boolean implies(final Permission permission) {
		if (!(permission instanceof AdaptPermission)) {
			return false;
		}
		final AdaptPermission requested = (AdaptPermission) permission;
		/* if requested permission has a filter, then it is an invalid argument */
		if (requested.filter != null) {
			return false;
		}

		int effective = AdaptPermission.ACTION_NONE;

		Collection<AdaptPermission> perms;
		synchronized (this) {
			Map<String, AdaptPermission> pc = permissions;
			/* short circuit if the "*" Permission was added */
			if (all_allowed) {
				AdaptPermission ap = pc.get("*");
				if (ap != null) {
					effective |= ap.action_mask;
					final int desired = requested.action_mask;
					if ((effective & desired) == desired) {
						return true;
					}
				}
			}
			perms = pc.values();
		}
		/* iterate one by one over filteredPermissions */
		for (AdaptPermission perm : perms) {
			if (perm.implies0(requested, effective)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns an enumeration of all {@code AdaptPermission} objects in the
	 * container.
	 * 
	 * @return Enumeration of all {@code AdaptPermission} objects.
	 */
	public synchronized Enumeration<Permission> elements() {
		List<Permission> all = new ArrayList<Permission>(permissions.values());
		return Collections.enumeration(all);
	}

	/* serialization logic */
	private static final ObjectStreamField[]	serialPersistentFields	= {
			new ObjectStreamField("permissions", HashMap.class),
			new ObjectStreamField("all_allowed", Boolean.TYPE)			};

	private synchronized void writeObject(ObjectOutputStream out)
			throws IOException {
		ObjectOutputStream.PutField pfields = out.putFields();
		pfields.put("permissions", permissions);
		pfields.put("all_allowed", all_allowed);
		out.writeFields();
	}

	private synchronized void readObject(java.io.ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		ObjectInputStream.GetField gfields = in.readFields();
		permissions = (HashMap<String, AdaptPermission>) gfields.get(
				"permissions", null);
		all_allowed = gfields.get("all_allowed", false);
	}
}
