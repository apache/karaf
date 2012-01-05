/*
 * Copyright (c) OSGi Alliance (2000, 2011). All Rights Reserved.
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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A bundle's authority to provide or require a capability.
 * <ul>
 * <li>The {@code provide} action allows a bundle to provide a capability
 * matching the specified filter.
 * <li>The {@code require} action allows a bundle to require a capability
 * matching the specified filter.
 * </ul>
 * 
 * @ThreadSafe
 * @version $Id: bab1ac06b46613f6cff39b291295d8b3e51d58ce $
 * @since 1.6
 */

public final class CapabilityPermission extends BasicPermission {
	static final long								serialVersionUID	= -7662148639076511574L;
	/**
	 * The action string {@code require}.
	 */
	public final static String						REQUIRE				= "require";
	/**
	 * The action string {@code provide}.
	 */
	public final static String						PROVIDE				= "provide";

	private final static int						ACTION_REQUIRE		= 0x00000001;
	private final static int						ACTION_PROVIDE		= 0x00000002;
	private final static int						ACTION_ALL			= ACTION_REQUIRE
																				| ACTION_PROVIDE;
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
	 * The attributes of the requested capability. Must be null if not
	 * constructed with attributes.
	 */
	transient final Map<String, Object>				attributes;

	/**
	 * The bundle of the requested capability. Must be null if not constructed
	 * with bundle.
	 */
	transient final Bundle							bundle;

	/**
	 * If this CapabilityPermission was constructed with a filter, this holds a
	 * Filter matching object used to evaluate the filter in implies.
	 */
	transient Filter								filter;

	/**
	 * This map holds the properties of the permission, used to match a filter
	 * in implies. This is not initialized until necessary, and then cached in
	 * this object.
	 */
	private transient volatile Map<String, Object>	properties;

	/**
	 * Create a new CapabilityPermission.
	 * 
	 * <p>
	 * The name is specified as a dot-separated string. Wildcards may be used.
	 * 
	 * <pre>
	 * name ::= &lt;namespace&gt; | &lt;namespace ending in &quot;.*&quot;&gt; | *
	 * </pre>
	 * 
	 * Examples:
	 * 
	 * <pre>
	 * com.acme.capability.*
	 * org.foo.capability
	 * *
	 * </pre>
	 * 
	 * For the {@code require} action, the name can also be a filter expression.
	 * The filter gives access to the capability attributes as well as the
	 * following attributes:
	 * <ul>
	 * <li>signer - A Distinguished Name chain used to sign the bundle providing
	 * the capability. Wildcards in a DN are not matched according to the filter
	 * string rules, but according to the rules defined for a DN chain.</li>
	 * <li>location - The location of the bundle providing the capability.</li>
	 * <li>id - The bundle ID of the bundle providing the capability.</li>
	 * <li>name - The symbolic name of the bundle providing the capability.</li>
	 * <li>capability.namespace - The name space of the required capability.</li>
	 * </ul>
	 * Since the above attribute names may conflict with attribute names of a
	 * capability, you can prefix an attribute name with '@' in the filter
	 * expression to match against the capability attributes and not one of the
	 * above attributes. Filter attribute names are processed in a case
	 * sensitive manner.
	 * 
	 * <p>
	 * There are two possible actions: {@code require} and {@code provide}. The
	 * {@code require} permission allows the owner of this permission to require
	 * a capability matching the attributes. The {@code provide} permission
	 * allows the bundle to provide a capability in the specified capability
	 * name space.
	 * 
	 * @param name The capability name space or a filter over the attributes.
	 * @param actions {@code require},{@code provide} (canonical order)
	 * @throws IllegalArgumentException If the specified name is a filter
	 *         expression and either the specified action is not {@code require}
	 *         or the filter has an invalid syntax.
	 */
	public CapabilityPermission(String name, String actions) {
		this(name, parseActions(actions));
		if ((this.filter != null)
				&& ((action_mask & ACTION_ALL) != ACTION_REQUIRE)) {
			throw new IllegalArgumentException(
					"invalid action string for filter expression");
		}
	}

	/**
	 * Creates a new requested {@code CapabilityPermission} object to be used by
	 * code that must perform {@code checkPermission} for the {@code require}
	 * action. {@code CapabilityPermission} objects created with this
	 * constructor cannot be added to a {@code CapabilityPermission} permission
	 * collection.
	 * 
	 * @param namespace The requested capability name space.
	 * @param attributes The requested capability attributes.
	 * @param providingBundle The bundle providing the requested capability.
	 * @param actions The action {@code require}.
	 * @throws IllegalArgumentException If the specified action is not
	 *         {@code require} or attributes or providingBundle are {@code null}
	 *         .
	 */
	public CapabilityPermission(String namespace, Map<String, ? > attributes,
			Bundle providingBundle, String actions) {
		super(namespace);
		setTransients(namespace, parseActions(actions));
		if (attributes == null) {
			throw new IllegalArgumentException("attributes must not be null");
		}
		if (providingBundle == null) {
			throw new IllegalArgumentException("bundle must not be null");
		}
		this.attributes = new HashMap<String, Object>(attributes);
		this.bundle = providingBundle;
		if ((action_mask & ACTION_ALL) != ACTION_REQUIRE) {
			throw new IllegalArgumentException("invalid action string");
		}
	}

	/**
	 * Package private constructor used by CapabilityPermissionCollection.
	 * 
	 * @param name class name
	 * @param mask action mask
	 */
	CapabilityPermission(String name, int mask) {
		super(name);
		setTransients(name, mask);
		this.attributes = null;
		this.bundle = null;
	}

	/**
	 * Called by constructors and when deserialized.
	 * 
	 * @param mask action mask
	 */
	private void setTransients(String name, int mask) {
		if ((mask == ACTION_NONE) || ((mask & ACTION_ALL) != mask)) {
			throw new IllegalArgumentException("invalid action string");
		}
		action_mask = mask;
		filter = parseFilter(name);
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

			if (i >= 6 && (a[i - 6] == 'r' || a[i - 6] == 'R')
					&& (a[i - 5] == 'e' || a[i - 5] == 'E')
					&& (a[i - 4] == 'q' || a[i - 4] == 'Q')
					&& (a[i - 3] == 'u' || a[i - 3] == 'U')
					&& (a[i - 2] == 'i' || a[i - 2] == 'I')
					&& (a[i - 1] == 'r' || a[i - 1] == 'R')
					&& (a[i] == 'e' || a[i] == 'E')) {
				matchlen = 7;
				mask |= ACTION_REQUIRE;
			}
			else
				if (i >= 6 && (a[i - 6] == 'p' || a[i - 6] == 'P')
						&& (a[i - 5] == 'r' || a[i - 5] == 'R')
						&& (a[i - 4] == 'o' || a[i - 4] == 'O')
						&& (a[i - 3] == 'v' || a[i - 3] == 'V')
						&& (a[i - 2] == 'i' || a[i - 2] == 'I')
						&& (a[i - 1] == 'd' || a[i - 1] == 'D')
						&& (a[i] == 'e' || a[i] == 'E')) {
					matchlen = 7;
					mask |= ACTION_PROVIDE;
				}
				else {
					// parse error
					throw new IllegalArgumentException("invalid permission: "
							+ actions);
				}

			// make sure we didn't just match the tail of a word
			// like "ackbarfprovide". Also, skip to the comma.
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
	 * Parse filter string into a Filter object.
	 * 
	 * @param filterString The filter string to parse.
	 * @return a Filter for this bundle. If the specified filterString is not a
	 *         filter expression, then {@code null} is returned.
	 * @throws IllegalArgumentException If the filter syntax is invalid.
	 */
	private static Filter parseFilter(String filterString) {
		filterString = filterString.trim();
		if (filterString.charAt(0) != '(') {
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
	 * Determines if a {@code CapabilityPermission} object "implies" the
	 * specified permission.
	 * 
	 * @param p The target permission to check.
	 * @return {@code true} if the specified permission is implied by this
	 *         object; {@code false} otherwise.
	 */
	public boolean implies(Permission p) {
		if (!(p instanceof CapabilityPermission)) {
			return false;
		}
		CapabilityPermission requested = (CapabilityPermission) p;
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
	 * @param requested The requested CapabilityPermission which has already be
	 *        validated as a proper argument. The requested CapabilityPermission
	 *        must not have a filter expression.
	 * @param effective The effective actions with which to start.
	 * @return {@code true} if the specified permission is implied by this
	 *         object; {@code false} otherwise.
	 */
	boolean implies0(CapabilityPermission requested, int effective) {
		/* check actions first - much faster */
		effective |= action_mask;
		final int desired = requested.action_mask;
		if ((effective & desired) != desired) {
			return false;
		}
		/* Get filter if any */
		Filter f = filter;
		if (f == null) {
			return super.implies(requested);
		}
		return f.matches(requested.getProperties());
	}

	/**
	 * Returns the canonical string representation of the actions. Always
	 * returns present actions in the following order: {@code require},
	 * {@code provide}.
	 * 
	 * @return The canonical string representation of the actions.
	 */
	public String getActions() {
		String result = actions;
		if (result == null) {
			StringBuffer sb = new StringBuffer();
			boolean comma = false;

			int mask = action_mask;
			if ((mask & ACTION_REQUIRE) == ACTION_REQUIRE) {
				sb.append(REQUIRE);
				comma = true;
			}

			if ((mask & ACTION_PROVIDE) == ACTION_PROVIDE) {
				if (comma)
					sb.append(',');
				sb.append(PROVIDE);
			}

			actions = result = sb.toString();
		}

		return result;
	}

	/**
	 * Returns a new {@code PermissionCollection} object for storing
	 * {@code CapabilityPermission} objects.
	 * 
	 * @return A new {@code PermissionCollection} object suitable for storing
	 *         {@code CapabilityPermission} objects.
	 */
	public PermissionCollection newPermissionCollection() {
		return new CapabilityPermissionCollection();
	}

	/**
	 * Determines the equality of two CapabilityPermission objects.
	 * 
	 * Checks that specified object has the same name and action as this
	 * {@code CapabilityPermission}.
	 * 
	 * @param obj The object to test for equality.
	 * @return true if obj is a {@code CapabilityPermission}, and has the same
	 *         name and actions as this {@code CapabilityPermission} object;
	 *         {@code false} otherwise.
	 */
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof CapabilityPermission)) {
			return false;
		}

		CapabilityPermission cp = (CapabilityPermission) obj;

		return (action_mask == cp.action_mask)
				&& getName().equals(cp.getName())
				&& ((attributes == cp.attributes) || ((attributes != null) && (attributes
						.equals(cp.attributes))))
				&& ((bundle == cp.bundle) || ((bundle != null) && bundle
						.equals(cp.bundle)));
	}

	/**
	 * Returns the hash code value for this object.
	 * 
	 * @return Hash code value for this object.
	 */
	public int hashCode() {
		int h = 31 * 17 + getName().hashCode();
		h = 31 * h + getActions().hashCode();
		if (attributes != null) {
			h = 31 * h + attributes.hashCode();
		}
		if (bundle != null) {
			h = 31 * h + bundle.hashCode();
		}
		return h;
	}

	/**
	 * WriteObject is called to save the state of this permission to a stream.
	 * The actions are serialized, and the superclass takes care of the name.
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
		setTransients(getName(), parseActions(actions));
	}

	/**
	 * Called by {@code <@link CapabilityPermission#implies(Permission)>}. This
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
		final Map<String, Object> props = new HashMap<String, Object>(5);
		props.put("capability.namespace", getName());
		if (bundle == null) {
			return properties = props;
		}
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			public Object run() {
				props.put("id", new Long(bundle.getBundleId()));
				props.put("location", bundle.getLocation());
				String name = bundle.getSymbolicName();
				if (name != null) {
					props.put("name", name);
				}
				SignerProperty signer = new SignerProperty(bundle);
				if (signer.isBundleSigned()) {
					props.put("signer", signer);
				}
				return null;
			}
		});
		return properties = new Properties(props, attributes);
	}

	static private final class Properties extends AbstractMap<String, Object> {
		private final Map<String, Object>							properties;
		private final Map<String, Object>							attributes;
		private transient volatile Set<Map.Entry<String, Object>>	entries;

		Properties(Map<String, Object> properties,
				Map<String, Object> attributes) {
			this.properties = properties;
			this.attributes = attributes;
			entries = null;
		}

		public Object get(Object k) {
			if (!(k instanceof String)) {
				return null;
			}
			String key = (String) k;
			if (key.charAt(0) == '@') {
				return attributes.get(key.substring(1));
			}
			Object value = properties.get(key);
			if (value != null) { // fall back to service properties
				return value;
			}
			return attributes.get(key);
		}

		public Set<Map.Entry<String, Object>> entrySet() {
			if (entries != null) {
				return entries;
			}
			Set<Map.Entry<String, Object>> all = new HashSet<Map.Entry<String, Object>>(
					attributes.size() + properties.size());
			all.addAll(attributes.entrySet());
			all.addAll(properties.entrySet());
			return entries = Collections.unmodifiableSet(all);
		}
	}
}

/**
 * Stores a set of CapabilityPermission permissions.
 * 
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 */
final class CapabilityPermissionCollection extends PermissionCollection {
	static final long							serialVersionUID	= -615322242639008920L;

	/**
	 * Table of permissions.
	 * 
	 * @serial
	 * @GuardedBy this
	 */
	private Map<String, CapabilityPermission>	permissions;

	/**
	 * Boolean saying if "*" is in the collection.
	 * 
	 * @serial
	 * @GuardedBy this
	 */
	private boolean								all_allowed;

	/**
	 * Table of permissions with filter expressions.
	 * 
	 * @serial
	 * @GuardedBy this
	 */
	private Map<String, CapabilityPermission>	filterPermissions;

	/**
	 * Creates an empty CapabilityPermissionCollection object.
	 */
	public CapabilityPermissionCollection() {
		permissions = new HashMap<String, CapabilityPermission>();
		all_allowed = false;
	}

	/**
	 * Adds a permission to this permission collection.
	 * 
	 * @param permission The Permission object to add.
	 * @throws IllegalArgumentException If the specified permission is not a
	 *         CapabilityPermission object.
	 * @throws SecurityException If this {@code CapabilityPermissionCollection}
	 *         object has been marked read-only.
	 */
	public void add(final Permission permission) {
		if (!(permission instanceof CapabilityPermission)) {
			throw new IllegalArgumentException("invalid permission: "
					+ permission);
		}
		if (isReadOnly()) {
			throw new SecurityException("attempt to add a Permission to a "
					+ "readonly PermissionCollection");
		}

		final CapabilityPermission cp = (CapabilityPermission) permission;
		if (cp.bundle != null) {
			throw new IllegalArgumentException("cannot add to collection: "
					+ cp);
		}

		final String name = cp.getName();
		final Filter f = cp.filter;
		synchronized (this) {
			/* select the bucket for the permission */
			Map<String, CapabilityPermission> pc;
			if (f != null) {
				pc = filterPermissions;
				if (pc == null) {
					filterPermissions = pc = new HashMap<String, CapabilityPermission>();
				}
			}
			else {
				pc = permissions;
			}
			final CapabilityPermission existing = pc.get(name);

			if (existing != null) {
				final int oldMask = existing.action_mask;
				final int newMask = cp.action_mask;
				if (oldMask != newMask) {
					pc.put(name, new CapabilityPermission(name, oldMask
							| newMask));
				}
			}
			else {
				pc.put(name, cp);
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
	 * {@code permission}.
	 * 
	 * @param permission The Permission object to compare.
	 * @return {@code true} if {@code permission} is a proper subset of a
	 *         permission in the set; {@code false} otherwise.
	 */
	public boolean implies(final Permission permission) {
		if (!(permission instanceof CapabilityPermission)) {
			return false;
		}
		final CapabilityPermission requested = (CapabilityPermission) permission;
		/* if requested permission has a filter, then it is an invalid argument */
		if (requested.filter != null) {
			return false;
		}

		String requestedName = requested.getName();
		final int desired = requested.action_mask;
		int effective = CapabilityPermission.ACTION_NONE;

		Collection<CapabilityPermission> perms;
		synchronized (this) {
			Map<String, CapabilityPermission> pc = permissions;
			CapabilityPermission cp;
			/* short circuit if the "*" Permission was added */
			if (all_allowed) {
				cp = pc.get("*");
				if (cp != null) {
					effective |= cp.action_mask;
					if ((effective & desired) == desired) {
						return true;
					}
				}
			}

			/*
			 * strategy: Check for full match first. Then work our way up the
			 * name looking for matches on a.b.*
			 */
			cp = pc.get(requestedName);
			if (cp != null) {
				/* we have a direct hit! */
				effective |= cp.action_mask;
				if ((effective & desired) == desired) {
					return true;
				}
			}
			/* work our way up the tree... */
			int last;
			int offset = requestedName.length() - 1;
			while ((last = requestedName.lastIndexOf(".", offset)) != -1) {
				requestedName = requestedName.substring(0, last + 1) + "*";
				cp = pc.get(requestedName);
				if (cp != null) {
					effective |= cp.action_mask;
					if ((effective & desired) == desired) {
						return true;
					}
				}
				offset = last - 1;
			}
			/*
			 * we don't have to check for "*" as it was already checked before
			 * we were called.
			 */
			pc = filterPermissions;
			if (pc == null) {
				return false;
			}
			perms = pc.values();
		}
		/* iterate one by one over filteredPermissions */
		for (CapabilityPermission perm : perms) {
			if (perm.implies0(requested, effective)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns an enumeration of all the {@code CapabilityPermission} objects in
	 * the container.
	 * 
	 * @return Enumeration of all the CapabilityPermission objects.
	 */
	public synchronized Enumeration<Permission> elements() {
		List<Permission> all = new ArrayList<Permission>(permissions.values());
		Map<String, CapabilityPermission> pc = filterPermissions;
		if (pc != null) {
			all.addAll(pc.values());
		}
		return Collections.enumeration(all);
	}

	/* serialization logic */
	private static final ObjectStreamField[]	serialPersistentFields	= {
			new ObjectStreamField("permissions", HashMap.class),
			new ObjectStreamField("all_allowed", Boolean.TYPE),
			new ObjectStreamField("filterPermissions", HashMap.class)	};

	private synchronized void writeObject(ObjectOutputStream out)
			throws IOException {
		ObjectOutputStream.PutField pfields = out.putFields();
		pfields.put("permissions", permissions);
		pfields.put("all_allowed", all_allowed);
		pfields.put("filterPermissions", filterPermissions);
		out.writeFields();
	}

	private synchronized void readObject(java.io.ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		ObjectInputStream.GetField gfields = in.readFields();
		HashMap<String, CapabilityPermission> p = (HashMap<String, CapabilityPermission>) gfields
				.get("permissions", null);
		permissions = p;
		all_allowed = gfields.get("all_allowed", false);
		HashMap<String, CapabilityPermission> fp = (HashMap<String, CapabilityPermission>) gfields
				.get("filterPermissions", null);
		filterPermissions = fp;
	}
}
