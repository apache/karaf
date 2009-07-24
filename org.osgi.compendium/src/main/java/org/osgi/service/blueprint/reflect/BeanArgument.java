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
package org.osgi.service.blueprint.reflect;

/**
 * Metadata for a factory method or constructor argument of a bean. The
 * arguments of a bean are obtained from {@link BeanMetadata#getArguments()}.
 * 
 * This is specified by the <code>argument</code> elements of a bean.
 * 
 * @ThreadSafe
 * @version $Revision: 7563 $
 */
public interface BeanArgument {

	/**
	 * Return the Metadata for the argument value.
	 * 
	 * This is specified by the <code>value</code> attribute.
	 * 
	 * @return The Metadata for the argument value.
	 */
	Metadata getValue();

	/**
	 * Return the name of the value type to match the argument and convert the
	 * value into when invoking the constructor or factory method.
	 * 
	 * This is specified by the <code>type</code> attribute.
	 * 
	 * @return The name of the value type to convert the value into, or
	 *         <code>null</code> if no type is specified.
	 */
	String getValueType();

	/**
	 * Return the zero-based index into the parameter list of the factory method
	 * or constructor to be invoked for this argument. This is determined by
	 * specifying the <code>index</code> attribute for the bean. If not
	 * explicitly set, this will return -1 and the initial ordering is defined
	 * by its position in the {@link BeanMetadata#getArguments()} list.
	 * 
	 * This is specified by the <code>index</code> attribute.
	 * 
	 * @return The zero-based index of the parameter, or -1 if no index is
	 *         specified.
	 */
	int getIndex();
}
