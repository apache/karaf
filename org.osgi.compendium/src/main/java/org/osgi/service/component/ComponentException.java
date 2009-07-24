/*
 * Copyright (c) OSGi Alliance (2004, 2008). All Rights Reserved.
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

package org.osgi.service.component;

/**
 * Unchecked exception which may be thrown by the Service Component Runtime.
 * 
 * @version $Revision: 6083 $
 */
public class ComponentException extends RuntimeException {
	static final long	serialVersionUID	= -7438212656298726924L;

	/**
	 * Construct a new ComponentException with the specified message and cause.
	 * 
	 * @param message The message for the exception.
	 * @param cause The cause of the exception. May be <code>null</code>.
	 */
	public ComponentException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Construct a new ComponentException with the specified message.
	 * 
	 * @param message The message for the exception.
	 */
	public ComponentException(String message) {
		super(message);
	}

	/**
	 * Construct a new ComponentException with the specified cause.
	 * 
	 * @param cause The cause of the exception. May be <code>null</code>.
	 */
	public ComponentException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * Returns the cause of this exception or <code>null</code> if no cause was
	 * set.
	 * 
	 * @return The cause of this exception or <code>null</code> if no cause was
	 *         set.
	 */
	public Throwable getCause() {
		return super.getCause();
	}

	/**
	 * Initializes the cause of this exception to the specified value.
	 * 
	 * @param cause The cause of this exception.
	 * @return This exception.
	 * @throws IllegalArgumentException If the specified cause is this
	 *         exception.
	 * @throws IllegalStateException If the cause of this exception has already
	 *         been set.
	 */
	public Throwable initCause(Throwable cause) {
		return super.initCause(cause);
	}
}
