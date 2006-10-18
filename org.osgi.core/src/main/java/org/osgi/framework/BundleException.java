/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/BundleException.java,v 1.15 2006/07/11 13:15:54 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2000, 2006). All Rights Reserved.
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
 * A Framework exception used to indicate that a bundle lifecycle problem
 * occurred.
 * 
 * <p>
 * <code>BundleException</code> object is created by the Framework to denote
 * an exception condition in the lifecycle of a bundle.
 * <code>BundleException</code>s should not be created by bundle developers.
 * 
 * <p>
 * This exception is updated to conform to the general purpose exception
 * chaining mechanism.
 * 
 * @version $Revision: 1.15 $
 */

public class BundleException extends Exception {
	static final long	serialVersionUID	= 3571095144220455665L;
	/**
	 * Nested exception.
	 */
	private final Throwable	cause;

	/**
	 * Creates a <code>BundleException</code> that wraps another exception.
	 * 
	 * @param msg The associated message.
	 * @param cause The cause of this exception.
	 */
	public BundleException(String msg, Throwable cause) {
		super(msg);
		this.cause = cause;
	}

	/**
	 * Creates a <code>BundleException</code> object with the specified
	 * message.
	 * 
	 * @param msg The message.
	 */
	public BundleException(String msg) {
		super(msg);
		this.cause = null;
	}

	/**
	 * Returns any nested exceptions included in this exception.
	 * 
	 * <p>
	 * This method predates the general purpose exception chaining mechanism.
	 * The {@link #getCause()} method is now the preferred means of obtaining
	 * this information.
	 * 
	 * @return The nested exception; <code>null</code> if there is no nested
	 *         exception.
	 */
	public Throwable getNestedException() {
		return cause;
	}

	/**
	 * Returns the cause of this exception or <code>null</code> if no cause
	 * was specified when this exception was created.
	 * 
	 * @return The cause of this exception or <code>null</code> if no cause
	 *         was specified.
	 * @since 1.3
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
	 * @since 1.3
	 */
	public Throwable initCause(Throwable cause) {
		throw new IllegalStateException();
	}
}
