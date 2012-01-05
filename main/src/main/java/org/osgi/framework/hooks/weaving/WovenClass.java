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

package org.osgi.framework.hooks.weaving;

import java.security.ProtectionDomain;
import java.util.List;

import org.osgi.framework.wiring.BundleWiring;

/**
 * A class being woven.
 * 
 * This object represents a class being woven and is passed to each
 * {@link WeavingHook} for possible modification. It allows access to the most
 * recently transformed class file bytes and to any additional packages that
 * should be added to the bundle as dynamic imports.
 * 
 * <p>
 * After weaving is {@link #isWeavingComplete() complete}, this object becomes
 * effectively immutable.
 * 
 * @NotThreadSafe
 * @noimplement
 * @version $Id: c689a4c27dc39af1bf5f51338f1a8eaca1dddc1a $
 */
public interface WovenClass {

	/**
	 * Returns the class file bytes to be used to define the
	 * {@link WovenClass#getClassName() named} class.
	 * 
	 * <p>
	 * While weaving is not {@link #isWeavingComplete() complete}, this method
	 * returns a reference to the class files byte array contained in this
	 * object. After weaving is {@link #isWeavingComplete() complete}, this
	 * object becomes effectively immutable and a copy of the class file byte
	 * array is returned.
	 * 
	 * @return The bytes to be used to define the
	 *         {@link WovenClass#getClassName() named} class.
	 * @throws SecurityException If the caller does not have
	 *         {@code AdminPermission[bundle,WEAVE]} and the Java runtime
	 *         environment supports permissions.
	 */
	public byte[] getBytes();

	/**
	 * Set the class file bytes to be used to define the
	 * {@link WovenClass#getClassName() named} class. This method must not be
	 * called outside invocations of the {@link WeavingHook#weave(WovenClass)
	 * weave} method by the framework.
	 * 
	 * <p>
	 * While weaving is not {@link #isWeavingComplete() complete}, this method
	 * replaces the reference to the array contained in this object with the
	 * specified array. After weaving is {@link #isWeavingComplete() complete},
	 * this object becomes effectively immutable and this method will throw an
	 * {@link IllegalStateException}.
	 * 
	 * @param newBytes The new classfile that will be used to define the
	 *        {@link WovenClass#getClassName() named} class. The specified array
	 *        is retained by this object and the caller must not modify the
	 *        specified array.
	 * @throws NullPointerException If newBytes is {@code null}.
	 * @throws IllegalStateException If weaving is {@link #isWeavingComplete()
	 *         complete}.
	 * @throws SecurityException If the caller does not have
	 *         {@code AdminPermission[bundle,WEAVE]} and the Java runtime
	 *         environment supports permissions.
	 */
	public void setBytes(byte[] newBytes);

	/**
	 * Returns the list of dynamic import package descriptions to add to the
	 * {@link #getBundleWiring() bundle wiring} for this woven class. Changes
	 * made to the returned list will be visible to later {@link WeavingHook
	 * weaving hooks} called with this object. The returned list must not be
	 * modified outside invocations of the {@link WeavingHook#weave(WovenClass)
	 * weave} method by the framework.
	 * 
	 * <p>
	 * After weaving is {@link #isWeavingComplete() complete}, this object
	 * becomes effectively immutable and the returned list will be unmodifiable.
	 * 
	 * <p>
	 * If the Java runtime environment supports permissions, the caller must
	 * have {@code AdminPermission[bundle,WEAVE]} to modify the returned list.
	 * 
	 * @return A list containing zero or more dynamic import package
	 *         descriptions to add to the bundle wiring for this woven class.
	 *         This list must throw {@code IllegalArgumentException} if a
	 *         malformed dynamic import package description is added.
	 * @see "Core Specification, Dynamic Import Package, for the syntax of a dynamic import package description."
	 */
	public List<String> getDynamicImports();

	/**
	 * Returns whether weaving is complete in this woven class. Weaving is
	 * complete after the last {@link WeavingHook weaving hook} is called and
	 * the class is defined.
	 * 
	 * <p>
	 * After weaving is complete, this object becomes effectively immutable.
	 * 
	 * @return {@code true} weaving is complete, {@code false} otherwise.
	 */
	public boolean isWeavingComplete();

	/**
	 * Returns the fully qualified name of the class being woven.
	 * 
	 * @return The fully qualified name of the class being woven.
	 */
	public String getClassName();

	/**
	 * Returns the protection domain to which the woven class will be assigned
	 * when it is defined.
	 * 
	 * @return The protection domain to which the woven class will be assigned
	 *         when it is defined, or {@code null} if no protection domain will
	 *         be assigned.
	 */
	public ProtectionDomain getProtectionDomain();

	/**
	 * Returns the class associated with this woven class. When loading a class
	 * for the first time this method will return {@code null} until weaving is
	 * {@link #isWeavingComplete() complete}. Once weaving is complete, this
	 * method will return the class object.
	 * 
	 * @return The class associated with this woven class, or {@code null} if
	 *         weaving is not complete or the class definition failed.
	 */
	public Class< ? > getDefinedClass();

	/**
	 * Returns the bundle wiring whose class loader will define the woven class.
	 * 
	 * @return The bundle wiring whose class loader will define the woven class.
	 */
	public BundleWiring getBundleWiring();
}
