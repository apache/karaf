/*
 * Copyright (c) OSGi Alliance (2000, 2009). All Rights Reserved.
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
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

/**
 * A bundle's authority to perform specific privileged administrative operations
 * on or to get sensitive information about a bundle. The actions for this
 * permission are:
 * 
 * <pre>
 *  Action               Methods
 *  class                Bundle.loadClass
 *  execute              Bundle.start
 *                       Bundle.stop
 *                       StartLevel.setBundleStartLevel
 *  extensionLifecycle   BundleContext.installBundle for extension bundles
 *                       Bundle.update for extension bundles
 *                       Bundle.uninstall for extension bundles
 *  lifecycle            BundleContext.installBundle
 *                       Bundle.update
 *                       Bundle.uninstall
 *  listener             BundleContext.addBundleListener for SynchronousBundleListener
 *                       BundleContext.removeBundleListener for SynchronousBundleListener
 *  metadata             Bundle.getHeaders
 *                       Bundle.getLocation
 *  resolve              PackageAdmin.refreshPackages
 *                       PackageAdmin.resolveBundles
 *  resource             Bundle.getResource
 *                       Bundle.getResources
 *                       Bundle.getEntry
 *                       Bundle.getEntryPaths
 *                       Bundle.findEntries
 *                       Bundle resource/entry URL creation
 *  startlevel           StartLevel.setStartLevel
 *                       StartLevel.setInitialBundleStartLevel 
 *  context              Bundle.getBundleContext
 * </pre>
 * 
 * <p>
 * The special action &quot;*&quot; will represent all actions. The
 * <code>resolve</code> action is implied by the <code>class</code>,
 * <code>execute</code> and <code>resource</code> actions.
 * <p>
 * The name of this permission is a filter expression. The filter gives access
 * to the following attributes:
 * <ul>
 * <li>signer - A Distinguished Name chain used to sign a bundle. Wildcards in a
 * DN are not matched according to the filter string rules, but according to the
 * rules defined for a DN chain.</li>
 * <li>location - The location of a bundle.</li>
 * <li>id - The bundle ID of the designated bundle.</li>
 * <li>name - The symbolic name of a bundle.</li>
 * </ul>
 * Filter attribute names are processed in a case sensitive manner.
 * 
 * @ThreadSafe
 * @version $Revision: 7743 $
 */

public final class AdminPermission extends BasicPermission {
	static final long						serialVersionUID			= 307051004521261705L;

	/**
	 * The action string <code>class</code>. The <code>class</code> action
	 * implies the <code>resolve</code> action.
	 * 
	 * @since 1.3
	 */
	public final static String	CLASS						= "class";
	/**
	 * The action string <code>execute</code>. The <code>execute</code> action
	 * implies the <code>resolve</code> action.
	 * 
	 * @since 1.3
	 */
	public final static String	EXECUTE						= "execute";
	/**
	 * The action string <code>extensionLifecycle</code>.
	 * 
	 * @since 1.3
	 */
	public final static String	EXTENSIONLIFECYCLE			= "extensionLifecycle";
	/**
	 * The action string <code>lifecycle</code>.
	 * 
	 * @since 1.3
	 */
	public final static String	LIFECYCLE					= "lifecycle";
	/**
	 * The action string <code>listener</code>.
	 * 
	 * @since 1.3
	 */
	public final static String	LISTENER					= "listener";
	/**
	 * The action string <code>metadata</code>.
	 * 
	 * @since 1.3
	 */
	public final static String	METADATA					= "metadata";
	/**
	 * The action string <code>resolve</code>. The <code>resolve</code> action
	 * is implied by the <code>class</code>, <code>execute</code> and
	 * <code>resource</code> actions.
	 * 
	 * @since 1.3
	 */
	public final static String	RESOLVE						= "resolve";
	/**
	 * The action string <code>resource</code>. The <code>resource</code> action
	 * implies the <code>resolve</code> action.
	 * 
	 * @since 1.3
	 */
	public final static String	RESOURCE					= "resource";
	/**
	 * The action string <code>startlevel</code>.
	 * 
	 * @since 1.3
	 */
	public final static String	STARTLEVEL					= "startlevel";

	/**
	 * The action string <code>context</code>.
	 * 
	 * @since 1.4
	 */
	public final static String	CONTEXT						= "context";

	private final static int	ACTION_CLASS				= 0x00000001;
	private final static int	ACTION_EXECUTE				= 0x00000002;
	private final static int	ACTION_LIFECYCLE			= 0x00000004;
	private final static int	ACTION_LISTENER				= 0x00000008;
	private final static int	ACTION_METADATA				= 0x00000010;
	private final static int	ACTION_RESOLVE				= 0x00000040;
	private final static int	ACTION_RESOURCE				= 0x00000080;
	private final static int	ACTION_STARTLEVEL			= 0x00000100;
	private final static int	ACTION_EXTENSIONLIFECYCLE	= 0x00000200;
	private final static int	ACTION_CONTEXT				= 0x00000400;
	private final static int	ACTION_ALL					= ACTION_CLASS
																	| ACTION_EXECUTE
																	| ACTION_LIFECYCLE
																	| ACTION_LISTENER
																	| ACTION_METADATA
																	| ACTION_RESOLVE
																	| ACTION_RESOURCE
																	| ACTION_STARTLEVEL
																	| ACTION_EXTENSIONLIFECYCLE
																	| ACTION_CONTEXT;
	final static int						ACTION_NONE					= 0;

	/**
	 * The actions in canonical form.
	 * 
	 * @serial
	 */
	private volatile String		actions						= null;

	/**
	 * The actions mask.
	 */
	transient int							action_mask;

	/**
	 * If this AdminPermission was constructed with a filter, this holds a
	 * Filter matching object used to evaluate the filter in implies.
	 */
	transient Filter						filter;

	/**
	 * The bundle governed by this AdminPermission - only used if filter == null
	 */
	transient final Bundle					bundle; 

	/**
	 * This dictionary holds the properties of the permission, used to match a
	 * filter in implies. This is not initialized until necessary, and then
	 * cached in this object.
	 */
	private transient volatile Dictionary	properties;

	/**
	 * ThreadLocal used to determine if we have recursively called
	 * getProperties.
	 */
	private static final ThreadLocal		recurse						= new ThreadLocal();

	/**
	 * Creates a new <code>AdminPermission</code> object that matches all
	 * bundles and has all actions. Equivalent to AdminPermission("*","*");
	 */
	public AdminPermission() {
		this(null, ACTION_ALL); 
	}

	/**
	 * Create a new AdminPermission.
	 * 
	 * This constructor must only be used to create a permission that is going
	 * to be checked.
	 * <p>
	 * Examples:
	 * 
	 * <pre>
	 * (signer=\*,o=ACME,c=US)   
	 * (&amp;(signer=\*,o=ACME,c=US)(name=com.acme.*)(location=http://www.acme.com/bundles/*))
	 * (id&gt;=1)
	 * </pre>
	 * 
	 * <p>
	 * When a signer key is used within the filter expression the signer value
	 * must escape the special filter chars ('*', '(', ')').
	 * <p>
	 * Null arguments are equivalent to "*".
	 * 
	 * @param filter A filter expression that can use signer, location, id, and
	 *        name keys. A value of &quot;*&quot; or <code>null</code> matches
	 *        all bundle. Filter attribute names are processed in a case
	 *        sensitive manner.
	 * @param actions <code>class</code>, <code>execute</code>,
	 *        <code>extensionLifecycle</code>, <code>lifecycle</code>,
	 *        <code>listener</code>, <code>metadata</code>, <code>resolve</code>
	 *        , <code>resource</code>, <code>startlevel</code> or
	 *        <code>context</code>. A value of "*" or <code>null</code>
	 *        indicates all actions.
	 * @throws IllegalArgumentException If the filter has an invalid syntax.
	 */
	public AdminPermission(String filter, String actions) {
		// arguments will be null if called from a PermissionInfo defined with
		// no args
		this(parseFilter(filter), parseActions(actions));
	}

	/**
	 * Creates a new requested <code>AdminPermission</code> object to be used by
	 * the code that must perform <code>checkPermission</code>.
	 * <code>AdminPermission</code> objects created with this constructor cannot
	 * be added to an <code>AdminPermission</code> permission collection.
	 * 
	 * @param bundle A bundle.
	 * @param actions <code>class</code>, <code>execute</code>,
	 *        <code>extensionLifecycle</code>, <code>lifecycle</code>,
	 *        <code>listener</code>, <code>metadata</code>, <code>resolve</code>
	 *        , <code>resource</code>, <code>startlevel</code>,
	 *        <code>context</code>. A value of "*" or <code>null</code>
	 *        indicates all actions.
	 * @since 1.3
	 */
	public AdminPermission(Bundle bundle, String actions) {
		super(createName(bundle));
		setTransients(null, parseActions(actions));
		this.bundle = bundle;
	}

	/**
	 * Create a permission name from a Bundle
	 * 
	 * @param bundle Bundle to use to create permission name.
	 * @return permission name.
	 */
	private static String createName(Bundle bundle) {
		if (bundle == null) {
			throw new IllegalArgumentException("bundle must not be null");
		}
		StringBuffer sb = new StringBuffer("(id=");
		sb.append(bundle.getBundleId());
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Package private constructor used by AdminPermissionCollection.
	 * 
	 * @param filter name filter or <code>null</code> for wildcard.
	 * @param mask action mask
	 */
	AdminPermission(Filter filter, int mask) {
		super((filter == null) ? "*" : filter.toString());
		setTransients(filter, mask);
		this.bundle = null;
	}

	/**
	 * Called by constructors and when deserialized.
	 * 
	 * @param filter Permission's filter or <code>null</code> for wildcard.
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
		if ((actions == null) || actions.equals("*")) {
			return ACTION_ALL;
		}
	
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
	
			if (i >= 4 && (a[i - 4] == 'c' || a[i - 4] == 'C')
					&& (a[i - 3] == 'l' || a[i - 3] == 'L')
					&& (a[i - 2] == 'a' || a[i - 2] == 'A')
					&& (a[i - 1] == 's' || a[i - 1] == 'S')
					&& (a[i] == 's' || a[i] == 'S')) {
				matchlen = 5;
				mask |= ACTION_CLASS | ACTION_RESOLVE;
	
			}
			else
				if (i >= 6 && (a[i - 6] == 'e' || a[i - 6] == 'E')
						&& (a[i - 5] == 'x' || a[i - 5] == 'X')
						&& (a[i - 4] == 'e' || a[i - 4] == 'E')
						&& (a[i - 3] == 'c' || a[i - 3] == 'C')
						&& (a[i - 2] == 'u' || a[i - 2] == 'U')
						&& (a[i - 1] == 't' || a[i - 1] == 'T')
						&& (a[i] == 'e' || a[i] == 'E')) {
					matchlen = 7;
					mask |= ACTION_EXECUTE | ACTION_RESOLVE;
	
				}
				else
					if (i >= 17 && (a[i - 17] == 'e' || a[i - 17] == 'E')
							&& (a[i - 16] == 'x' || a[i - 16] == 'X')
							&& (a[i - 15] == 't' || a[i - 15] == 'T')
							&& (a[i - 14] == 'e' || a[i - 14] == 'E')
							&& (a[i - 13] == 'n' || a[i - 13] == 'N')
							&& (a[i - 12] == 's' || a[i - 12] == 'S')
							&& (a[i - 11] == 'i' || a[i - 11] == 'I')
							&& (a[i - 10] == 'o' || a[i - 10] == 'O')
							&& (a[i - 9] == 'n' || a[i - 9] == 'N')
							&& (a[i - 8] == 'l' || a[i - 8] == 'L')
							&& (a[i - 7] == 'i' || a[i - 7] == 'I')
							&& (a[i - 6] == 'f' || a[i - 6] == 'F')
							&& (a[i - 5] == 'e' || a[i - 5] == 'E')
							&& (a[i - 4] == 'c' || a[i - 4] == 'C')
							&& (a[i - 3] == 'y' || a[i - 3] == 'Y')
							&& (a[i - 2] == 'c' || a[i - 2] == 'C')
							&& (a[i - 1] == 'l' || a[i - 1] == 'L')
							&& (a[i] == 'e' || a[i] == 'E')) {
						matchlen = 18;
						mask |= ACTION_EXTENSIONLIFECYCLE;
	
					}
					else
						if (i >= 8 && (a[i - 8] == 'l' || a[i - 8] == 'L')
								&& (a[i - 7] == 'i' || a[i - 7] == 'I')
								&& (a[i - 6] == 'f' || a[i - 6] == 'F')
								&& (a[i - 5] == 'e' || a[i - 5] == 'E')
								&& (a[i - 4] == 'c' || a[i - 4] == 'C')
								&& (a[i - 3] == 'y' || a[i - 3] == 'Y')
								&& (a[i - 2] == 'c' || a[i - 2] == 'C')
								&& (a[i - 1] == 'l' || a[i - 1] == 'L')
								&& (a[i] == 'e' || a[i] == 'E')) {
							matchlen = 9;
							mask |= ACTION_LIFECYCLE;
	
						}
						else
							if (i >= 7 && (a[i - 7] == 'l' || a[i - 7] == 'L')
									&& (a[i - 6] == 'i' || a[i - 6] == 'I')
									&& (a[i - 5] == 's' || a[i - 5] == 'S')
									&& (a[i - 4] == 't' || a[i - 4] == 'T')
									&& (a[i - 3] == 'e' || a[i - 3] == 'E')
									&& (a[i - 2] == 'n' || a[i - 2] == 'N')
									&& (a[i - 1] == 'e' || a[i - 1] == 'E')
									&& (a[i] == 'r' || a[i] == 'R')) {
								matchlen = 8;
								mask |= ACTION_LISTENER;
	
							}
							else
								if (i >= 7
										&& (a[i - 7] == 'm' || a[i - 7] == 'M')
										&& (a[i - 6] == 'e' || a[i - 6] == 'E')
										&& (a[i - 5] == 't' || a[i - 5] == 'T')
										&& (a[i - 4] == 'a' || a[i - 4] == 'A')
										&& (a[i - 3] == 'd' || a[i - 3] == 'D')
										&& (a[i - 2] == 'a' || a[i - 2] == 'A')
										&& (a[i - 1] == 't' || a[i - 1] == 'T')
										&& (a[i] == 'a' || a[i] == 'A')) {
									matchlen = 8;
									mask |= ACTION_METADATA;
	
								}
								else
									if (i >= 6
											&& (a[i - 6] == 'r' || a[i - 6] == 'R')
											&& (a[i - 5] == 'e' || a[i - 5] == 'E')
											&& (a[i - 4] == 's' || a[i - 4] == 'S')
											&& (a[i - 3] == 'o' || a[i - 3] == 'O')
											&& (a[i - 2] == 'l' || a[i - 2] == 'L')
											&& (a[i - 1] == 'v' || a[i - 1] == 'V')
											&& (a[i] == 'e' || a[i] == 'E')) {
										matchlen = 7;
										mask |= ACTION_RESOLVE;
	
									}
									else
										if (i >= 7
												&& (a[i - 7] == 'r' || a[i - 7] == 'R')
												&& (a[i - 6] == 'e' || a[i - 6] == 'E')
												&& (a[i - 5] == 's' || a[i - 5] == 'S')
												&& (a[i - 4] == 'o' || a[i - 4] == 'O')
												&& (a[i - 3] == 'u' || a[i - 3] == 'U')
												&& (a[i - 2] == 'r' || a[i - 2] == 'R')
												&& (a[i - 1] == 'c' || a[i - 1] == 'C')
												&& (a[i] == 'e' || a[i] == 'E')) {
											matchlen = 8;
											mask |= ACTION_RESOURCE
													| ACTION_RESOLVE;
	
										}
										else
											if (i >= 9
													&& (a[i - 9] == 's' || a[i - 9] == 'S')
													&& (a[i - 8] == 't' || a[i - 8] == 'T')
													&& (a[i - 7] == 'a' || a[i - 7] == 'A')
													&& (a[i - 6] == 'r' || a[i - 6] == 'R')
													&& (a[i - 5] == 't' || a[i - 5] == 'T')
													&& (a[i - 4] == 'l' || a[i - 4] == 'L')
													&& (a[i - 3] == 'e' || a[i - 3] == 'E')
													&& (a[i - 2] == 'v' || a[i - 2] == 'V')
													&& (a[i - 1] == 'e' || a[i - 1] == 'E')
													&& (a[i] == 'l' || a[i] == 'L')) {
												matchlen = 10;
												mask |= ACTION_STARTLEVEL;
	
											}
											else
												if (i >= 6
														&& (a[i - 6] == 'c' || a[i - 6] == 'C')
														&& (a[i - 5] == 'o' || a[i - 5] == 'O')
														&& (a[i - 4] == 'n' || a[i - 4] == 'N')
														&& (a[i - 3] == 't' || a[i - 3] == 'T')
														&& (a[i - 2] == 'e' || a[i - 2] == 'E')
														&& (a[i - 1] == 'x' || a[i - 1] == 'X')
														&& (a[i] == 't' || a[i] == 'T')) {
													matchlen = 7;
													mask |= ACTION_CONTEXT;
	
												}
												else
													if (i >= 0 &&
	
													(a[i] == '*')) {
														matchlen = 1;
														mask |= ACTION_ALL;
	
													}
													else {
														// parse error
														throw new IllegalArgumentException(
																"invalid permission: "
																		+ actions); 
													}
	
			// make sure we didn't just match the tail of a word
			// like "ackbarfstartlevel". Also, skip to the comma.
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
			throw new IllegalArgumentException("invalid permission: " + 
					actions);
		}
	
		return mask;
	}

	/**
	 * Parse filter string into a Filter object.
	 * 
	 * @param filterString The filter string to parse.
	 * @return a Filter for this bundle. If the specified filterString is
	 *         <code>null</code> or equals "*", then <code>null</code> is
	 *         returned to indicate a wildcard.
	 * @throws IllegalArgumentException If the filter syntax is invalid.
	 */
	private static Filter parseFilter(String filterString) {
		if (filterString == null) {
			return null;
		}
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
	 * Determines if the specified permission is implied by this object. This
	 * method throws an exception if the specified permission was not
	 * constructed with a bundle.
	 * 
	 * <p>
	 * This method returns <code>true</code> if the specified permission is an
	 * AdminPermission AND
	 * <ul>
	 * <li>this object's filter matches the specified permission's bundle ID,
	 * bundle symbolic name, bundle location and bundle signer distinguished
	 * name chain OR</li>
	 * <li>this object's filter is "*"</li>
	 * </ul>
	 * AND this object's actions include all of the specified permission's
	 * actions.
	 * <p>
	 * Special case: if the specified permission was constructed with "*"
	 * filter, then this method returns <code>true</code> if this object's
	 * filter is "*" and this object's actions include all of the specified
	 * permission's actions
	 * 
	 * @param p The requested permission.
	 * @return <code>true</code> if the specified permission is implied by this
	 *         object; <code>false</code> otherwise.
	 */
	public boolean implies(Permission p) {
		if (!(p instanceof AdminPermission)) {
			return false;
		}
		AdminPermission requested = (AdminPermission) p;
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
	 * @param requested The requested AdminPermision which has already be
	 *        validated as a proper argument. The requested AdminPermission must
	 *        not have a filter expression.
	 * @param effective The effective actions with which to start.
	 * @return <code>true</code> if the specified permission is implied by this
	 *         object; <code>false</code> otherwise.
	 */
	boolean implies0(AdminPermission requested, int effective) {
		/* check actions first - much faster */
		effective |= action_mask;
		final int desired = requested.action_mask;
		if ((effective & desired) != desired) {
			return false;
		}
	
		/* Get our filter */
		Filter f = filter;
		if (f == null) {
			// it's "*"
			return true;
		}
		/* is requested a wildcard filter? */
		if (requested.bundle == null) {
			return false;
		}
		Dictionary requestedProperties = requested.getProperties();
		if (requestedProperties == null) {
			/*
			 * If the requested properties are null, then we have detected a
			 * recursion getting the bundle location. So we return true to
			 * permit the bundle location request in the AdminPermission check
			 * up the stack to succeed.
			 */
			return true;
		}
		return f.matchCase(requestedProperties);
	}

	/**
	 * Returns the canonical string representation of the
	 * <code>AdminPermission</code> actions.
	 * 
	 * <p>
	 * Always returns present <code>AdminPermission</code> actions in the
	 * following order: <code>class</code>, <code>execute</code>,
	 * <code>extensionLifecycle</code>, <code>lifecycle</code>,
	 * <code>listener</code>, <code>metadata</code>, <code>resolve</code>,
	 * <code>resource</code>, <code>startlevel</code>, <code>context</code>.
	 * 
	 * @return Canonical string representation of the
	 *         <code>AdminPermission</code> actions.
	 */
	public String getActions() {
		String result = actions;
		if (result == null) {
			StringBuffer sb = new StringBuffer();
	
			int mask = action_mask;
			if ((mask & ACTION_CLASS) == ACTION_CLASS) {
				sb.append(CLASS);
				sb.append(',');
			}
	
			if ((mask & ACTION_EXECUTE) == ACTION_EXECUTE) {
				sb.append(EXECUTE);
				sb.append(',');
			}
	
			if ((mask & ACTION_EXTENSIONLIFECYCLE) == ACTION_EXTENSIONLIFECYCLE) {
				sb.append(EXTENSIONLIFECYCLE);
				sb.append(',');
			}
	
			if ((mask & ACTION_LIFECYCLE) == ACTION_LIFECYCLE) {
				sb.append(LIFECYCLE);
				sb.append(',');
			}
	
			if ((mask & ACTION_LISTENER) == ACTION_LISTENER) {
				sb.append(LISTENER);
				sb.append(',');
			}
	
			if ((mask & ACTION_METADATA) == ACTION_METADATA) {
				sb.append(METADATA);
				sb.append(',');
			}
	
			if ((mask & ACTION_RESOLVE) == ACTION_RESOLVE) {
				sb.append(RESOLVE);
				sb.append(',');
			}
	
			if ((mask & ACTION_RESOURCE) == ACTION_RESOURCE) {
				sb.append(RESOURCE);
				sb.append(',');
			}
	
			if ((mask & ACTION_STARTLEVEL) == ACTION_STARTLEVEL) {
				sb.append(STARTLEVEL);
				sb.append(',');
			}
	
			if ((mask & ACTION_CONTEXT) == ACTION_CONTEXT) {
				sb.append(CONTEXT);
				sb.append(',');
			}
	
			// remove trailing comma
			if (sb.length() > 0) {
				sb.setLength(sb.length() - 1);
			}
	
			actions = result = sb.toString();
		}
		return result;
	}

	/**
	 * Returns a new <code>PermissionCollection</code> object suitable for
	 * storing <code>AdminPermission</code>s.
	 * 
	 * @return A new <code>PermissionCollection</code> object.
	 */
	public PermissionCollection newPermissionCollection() {
		return new AdminPermissionCollection();
	}

	/**
	 * Determines the equality of two <code>AdminPermission</code> objects.
	 * 
	 * @param obj The object being compared for equality with this object.
	 * @return <code>true</code> if <code>obj</code> is equivalent to this
	 *         <code>AdminPermission</code>; <code>false</code> otherwise.
	 */
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof AdminPermission)) {
			return false;
		}

		AdminPermission ap = (AdminPermission) obj;

		return (action_mask == ap.action_mask)
				&& ((bundle == ap.bundle) || ((bundle != null) && bundle
						.equals(ap.bundle)))
				&& (filter == null ? ap.filter == null : filter
						.equals(ap.filter));
	}

	/**
	 * Returns the hash code value for this object.
	 * 
	 * @return Hash code value for this object.
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
		// Read in the data, then initialize the transients
		s.defaultReadObject();
		setTransients(parseFilter(getName()), parseActions(actions));
	}

	/**
	 * Called by <code>implies0</code> on an AdminPermission which was
	 * constructed with a Bundle. This method loads a dictionary with the
	 * filter-matchable properties of this bundle. The dictionary is cached so
	 * this lookup only happens once.
	 * 
	 * This method should only be called on an AdminPermission which was
	 * constructed with a bundle
	 * 
	 * @return a dictionary of properties for this bundle
	 */
	private Dictionary getProperties() {
		Dictionary result = properties;
		if (result != null) {
			return result;
		}
		/*
		 * We may have recursed here due to the Bundle.getLocation call in the
		 * doPrivileged below. If this is the case, return null to allow implies
		 * to return true.
		 */
		final Object mark = recurse.get();
		if (mark == bundle) {
			return null;
		}
		recurse.set(bundle);
		try {
			final Dictionary dict = new Hashtable(4);
			AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					dict.put("id", new Long(bundle.getBundleId()));
					dict.put("location", bundle.getLocation());
					String name = bundle.getSymbolicName();
					if (name != null) {
						dict.put("name", name);
					}
					SignerProperty signer = new SignerProperty(bundle);
					if (signer.isBundleSigned()) {
						dict.put("signer", signer);
					}
					return null;
				}
			});
			return properties = dict;
		}
		finally {
			recurse.set(null);
		}
	}
}

/**
 * Stores a collection of <code>AdminPermission</code>s.
 */
final class AdminPermissionCollection extends PermissionCollection {
	private static final long	serialVersionUID	= 3906372644575328048L;
	/**
	 * Collection of permissions.
	 * 
	 * @GuardedBy this
	 */
	private transient Map		permissions;

	/**
	 * Boolean saying if "*" is in the collection.
	 * 
	 * @serial
	 * @GuardedBy this
	 */
	private boolean				all_allowed;

	/**
	 * Create an empty AdminPermissions object.
	 * 
	 */
	public AdminPermissionCollection() {
		permissions = new HashMap();
	}

	/**
	 * Adds a permission to this permission collection.
	 * 
	 * @param permission The <code>AdminPermission</code> object to add.
	 * @throws IllegalArgumentException If the specified permission is not an
	 *         <code>AdminPermission</code> instance or was constructed with a
	 *         Bundle object.
	 * @throws SecurityException If this <code>AdminPermissionCollection</code>
	 *         object has been marked read-only.
	 */
	public void add(Permission permission) {
		if (!(permission instanceof AdminPermission)) {
			throw new IllegalArgumentException("invalid permission: "
					+ permission);
		}
		if (isReadOnly()) {
			throw new SecurityException("attempt to add a Permission to a "
					+ "readonly PermissionCollection"); 
		}
		final AdminPermission ap = (AdminPermission) permission;
		if (ap.bundle != null) {
			throw new IllegalArgumentException("cannot add to collection: "
					+ ap);
		}
		final String name = ap.getName();
		synchronized (this) {
			Map pc = permissions;
			AdminPermission existing = (AdminPermission) pc.get(name);
			if (existing != null) {
				int oldMask = existing.action_mask;
				int newMask = ap.action_mask;

				if (oldMask != newMask) {
					pc.put(name, new AdminPermission(existing.filter, oldMask
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
	 * in <code>permission</code>.
	 * 
	 * @param permission The Permission object to compare with the
	 *        <code>AdminPermission</code> objects in this collection.
	 * @return <code>true</code> if <code>permission</code> is implied by an
	 *         <code>AdminPermission</code> in this collection,
	 *         <code>false</code> otherwise.
	 */
	public boolean implies(Permission permission) {
		if (!(permission instanceof AdminPermission)) {
			return false;
		}

		AdminPermission requested = (AdminPermission) permission;
		// if requested permission has a filter, then it is an invalid argument
		if (requested.filter != null) {
			return false;
		}
		int effective = AdminPermission.ACTION_NONE;
		Collection perms;
		synchronized (this) {
			Map pc = permissions;
			// short circuit if the "*" Permission was added
			if (all_allowed) {
				AdminPermission ap = (AdminPermission) pc.get("*");
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

		// just iterate one by one
		for (Iterator iter = perms.iterator(); iter.hasNext();) {
			if (((AdminPermission) iter.next()).implies0(requested, effective)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns an enumeration of all <code>AdminPermission</code> objects in the
	 * container.
	 * 
	 * @return Enumeration of all <code>AdminPermission</code> objects.
	 */
	public synchronized Enumeration elements() {
		return Collections.enumeration(permissions.values());
	}
	
	/* serialization logic */
    private static final ObjectStreamField[]	serialPersistentFields	= {
			new ObjectStreamField("permissions", Hashtable.class),
			new ObjectStreamField("all_allowed", Boolean.TYPE)			};
    
    private synchronized void writeObject(ObjectOutputStream out)
			throws IOException {
		Hashtable hashtable = new Hashtable(permissions);
		ObjectOutputStream.PutField pfields = out.putFields();
		pfields.put("permissions", hashtable);
		pfields.put("all_allowed", all_allowed);
		out.writeFields();
	}
    
    private synchronized void readObject(java.io.ObjectInputStream in)
			throws IOException,
			ClassNotFoundException {
		ObjectInputStream.GetField gfields = in.readFields();
		Hashtable hashtable = (Hashtable) gfields.get("permissions", null);
		permissions = new HashMap(hashtable);
		all_allowed = gfields.get("all_allowed", false);
	}
}
