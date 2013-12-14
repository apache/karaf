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
package org.apache.felix.gogo.commands.converter;

/**
 * Provides access to a concrete type and its optional generic type arguments.
 *
 * Java 5 and later support generic types. These types consist of a raw class
 * with type arguments. This class models such a <code>Type</code> class but
 * ensures that the type is <em>reified</em>. Reification means that the Type
 * graph associated with a Java 5 <code>Type</code> instance is traversed
 * until the type becomes a concrete class. In Java 1.4 a class has no
 * arguments. This concrete class implements the Reified Type for Java 1.4.
 *
 * In Java 1.4, this class works with non-generic types. In that cases, a
 * Reified Type provides access to the class and has zero type arguments, though
 * a subclass that provide type arguments should be respected. Blueprint
 * extender implementations can subclass this class and provide access to the
 * generics type graph if used in a conversion. Such a subclass must
 * <em>reify<em> the different Java 5 <code>Type</code> instances into the
 * reified form. That is, a form where the raw Class is available with its optional type arguments as Reified Types.
 *
 * @Immutable
 */
@Deprecated
public class ReifiedType {

    final static ReifiedType ALL = new ReifiedType(Object.class);

    private final Class clazz;

    /**
     * Create a Reified Type for a raw Java class without any generic arguments.
     * Subclasses can provide the optional generic argument information. Without
     * subclassing, this instance has no type arguments.
     *
     * @param clazz
     *            The raw class of the Reified Type.
     */
    public ReifiedType(Class clazz) {
        this.clazz = clazz;
    }

    /**
     * Access to the raw class.
     *
     * The raw class represents the concrete class that is associated with a
     * type declaration. This class could have been deduced from the generics
     * type graph of the declaration. For example, in the following example:
     *
     * <pre>
     * Map&lt;String, Object&gt; map;
     * </pre>
     *
     * The raw class is the Map class.
     *
     * @return the collapsed raw class that represents this type.
     */
    public Class getRawClass() {
        return clazz;
    }

    /**
     * Access to a type argument.
     *
     * The type argument refers to a argument in a generic type declaration
     * given by index <code>i</code>. This method returns a Reified Type that
     * has Object as class when no generic type information is available. Any
     * object is assignable to Object and therefore no conversion is then
     * necessary, this is compatible with older Javas than 5. For this reason,
     * the implementation in this class always returns the
     * <code>Object<code> class, regardless of the given index.
     *
     * This method should be overridden by a subclass that provides access to
     * the generic information.
     *
     * For example, in the following example:
     *
     * <pre>
     * Map&lt;String, Object&gt; map;
     * </pre>
     *
     * The type argument 0 is <code>String</code>, and type argument 1 is
     * <code>Object</code>.
     *
     * @param i
     *            The index of the type argument
     * @return <code>ReifiedType(Object.class)<code>, subclasses must override this and return the generic argument at index <code>i</code>
     */
    public ReifiedType getActualTypeArgument(int i) {
        return ALL;
    }

    /**
     * Return the number of type arguments.
     *
     * This method should be overridden by a subclass to support Java 5 types.
     *
     * @return 0, subclasses must override this and return the number of generic
     *         arguments
     */
    public int size() {
        return 0;
    }
}
