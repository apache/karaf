/*
 * $Header: /cvshome/build/org.osgi.service.component/src/org/osgi/service/component/ComponentException.java,v 1.13 2006/07/11 13:15:56 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2004, 2006). All Rights Reserved.
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
 * @version $Revision: 1.13 $
 */
public class ComponentException extends RuntimeException {
	static final long	serialVersionUID	= -7438212656298726924L;
	/**
	 * Nested exception.
	 */
	private final Throwable	cause;

	/**
	 * Construct a new ComponentException with the specified message and cause.
	 * 
	 * @param message The message for the exception.
	 * @param cause The cause of the exception. May be <code>null</code>.
	 */
	public ComponentException(String message, Throwable cause) {
		super(message);
		this.cause = cause;
	}

	/**
	 * Construct a new ComponentException with the specified message.
	 * 
	 * @param message The message for the exception.
	 */
	public ComponentException(String message) {
		super(message);
		this.cause = null;
	}

	/**
	 * Construct a new ComponentException with the specified cause.
	 * 
	 * @param cause The cause of the exception. May be <code>null</code>.
	 */
	public ComponentException(Throwable cause) {
		super();
		this.cause = cause;
	}

	/**
	 * Returns the cause of this exception or <code>null</code> if no cause
	 * was specified when this exception was created.
	 * 
	 * @return The cause of this exception or <code>null</code> if no cause
	 *         was specified.
	 */
	public Throwable getCause() {
		return cause;
	}

	/**
	 * The cause of this exception can only be set when constructed.
	 * 
	 * @param cause Cause of the exception.
	 * @return This object.
	 * @throws java.lang.IllegalStateException This method will always throw an
	 *         <code>IllegalStateException</code> since the cause of this
	 *         exception can only be set when constructed.
	 */
	public Throwable initCause(Throwable cause) {
		throw new IllegalStateException();
	}
}
