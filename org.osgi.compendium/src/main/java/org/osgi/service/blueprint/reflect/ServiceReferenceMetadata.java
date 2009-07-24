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

import java.util.Collection;

/**
 * Metadata for a reference to an OSGi service. This is the base type for
 * {@link ReferenceListMetadata} and {@link ReferenceMetadata}.
 * 
 * @ThreadSafe
 * @version $Revision: 7563 $
 */
public interface ServiceReferenceMetadata extends ComponentMetadata {

	/**
	 * A matching service is required at all times.
	 * 
	 * @see #getAvailability()
	 */
	static final int	AVAILABILITY_MANDATORY	= 1;

	/**
	 * A matching service is not required to be present.
	 * 
	 * @see #getAvailability()
	 */
	static final int	AVAILABILITY_OPTIONAL	= 2;

	/**
	 * Return whether or not a matching service is required at all times.
	 * 
	 * This is specified in the <code>availability</code> attribute of the
	 * service reference.
	 * 
	 * @return Whether or not a matching service is required at all times.
	 * @see #AVAILABILITY_MANDATORY
	 * @see #AVAILABILITY_OPTIONAL
	 */
	int getAvailability();

	/**
	 * Return the name of the interface type that a matching service must
	 * support.
	 * 
	 * This is specified in the <code>interface</code> attribute of the service
	 * reference.
	 * 
	 * @return The name of the interface type that a matching service must
	 *         support or <code>null</code> when no interface name is specified.
	 */
	String getInterface();

	/**
	 * Return the value of the <code>component-name</code> attribute of the
	 * service reference. This specifies the id of a component that is
	 * registered in the service registry. This will create an automatic filter,
	 * appended with the filter if set, to select this component based on its
	 * automatic <code>id</code> attribute.
	 * 
	 * @return The value of the <code>component-name</code> attribute of the
	 *         service reference or <code>null</code> if the attribute is not
	 *         specified.
	 */
	String getComponentName();

	/**
	 * Return the filter expression that a matching service must match.
	 * 
	 * This is specified by the <code>filter</code> attribute of the service
	 * reference.
	 * 
	 * @return The filter expression that a matching service must match or
	 *         <code>null</code> if a filter is not specified.
	 */
	String getFilter();

	/**
	 * Return the reference listeners to receive bind and unbind events.
	 * 
	 * This is specified by the <code>reference-listener</code> elements of the
	 * service reference.
	 * 
	 * @return An immutable Collection of {@link ReferenceListener} objects to
	 *         receive bind and unbind events. The Collection is empty if no
	 *         reference listeners are specified for the service reference.
	 */
	Collection /* <ReferenceListener> */getReferenceListeners();
}
