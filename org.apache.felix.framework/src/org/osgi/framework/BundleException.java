/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/BundleException.java,v 1.10 2005/05/13 20:32:55 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2000, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.framework;

/**
 * A Framework exception used to indicate that a bundle lifecycle problem
 * occurred.
 * 
 * <p>
 * <code>BundleException</code> object is created by the Framework to denote an
 * exception condition in the lifecycle of a bundle. <code>BundleException</code>s
 * should not be created by bundle developers.
 * 
 * <p>
 * This exception is updated to conform to the general purpose exception
 * chaining mechanism.
 * 
 * @version $Revision: 1.10 $
 */

public class BundleException extends Exception {
	static final long	serialVersionUID	= 3571095144220455665L;
	/**
	 * Nested exception.
	 */
	private Throwable	cause;

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
	 * Creates a <code>BundleException</code> object with the specified message.
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
	 * Returns the cause of this exception or <code>null</code> if no cause was
	 * specified when this exception was created.
	 * 
	 * @return The cause of this exception or <code>null</code> if no cause was
	 *         specified.
	 * @since 1.3
	 */
	public Throwable getCause() {
		return cause;
	}

	/**
	 * The cause of this exception can only be set when constructed.
	 * 
	 * @throws java.lang.IllegalStateException This method will always throw an
	 *         <code>IllegalStateException</code> since the cause of this
	 *         exception can only be set when constructed.
	 * @since 1.3
	 */
	public Throwable initCause(Throwable cause) {
		throw new IllegalStateException();
	}
}