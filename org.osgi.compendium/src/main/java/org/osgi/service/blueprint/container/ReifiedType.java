/*
 * Copyright (c) OSGi Alliance (2008, 2009). All Rights Reserved.
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
package org.osgi.service.blueprint.container;

/**
 * Provides access to a concrete type and its optional generic type parameters.
 * 
 * <p>
 * Java 5 and later support generic types. These types consist of a raw class
 * with type parameters. This class models such a <code>Type</code> class but
 * ensures that the type is <em>reified</em>. Reification means that the Type
 * graph associated with a Java 5 <code>Type</code> instance is traversed until
 * the type becomes a concrete class. This class is available with the
 * {@link #getRawClass()} method. The optional type parameters are recursively
 * represented as Reified Types.
 * 
 * <p>
 * In Java 1.4, a class has by definition no type parameters. This class
 * implementation provides the Reified Type for Java 1.4 by making the raw class
 * the Java 1.4 class and using a Reified Type based on the <code>Object</code>
 * class for any requested type parameter.
 * 
 * <p>
 * A Blueprint extender implementations can subclass this class and provide
 * access to the generic type parameter graph for conversion. Such a subclass
 * must <em>reify</em> the different Java 5 <code>Type</code> instances into the
 * reified form. That is, a form where the raw Class is available with its
 * optional type parameters as Reified Types.
 * 
 * @Immutable
 * @version $Revision: 7564 $
 */
public class ReifiedType {
	private final static ReifiedType	OBJECT	= new ReifiedType(Object.class);

	private final Class					clazz;

	/**
	 * Create a Reified Type for a raw Java class without any generic type
	 * parameters. Subclasses can provide the optional generic type parameter
	 * information. Without subclassing, this instance has no type parameters.
	 * 
	 * @param clazz The raw class of the Reified Type.
	 */
	public ReifiedType(Class clazz) {
		this.clazz = clazz;
	}

	/**
	 * Return the raw class represented by this type.
	 * 
	 * The raw class represents the concrete class that is associated with a
	 * type declaration. This class could have been deduced from the generics
	 * type parameter graph of the declaration. For example, in the following
	 * example:
	 * 
	 * <pre>
	 * Map&lt;String, ? extends Metadata&gt;
	 * </pre>
	 * 
	 * The raw class is the Map class.
	 * 
	 * @return The raw class represented by this type.
	 */
	public Class getRawClass() {
		return clazz;
	}

	/**
	 * Return a type parameter for this type.
	 * 
	 * The type parameter refers to a parameter in a generic type declaration
	 * given by the zero-based index <code>i</code>.
	 * 
	 * For example, in the following example:
	 * 
	 * <pre>
	 * Map&lt;String, ? extends Metadata&gt;
	 * </pre>
	 * 
	 * type parameter 0 is <code>String</code>, and type parameter 1 is
	 * <code>Metadata</code>.
	 * 
	 * <p>
	 * This implementation returns a Reified Type that has <code>Object</code>
	 * as class. Any object is assignable to Object and therefore no conversion
	 * is then necessary. This is compatible with versions of Java language
	 * prior to Java 5.
	 * 
	 * This method should be overridden by a subclass that provides access to
	 * the generic type parameter information for Java 5 and later.
	 * 
	 * @param i The zero-based index of the requested type parameter.
	 * @return The <code>ReifiedType</code> for the generic type parameter at
	 *         the specified index.
	 */
	public ReifiedType getActualTypeArgument(int i) {
		return OBJECT;
	}

	/**
	 * Return the number of type parameters for this type.
	 * 
	 * <p>
	 * This implementation returns <code>0</code>. This method should be
	 * overridden by a subclass that provides access to the generic type
	 * parameter information for Java 5 and later.
	 * 
	 * @return The number of type parameters for this type.
	 */
	public int size() {
		return 0;
	}
}
