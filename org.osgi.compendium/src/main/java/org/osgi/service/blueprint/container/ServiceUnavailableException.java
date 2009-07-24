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

import org.osgi.framework.ServiceException;

/**
 * A Blueprint exception indicating that a service is unavailable.
 * 
 * This exception is thrown when an invocation is made on a service reference
 * and a backing service is not available.
 * 
 * @version $Revision: 7556 $
 */
public class ServiceUnavailableException extends ServiceException {
	private static final long	serialVersionUID	= 1L;
	/**
	 * The filter string associated with the exception.
	 */
	private final String		filter;

	/**
	 * Creates a Service Unavailable Exception with the specified message.
	 * 
	 * @param message The associated message.
	 * @param filter The filter used for the service lookup.
	 */
	public ServiceUnavailableException(String message, String filter) {
		super(message, UNREGISTERED);
		this.filter = filter;
	}

	/**
	 * Creates a Service Unavailable Exception with the specified message and
	 * exception cause.
	 * 
	 * @param message The associated message.
	 * @param filter The filter used for the service lookup.
	 * @param cause The cause of this exception.
	 */
	public ServiceUnavailableException(String message, String filter,
			Throwable cause) {
		super(message, UNREGISTERED, cause);
		this.filter = filter;
	}

	/**
	 * Returns the filter expression that a service would have needed to satisfy
	 * in order for the invocation to proceed.
	 * 
	 * @return The failing filter.
	 */
	public String getFilter() {
		return this.filter;
	}
}
