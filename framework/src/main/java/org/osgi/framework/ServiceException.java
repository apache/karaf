/*
 * Copyright (c) OSGi Alliance (2007, 2009). All Rights Reserved.
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

/**
 * A service exception used to indicate that a service problem occurred.
 * 
 * <p>
 * A <code>ServiceException</code> object is created by the Framework or
 * service implementation to denote an exception condition in the service. A
 * type code is used to identify the exception type for future extendability.
 * Service implementations may also create subclasses of
 * <code>ServiceException</code>. When subclassing, the subclass should set
 * the type to {@link #SUBCLASSED} to indicate that
 * <code>ServiceException</code> has been subclassed.
 * 
 * <p>
 * This exception conforms to the general purpose exception chaining mechanism.
 * 
 * @version $Revision: 6518 $
 * @since 1.5
 */

public class ServiceException extends RuntimeException {
	static final long		serialVersionUID	= 3038963223712959631L;

	/**
	 * Type of service exception.
	 */
	private final int		type;

	/**
	 * No exception type is unspecified.
	 */
	public static final int	UNSPECIFIED			= 0;
	/**
	 * The service has been unregistered.
	 */
	public static final int	UNREGISTERED		= 1;
	/**
	 * The service factory produced an invalid service object.
	 */
	public static final int	FACTORY_ERROR		= 2;
	/**
	 * The service factory threw an exception.
	 */
	public static final int	FACTORY_EXCEPTION	= 3;
	/**
	 * The exception is a subclass of ServiceException. The subclass should be
	 * examined for the type of the exception.
	 */
	public static final int	SUBCLASSED			= 4;
	/**
	 * An error occurred invoking a remote service.
	 */
	public static final int REMOTE 				= 5;

	/**
	 * Creates a <code>ServiceException</code> with the specified message and
	 * exception cause.
	 * 
	 * @param msg The associated message.
	 * @param cause The cause of this exception.
	 */
	public ServiceException(String msg, Throwable cause) {
		this(msg, UNSPECIFIED, cause);
	}

	/**
	 * Creates a <code>ServiceException</code> with the specified message.
	 * 
	 * @param msg The message.
	 */
	public ServiceException(String msg) {
		this(msg, UNSPECIFIED);
	}

	/**
	 * Creates a <code>ServiceException</code> with the specified message,
	 * type and exception cause.
	 * 
	 * @param msg The associated message.
	 * @param type The type for this exception.
	 * @param cause The cause of this exception.
	 */
	public ServiceException(String msg, int type, Throwable cause) {
		super(msg, cause);
		this.type = type;
	}

	/**
	 * Creates a <code>ServiceException</code> with the specified message and
	 * type.
	 * 
	 * @param msg The message.
	 * @param type The type for this exception.
	 */
	public ServiceException(String msg, int type) {
		super(msg);
		this.type = type;
	}

	/**
	 * Returns the type for this exception or <code>UNSPECIFIED</code> if the
	 * type was unspecified or unknown.
	 * 
	 * @return The type of this exception.
	 */
	public int getType() {
		return type;
	}
}
