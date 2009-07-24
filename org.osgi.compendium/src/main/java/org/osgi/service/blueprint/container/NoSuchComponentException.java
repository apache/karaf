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
 * A Blueprint exception indicating that a component does not exist in a
 * Blueprint Container.
 * 
 * This exception is thrown when an attempt is made to create a component
 * instance or lookup Component Metadata using a component id that does not
 * exist in the Blueprint Container.
 * 
 * @version $Revision: 7556 $
 */
public class NoSuchComponentException extends RuntimeException {
	private static final long	serialVersionUID	= 1L;
	/**
	 * The requested component id that generated the exception.
	 */
	private final String		componentId;

	/**
	 * Create a No Such Component Exception for a non-existent component.
	 * 
	 * @param msg The associated message.
	 * @param id The id of the non-existent component.
	 */
	public NoSuchComponentException(String msg, String id) {
		super(msg);
		this.componentId = id;
	}

	/**
	 * Create a No Such Component Exception for a non-existent component.
	 * 
	 * @param id The id of the non-existent component.
	 */
	public NoSuchComponentException(String id) {
		super("No component with id '" + (id == null ? "<null>" : id)
				+ "' could be found");
		this.componentId = id;
	}

	/**
	 * Returns the id of the non-existent component.
	 * 
	 * @return The id of the non-existent component.
	 */
	public String getComponentId() {
		return componentId;
	}
}
