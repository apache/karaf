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

import java.util.List;

/**
 * Metadata for managed components. This is the base type for
 * {@link BeanMetadata}, {@link ServiceMetadata} and
 * {@link ServiceReferenceMetadata}.
 * 
 * @ThreadSafe
 * @version $Revision: 7563 $
 */
public interface ComponentMetadata extends NonNullMetadata {

	/**
	 * The component's manager must eagerly activate the component.
	 * 
	 * @see #getActivation()
	 */
	static final int	ACTIVATION_EAGER	= 1;

	/**
	 * The component's manager must lazily activate the component.
	 * 
	 * @see #getActivation()
	 */
	static final int	ACTIVATION_LAZY		= 2;

	/**
	 * Return the id of the component.
	 * 
	 * @return The id of the component. The component id can be
	 *         <code>null</code> if this is an anonymously defined and/or
	 *         inlined component.
	 */
	String getId();

	/**
	 * Return the activation strategy for the component.
	 * 
	 * This is specified by the <code>activation</code> attribute of a component
	 * definition. If this is not set, then the <code>default-activation</code>
	 * in the <code>blueprint</code> element is used. If that is also not set,
	 * then the activation strategy is {@link #ACTIVATION_EAGER}.
	 * 
	 * @return The activation strategy for the component.
	 * @see #ACTIVATION_EAGER
	 * @see #ACTIVATION_LAZY
	 */
	int getActivation();

	/**
	 * Return the ids of any components listed in a <code>depends-on</code>
	 * attribute for the component.
	 * 
	 * @return An immutable List of component ids that are explicitly declared
	 *         as a dependency, or an empty List if none.
	 */
	List/* <String> */getDependsOn();
}
