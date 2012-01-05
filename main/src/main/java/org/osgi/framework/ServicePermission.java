/*
 * Copyright (c) OSGi Alliance (2000, 2010). All Rights Reserved.
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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A bundle's authority to register or get a service.
 * <ul>
 * <li>The {@code register} action allows a bundle to register a service on
 * the specified names.
 * <li>The {@code get} action allows a bundle to detect a service and get
 * it.
 * </ul>
 * Permission to get a service is required in order to detect events regarding
 * the service. Untrusted bundles should not be able to detect the presence of
 * certain services unless they have the appropriate
 * {@code ServicePermission} to get the specific service.
 * 
 * @ThreadSafe
 * @version $Id: 1b6ee9543f4cbc16add8dc8c40dfa9a6dfee7aa2 $
 */

public final class ServicePermission extends BasicPermission {
	static final long			serialVersionUID	= -7662148639076511574L;
	/**
	 * The action string {@code get}.
	 */
	public final static String	GET					= "get";
	/**
	 * The action string {@code register}.
	 */
	public final static String	REGISTER			= "register";

	private final static int	ACTION_GET			= 0x00000001;
	private final static int	ACTION_REGISTER		= 0x00000002;
	private final static int	ACTION_ALL			= ACTION_GET
															| ACTION_REGISTER;
	final static int						ACTION_NONE			= 0;

	/**
	 * The actions mask.
	 */
	transient int							action_mask;

	/**
	 * The actions in canonical form.
	 * 
	 * @serial
	 */
	private volatile String		actions				= null;

	/**
	 * The service used by this ServicePermission. Must be null if not
	 * constructed with a service.
	 */
	transient final ServiceReference< ? >					service;

	/**
	 * The object classes for this ServicePermission. Must be null if not
	 * constructed with a service.
	 */
	transient final String[]				objectClass;

	/**
	 * If this ServicePermission was constructed with a filter, this holds a
	 * Filter matching object used to evaluate the filter in implies.
	 */
	transient Filter						filter;

	/**
	 * This map holds the properties of the permission, used to match a filter
	 * in implies. This is not initialized until necessary, and then cached in
	 * this object.
	 */
	private transient volatile Map<String, Object>	properties;

	/**
	 * True if constructed with a name and the name is "*" or ends with ".*".
	 */
	private transient boolean				wildcard;

	/**
	 * If constructed with a name and the name ends with ".*", this contains the
	 * name without the final "*".
	 */
	private transient String				prefix;

	/**
	 * Create a new ServicePermission.
	 * 
	 * <p>
	 * The name of the service is specified as a fully qualified class name.
	 * Wildcards may be used.
	 * 
	 * <pre>
	 * name ::= &lt;class name&gt; | &lt;class name ending in &quot;.*&quot;&gt; | *
	 * </pre>
	 * 
	 * Examples:
	 * 
	 * <pre>
	 * org.osgi.service.http.HttpService
	 * org.osgi.service.http.*
	 * *
	 * </pre>
	 * 
	 * For the {@code get} action, the name can also be a filter
	 * expression. The filter gives access to the service properties as well as
	 * the following attributes:
	 * <ul>
	 * <li>signer - A Distinguished Name chain used to sign the bundle
	 * publishing the service. Wildcards in a DN are not matched according to
	 * the filter string rules, but according to the rules defined for a DN
	 * chain.</li>
	 * <li>location - The location of the bundle publishing the service.</li>
	 * <li>id - The bundle ID of the bundle publishing the service.</li>
	 * <li>name - The symbolic name of the bundle publishing the service.</li>
	 * </ul>
	 * Since the above attribute names may conflict with service property names
	 * used by a service, you can prefix an attribute name with '@' in the
	 * filter expression to match against the service property and not one of
	 * the above attributes. Filter attribute names are processed in a case
	 * sensitive manner unless the attribute references a service property.
	 * Service properties names are case insensitive.
	 * 
	 * <p>
	 * There are two possible actions: {@code get} and
	 * {@code register}. The {@code get} permission allows the owner
	 * of this permission to obtain a service with this name. The
	 * {@code register} permission allows the bundle to register a service
	 * under that name.
	 * 
	 * @param name The service class name
	 * @param actions {@code get},{@code register} (canonical order)
	 * @throws IllegalArgumentException If the specified name is a filter
	 *         expression and either the specified action is not
	 *         {@code get} or the filter has an invalid syntax.
	 */
	public ServicePermission(String name, String actions) {
		this(name, parseActions(actions));
		if ((filter != null)
				&& ((action_mask & ACTION_ALL) != ACTION_GET)) {
			throw new IllegalArgumentException(
					"invalid action string for filter expression");
		}
	}

	/**
	 * Creates a new requested {@code ServicePermission} object to be used
	 * by code that must perform {@code checkPermission} for the
	 * {@code get} action. {@code ServicePermission} objects created
	 * with this constructor cannot be added to a {@code ServicePermission}
	 * permission collection.
	 * 
	 * @param reference The requested service.
	 * @param actions The action {@code get}.
	 * @throws IllegalArgumentException If the specified action is not
	 *         {@code get} or reference is {@code null}.
	 * @since 1.5
	 */
	public ServicePermission(ServiceReference< ? > reference, String actions) {
		super(createName(reference));
		setTransients(null, parseActions(actions));
		this.service = reference;
		this.objectClass = (String[]) reference
				.getProperty(Constants.OBJECTCLASS);
		if ((action_mask & ACTION_ALL) != ACTION_GET) {
			throw new IllegalArgumentException("invalid action string");
		}
	}

	/**
	 * Create a permission name from a ServiceReference
	 * 
	 * @param reference ServiceReference to use to create permission name.
	 * @return permission name.
	 */
	private static String createName(ServiceReference< ? > reference) {
		if (reference == null) {
			throw new IllegalArgumentException("reference must not be null");
		}
		StringBuffer sb = new StringBuffer("(service.id=");
		sb.append(reference.getProperty(Constants.SERVICE_ID));
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Package private constructor used by ServicePermissionCollection.
	 * 
	 * @param name class name
	 * @param mask action mask
	 */
	ServicePermission(String name, int mask) {
		super(name);
		setTransients(parseFilter(name), mask);
		this.service = null;
		this.objectClass = null;
	}

	/**
	 * Called by constructors and when deserialized.
	 * 
	 * @param mask action mask
	 */
	private void setTransients(Filter f, int mask) {
		if ((mask == ACTION_NONE) || ((mask & ACTION_ALL) != mask)) {
			throw new IllegalArgumentException("invalid action string");
		}
		action_mask = mask;
		filter = f;
		if (f == null) {
			String name = getName();
			int l = name.length();
			/* if "*" or endsWith ".*" */
			wildcard = ((name.charAt(l - 1) == '*') && ((l == 1) || (name
					.charAt(l - 2) == '.')));
			if (wildcard && (l > 1)) {
				prefix = name.substring(0, l - 1);
			}
		}
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

			if (i >= 2 && (a[i - 2] == 'g' || a[i - 2] == 'G')
					&& (a[i - 1] == 'e' || a[i - 1] == 'E')
					&& (a[i] == 't' || a[i] == 'T')) {
				matchlen = 3;
				mask |= ACTION_GET;

			}
			else
				if (i >= 7 && (a[i - 7] == 'r' || a[i - 7] == 'R')
						&& (a[i - 6] == 'e' || a[i - 6] == 'E')
						&& (a[i - 5] == 'g' || a[i - 5] == 'G')
						&& (a[i - 4] == 'i' || a[i - 4] == 'I')
						&& (a[i - 3] == 's' || a[i - 3] == 'S')
						&& (a[i - 2] == 't' || a[i - 2] == 'T')
						&& (a[i - 1] == 'e' || a[i - 1] == 'E')
						&& (a[i] == 'r' || a[i] == 'R')) {
					matchlen = 8;
					mask |= ACTION_REGISTER;

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
	 * Determines if a {@code ServicePermission} object "implies" the
	 * specified permission.
	 * 
	 * @param p The target permission to check.
	 * @return {@code true} if the specified permission is implied by this
	 *         object; {@code false} otherwise.
	 */
	public boolean implies(Permission p) {
		if (!(p instanceof ServicePermission)) {
			return false;
		}
		ServicePermission requested = (ServicePermission) p;
		if (service != null) {
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
	 * @param requested The requested ServicePermission which has already be
	 *        validated as a proper argument. The requested ServicePermission
	 *        must not have a filter expression.
	 * @param effective The effective actions with which to start.
	 * @return {@code true} if the specified permission is implied by this
	 *         object; {@code false} otherwise.
	 */
	boolean implies0(ServicePermission requested, int effective) {
		/* check actions first - much faster */
		effective |= action_mask;
		final int desired = requested.action_mask;
		if ((effective & desired) != desired) {
			return false;
		}
		/* we have name of "*" */
		if (wildcard && (prefix == null)) {
			return true;
		}
		/* if we have a filter */
		Filter f = filter;
		if (f != null) {
			return f.matches(requested.getProperties());
		}
		/* if requested permission not created with ServiceReference */
		String[] requestedNames = requested.objectClass;
		if (requestedNames == null) {
			return super.implies(requested);
		}
		/* requested permission created with ServiceReference */
		if (wildcard) {
			int pl = prefix.length();
			for (int i = 0, l = requestedNames.length; i < l; i++) {
				String requestedName = requestedNames[i];
				if ((requestedName.length() > pl)
						&& requestedName.startsWith(prefix)) {
					return true;
				}
			}
		}
		else {
			String name = getName();
			for (int i = 0, l = requestedNames.length; i < l; i++) {
				if (requestedNames[i].equals(name)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the canonical string representation of the actions. Always
	 * returns present actions in the following order: {@code get},
	 * {@code register}.
	 * 
	 * @return The canonical string representation of the actions.
	 */
	public String getActions() {
		String result = actions;
		if (result == null) {
			StringBuffer sb = new StringBuffer();
			boolean comma = false;

			int mask = action_mask;
			if ((mask & ACTION_GET) == ACTION_GET) {
				sb.append(GET);
				comma = true;
			}

			if ((mask & ACTION_REGISTER) == ACTION_REGISTER) {
				if (comma)
					sb.append(',');
				sb.append(REGISTER);
			}

			actions = result = sb.toString();
		}

		return result;
	}

	/**
	 * Returns a new {@code PermissionCollection} object for storing
	 * {@code ServicePermission} objects.
	 * 
	 * @return A new {@code PermissionCollection} object suitable for storing
	 *         {@code ServicePermission} objects.
	 */
	public PermissionCollection newPermissionCollection() {
		return new ServicePermissionCollection();
	}

	/**
	 * Determines the equality of two ServicePermission objects.
	 * 
	 * Checks that specified object has the same class name and action as this
	 * {@code ServicePermission}.
	 * 
	 * @param obj The object to test for equality.
	 * @return true if obj is a {@code ServicePermission}, and has the same
	 *         class name and actions as this {@code ServicePermission}
	 *         object; {@code false} otherwise.
	 */
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof ServicePermission)) {
			return false;
		}

		ServicePermission sp = (ServicePermission) obj;

		return (action_mask == sp.action_mask)
				&& getName().equals(sp.getName())
				&& ((service == sp.service) || ((service != null) && (service
						.compareTo(sp.service) == 0)));
	}

	/**
	 * Returns the hash code value for this object.
	 * 
	 * @return Hash code value for this object.
	 */
	public int hashCode() {
		int h = 31 * 17 + getName().hashCode();
		h = 31 * h + getActions().hashCode();
		if (service != null) {
			h = 31 * h + service.hashCode();
		}
		return h;
	}

	/**
	 * WriteObject is called to save the state of this permission to a stream.
	 * The actions are serialized, and the superclass takes care of the name.
	 */
	private synchronized void writeObject(java.io.ObjectOutputStream s)
			throws IOException {
		if (service != null) {
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
	 * Called by {@code <@link ServicePermission#implies(Permission)>}. This
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
		if (service == null) {
			result = new HashMap<String, Object>(1);
			result.put(Constants.OBJECTCLASS, new String[] {getName()});
			return properties = result;
		}
		final Map<String, Object> props = new HashMap<String, Object>(4);
		final Bundle bundle = service.getBundle();
		if (bundle != null) {
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
		}
		return properties = new Properties(props, service);
	}
	
	static private final class Properties extends AbstractMap<String, Object> {
		private final Map<String, Object>	properties;
		private final ServiceReference< ? >	service;
		private transient volatile Set<Map.Entry<String, Object>>	entries;

		Properties(Map<String, Object> properties, ServiceReference< ? > service) {
			this.properties = properties;
			this.service = service;
			entries = null;
		}

		public Object get(Object k) {
			if (!(k instanceof String)) {
				return null;
			}
			String key = (String) k;
			if (key.charAt(0) == '@') {
				return service.getProperty(key.substring(1));
			}
			Object value = properties.get(key);
			if (value != null) { // fall back to service properties
				return value;
			}
			return service.getProperty(key);
		}

		public Set<Map.Entry<String, Object>> entrySet() {
			if (entries != null) {
				return entries;
			}
			Set<Map.Entry<String, Object>> all = new HashSet<Map.Entry<String, Object>>(
					properties.entrySet());
			add: for (String key : service.getPropertyKeys()) {
				for (String k : properties.keySet()) {
					if (key.equalsIgnoreCase(k)) {
						continue add;
					}
				}
				all.add(new Entry(key, service.getProperty(key)));
			}
			return entries = Collections.unmodifiableSet(all);
		}
		
		static private final class Entry implements Map.Entry<String, Object> {
			private final String	k;
			private final Object	v;

			Entry(String key, Object value) {
				this.k = key;
				this.v = value;
			}
			public String getKey() {
				return k;
			}
			public Object getValue() {
				return v;
			}
			public Object setValue(Object value) {
				throw new UnsupportedOperationException();
			}
			public String toString() {
				return k + "=" + v;
			}
			public int hashCode() {
				return ((k == null) ? 0 : k.hashCode())
						^ ((v == null) ? 0 : v.hashCode());
			}
			public boolean equals(Object obj) {
				if (obj == this) {
					return true;
				}
				if (!(obj instanceof Map.Entry)) {
					return false;
				}
				Map.Entry< ? , ? > e = (Map.Entry< ? , ? >) obj;
				final Object key = e.getKey();
				if ((k == key) || ((k != null) && k.equals(key))) {
					final Object value = e.getValue();
					if ((v == value) || ((v != null) && v.equals(value))) {
						return true;
					}
				}
				return false;
			}
		}
	}
}

/**
 * Stores a set of ServicePermission permissions.
 * 
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 */
final class ServicePermissionCollection extends PermissionCollection {
	static final long	serialVersionUID	= 662615640374640621L;
	/**
	 * Table of permissions.
	 * 
	 * @GuardedBy this
	 */
	private transient Map<String, ServicePermission>	permissions;

	/**
	 * Boolean saying if "*" is in the collection.
	 * 
	 * @serial
	 * @GuardedBy this
	 */
	private boolean		all_allowed;

	/**
	 * Table of permissions with filter expressions.
	 * 
	 * @serial
	 * @GuardedBy this
	 */
	private Map<String, ServicePermission>				filterPermissions;

	/**
	 * Creates an empty ServicePermissions object.
	 */
	public ServicePermissionCollection() {
		permissions = new HashMap<String, ServicePermission>();
		all_allowed = false;
	}

	/**
	 * Adds a permission to this permission collection.
	 * 
	 * @param permission The Permission object to add.
	 * @throws IllegalArgumentException If the specified permission is not a
	 *         ServicePermission object.
	 * @throws SecurityException If this
	 *         {@code ServicePermissionCollection} object has been marked
	 *         read-only.
	 */
	public void add(final Permission permission) {
		if (!(permission instanceof ServicePermission)) {
			throw new IllegalArgumentException("invalid permission: "
					+ permission);
		}
		if (isReadOnly()) {
			throw new SecurityException("attempt to add a Permission to a "
					+ "readonly PermissionCollection");
		}

		final ServicePermission sp = (ServicePermission) permission;
		if (sp.service != null) {
			throw new IllegalArgumentException("cannot add to collection: "
					+ sp);
		}
		
		final String name = sp.getName();
		final Filter f = sp.filter;
		synchronized (this) {
			/* select the bucket for the permission */
			Map<String, ServicePermission> pc;
			if (f != null) {
				pc = filterPermissions;
				if (pc == null) {
					filterPermissions = pc = new HashMap<String, ServicePermission>();
				}
			}
			else {
				pc = permissions;
			}
			final ServicePermission existing = pc.get(name);
			
			if (existing != null) {
				final int oldMask = existing.action_mask;
				final int newMask = sp.action_mask;
				if (oldMask != newMask) {
					pc
							.put(name, new ServicePermission(name, oldMask
							| newMask));
				}
			}
			else {
				pc.put(name, sp);
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
	 * @return {@code true} if {@code permission} is a proper
	 *         subset of a permission in the set; {@code false}
	 *         otherwise.
	 */
	public boolean implies(final Permission permission) {
		if (!(permission instanceof ServicePermission)) {
			return false;
		}
		final ServicePermission requested = (ServicePermission) permission;
		/* if requested permission has a filter, then it is an invalid argument */
		if (requested.filter != null) {
			return false;
		}

		int effective = ServicePermission.ACTION_NONE;
		Collection<ServicePermission> perms;
		synchronized (this) {
			final int desired = requested.action_mask;
			/* short circuit if the "*" Permission was added */
			if (all_allowed) {
				ServicePermission sp = permissions.get("*");
				if (sp != null) {
					effective |= sp.action_mask;
					if ((effective & desired) == desired) {
						return true;
					}
				}
			}
			
			String[] requestedNames = requested.objectClass;
			/* if requested permission not created with ServiceReference */
			if (requestedNames == null) {
				effective |= effective(requested.getName(), desired, effective);
				if ((effective & desired) == desired) {
					return true;
				}
			}
			/* requested permission created with ServiceReference */
			else {
				for (int i = 0, l = requestedNames.length; i < l; i++) {
					if ((effective(requestedNames[i], desired, effective) & desired) == desired) {
						return true;
					}
				}
			}
			Map<String, ServicePermission> pc = filterPermissions;
			if (pc == null) {
				return false;
			}
			perms = pc.values();
		}
		
		/* iterate one by one over filteredPermissions */
		for (ServicePermission perm : perms) {
			if (perm.implies0(requested, effective)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Consult permissions map to compute the effective permission for the
	 * requested permission name.
	 * 
	 * @param requestedName The requested service name.
	 * @param desired The desired actions.
	 * @param effective The effective actions.
	 * @return The new effective actions.
	 */
	private int effective(String requestedName, final int desired,
			int effective) {
		final Map<String, ServicePermission> pc = permissions;
		ServicePermission sp = pc.get(requestedName);
		// strategy:
		// Check for full match first. Then work our way up the
		// name looking for matches on a.b.*
		if (sp != null) {
			// we have a direct hit!
			effective |= sp.action_mask;
			if ((effective & desired) == desired) {
				return effective;
			}
		}
		// work our way up the tree...
		int last;
		int offset = requestedName.length() - 1;
		while ((last = requestedName.lastIndexOf(".", offset)) != -1) {
			requestedName = requestedName.substring(0, last + 1) + "*";
			sp = pc.get(requestedName);
			if (sp != null) {
				effective |= sp.action_mask;
				if ((effective & desired) == desired) {
					return effective;
				}
			}
			offset = last - 1;
		}
		/*
		 * we don't have to check for "*" as it was already checked before we
		 * were called.
		 */
		return effective;
	}
	
	/**
	 * Returns an enumeration of all the {@code ServicePermission}
	 * objects in the container.
	 * 
	 * @return Enumeration of all the ServicePermission objects.
	 */
	public synchronized Enumeration<Permission> elements() {
		List<Permission> all = new ArrayList<Permission>(permissions.values());
		Map<String, ServicePermission> pc = filterPermissions;
		if (pc != null) {
			all.addAll(pc.values());
		}
		return Collections.enumeration(all);
	}
	
	/* serialization logic */
	private static final ObjectStreamField[]	serialPersistentFields	= {
			new ObjectStreamField("permissions", Hashtable.class),
			new ObjectStreamField("all_allowed", Boolean.TYPE),
			new ObjectStreamField("filterPermissions", HashMap.class)	};

	private synchronized void writeObject(ObjectOutputStream out)
			throws IOException {
		Hashtable<String, ServicePermission> hashtable = new Hashtable<String, ServicePermission>(
				permissions);
		ObjectOutputStream.PutField pfields = out.putFields();
		pfields.put("permissions", hashtable);
		pfields.put("all_allowed", all_allowed);
		pfields.put("filterPermissions", filterPermissions);
		out.writeFields();
	}

	private synchronized void readObject(java.io.ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		ObjectInputStream.GetField gfields = in.readFields();
		Hashtable<String, ServicePermission> hashtable = (Hashtable<String, ServicePermission>) gfields
				.get("permissions", null);
		permissions = new HashMap<String, ServicePermission>(hashtable);
		all_allowed = gfields.get("all_allowed", false);
		HashMap<String, ServicePermission> fp = (HashMap<String, ServicePermission>) gfields
				.get("filterPermissions", null);
		filterPermissions = fp;
	}
}
