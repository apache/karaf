/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/InvalidSyntaxException.java,v 1.10 2005/05/13 20:32:55 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2000, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.framework;

/**
 * A Framework exception.
 * 
 * <p>
 * An <code>InvalidSyntaxException</code> object indicates that a filter string
 * parameter has an invalid syntax and cannot be parsed.
 * 
 * <p>
 * See {@link Filter} for a description of the filter string syntax.
 * 
 * @version $Revision: 1.10 $
 */

public class InvalidSyntaxException extends Exception {
	static final long	serialVersionUID	= -4295194420816491875L;
	/**
	 * The invalid filter string.
	 */
	private String		filter;
	/**
	 * Nested exception.
	 */
	private Throwable	cause;

	/**
	 * Creates an exception of type <code>InvalidSyntaxException</code>.
	 * 
	 * <p>
	 * This method creates an <code>InvalidSyntaxException</code> object with the
	 * specified message and the filter string which generated the exception.
	 * 
	 * @param msg The message.
	 * @param filter The invalid filter string.
	 */
	public InvalidSyntaxException(String msg, String filter) {
		super(msg);
		this.filter = filter;
		this.cause = null;
	}

	/**
	 * Creates an exception of type <code>InvalidSyntaxException</code>.
	 * 
	 * <p>
	 * This method creates an <code>InvalidSyntaxException</code> object with the
	 * specified message and the filter string which generated the exception.
	 * 
	 * @param msg The message.
	 * @param filter The invalid filter string.
	 * @param cause The cause of this exception.
	 * @since 1.3
	 */
	public InvalidSyntaxException(String msg, String filter, Throwable cause) {
		super(msg);
		this.filter = filter;
		this.cause = cause;
	}

	/**
	 * Returns the filter string that generated the
	 * <code>InvalidSyntaxException</code> object.
	 * 
	 * @return The invalid filter string.
	 * @see BundleContext#getServiceReferences
	 * @see BundleContext#addServiceListener(ServiceListener,String)
	 */
	public String getFilter() {
		return filter;
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

