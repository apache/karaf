/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/AdminPermission.java,v 1.29 2006/06/16 16:31:18 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2000, 2006). All Rights Reserved.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.*;

/**
 * Indicates the caller's authority to perform specific privileged
 * administrative operations on or to get sensitive information about a bundle.
 * The actions for this permission are:
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
 * </pre>
 * 
 * <p>
 * The special action "*" will represent all actions.
 * <p>
 * The name of this permission is a filter expression. The filter gives access
 * to the following parameters:
 * <ul>
 * <li>signer - A Distinguished Name chain used to sign a bundle. Wildcards in
 * a DN are not matched according to the filter string rules, but according to
 * the rules defined for a DN chain.</li>
 * <li>location - The location of a bundle.</li>
 * <li>id - The bundle ID of the designated bundle.</li>
 * <li>name - The symbolic name of a bundle.</li>
 * </ul>
 * 
 * @version $Revision: 1.29 $
 */

public final class AdminPermission extends BasicPermission {
	static final long					serialVersionUID	= 307051004521261705L;

	/**
	 * The action string <code>class</code> (Value is "class").
	 * @since 1.3
	 */
	public final static String			CLASS				= "class";
	/**
	 * The action string <code>execute</code> (Value is "execute").
	 * @since 1.3
	 */
	public final static String			EXECUTE				= "execute";
	/**
	 * The action string <code>extensionLifecycle</code> (Value is
	 * "extensionLifecycle").
	 * @since 1.3
	 */
	public final static String			EXTENSIONLIFECYCLE	= "extensionLifecycle";
	/**
	 * The action string <code>lifecycle</code> (Value is "lifecycle").
	 * @since 1.3
	 */
	public final static String			LIFECYCLE			= "lifecycle";
	/**
	 * The action string <code>listener</code> (Value is "listener").
	 * @since 1.3
	 */
	public final static String			LISTENER			= "listener";
	/**
	 * The action string <code>metadata</code> (Value is "metadata").
	 * @since 1.3
	 */
	public final static String			METADATA			= "metadata";
	/**
	 * The action string <code>resolve</code> (Value is "resolve").
	 * @since 1.3
	 */
	public final static String			RESOLVE				= "resolve";
	/**
	 * The action string <code>resource</code> (Value is "resource").
	 * @since 1.3
	 */
	public final static String			RESOURCE			= "resource";
	/**
	 * The action string <code>startlevel</code> (Value is "startlevel").
	 * @since 1.3
	 */
	public final static String			STARTLEVEL			= "startlevel";

	/*
	 * NOTE: A framework implementor may also choose to replace this class in
	 * their distribution with a class that directly interfaces with the
	 * framework implementation. This replacement class MUST NOT
	 * alter the public/protected signature of this class.
	 */

	/*
	 * This class will load the AdminPermission class in the package named by
	 * the org.osgi.vendor.framework package. For each instance of this class,
	 * an instance of the vendor AdminPermission class will be created and this
	 * class will delegate method calls to the vendor AdminPermission instance.
	 */
	private static final String			packageProperty		= "org.osgi.vendor.framework";
	private static final Constructor	initStringString;
	private static final Constructor	initBundleString;
	static {
		Constructor[] constructors = (Constructor[]) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						String packageName = System
								.getProperty(packageProperty);
						if (packageName == null) {
							throw new NoClassDefFoundError(packageProperty
									+ " property not set");
						}

						Class delegateClass;
						try {
							delegateClass = Class.forName(packageName
									+ ".AdminPermission");
						}
						catch (ClassNotFoundException e) {
							throw new NoClassDefFoundError(e.toString());
						}

						Constructor[] result = new Constructor[2];
						try {
							result[0] = delegateClass
									.getConstructor(new Class[] {String.class,
			String.class			});
							result[1] = delegateClass
									.getConstructor(new Class[] {Bundle.class,
			String.class			});
						}
						catch (NoSuchMethodException e) {
							throw new NoSuchMethodError(e.toString());
						}

						return result;
					}
				});

		initStringString = constructors[0];
		initBundleString = constructors[1];
	}

	/*
	 * This is the delegate permission created by the constructor.
	 */
	private final Permission			delegate;

	/**
	 * Creates a new <code>AdminPermission</code> object that matches all
	 * bundles and has all actions. Equivalent to AdminPermission("*","*");
	 */
	public AdminPermission() {
		this("*", "*"); //$NON-NLS-1$
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
	 *        all bundle.
	 * @param actions <code>class</code>, <code>execute</code>,
	 *        <code>extensionLifecycle</code>, <code>lifecycle</code>,
	 *        <code>listener</code>, <code>metadata</code>,
	 *        <code>resolve</code>, <code>resource</code>, or
	 *        <code>startlevel</code>. A value of "*" or <code>null</code>
	 *        indicates all actions
	 */
	public AdminPermission(String filter, String actions) {
		// arguments will be null if called from a PermissionInfo defined with
		// no args
		super(filter == null ? "*" : filter);
		try {
			try {
				delegate = (Permission) initStringString
						.newInstance(new Object[] {filter, actions});
			}
			catch (InvocationTargetException e) {
				throw e.getTargetException();
			}
		}
		catch (Error e) {
			throw e;
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Throwable e) {
			throw new RuntimeException(e.toString());
		}
	}

	/**
	 * Creates a new <code>AdminPermission</code> object to be used by the
	 * code that must check a <code>Permission</code> object.
	 * 
	 * @param bundle A bundle
	 * @param actions <code>class</code>, <code>execute</code>,
	 *        <code>extensionLifecycle</code>, <code>lifecycle</code>,
	 *        <code>listener</code>, <code>metadata</code>,
	 *        <code>resolve</code>, <code>resource</code>,
	 *        <code>startlevel</code>
	 * @since 1.3
	 */
	public AdminPermission(Bundle bundle, String actions) {
		super(createName(bundle));
		try {
			try {
				delegate = (Permission) initBundleString
						.newInstance(new Object[] {bundle, actions});
			}
			catch (InvocationTargetException e) {
				throw e.getTargetException();
			}
		}
		catch (Error e) {
			throw e;
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Throwable e) {
			throw new RuntimeException(e.toString());
		}
	}

	/**
	 * Create a permission name from a Bundle
	 * 
	 * @param bundle Bundle to use to create permission name.
	 * @return permission name.
	 */
	private static String createName(Bundle bundle) {
		StringBuffer sb = new StringBuffer();
		sb.append("(id=");
		sb.append(bundle.getBundleId());
		sb.append(")");
		return sb.toString();
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

		AdminPermission p = (AdminPermission) obj;

		return delegate.equals(p.delegate);
	}

	/**
	 * Returns the hash code value for this object.
	 * 
	 * @return Hash code value for this object.
	 */
	public int hashCode() {
		return delegate.hashCode();
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
	 * <code>resource</code>, <code>startlevel</code>.
	 * 
	 * @return Canonical string representation of the
	 *         <code>AdminPermission</code> actions.
	 */
	public String getActions() {
		return delegate.getActions();
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
	 * @param p The permission to interrogate.
	 * 
	 * @return <code>true</code> if the specified permission is implied by
	 *         this object; <code>false</code> otherwise.
	 * @throws RuntimeException if specified permission was not constructed with
	 *         a bundle or "*"
	 */
	public boolean implies(Permission p) {
		if (!(p instanceof AdminPermission)) {
			return false;
		}

		AdminPermission pp = (AdminPermission) p;
		return delegate.implies(pp.delegate);
	}

	/**
	 * Returns a new <code>PermissionCollection</code> object suitable for
	 * storing <code>AdminPermission</code>s.
	 * 
	 * @return A new <code>PermissionCollection</code> object.
	 */
	public PermissionCollection newPermissionCollection() {
		return delegate.newPermissionCollection();
	}
}
