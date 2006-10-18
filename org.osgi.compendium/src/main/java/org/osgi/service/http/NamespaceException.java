/*
 * $Header: /cvshome/build/org.osgi.service.http/src/org/osgi/service/http/NamespaceException.java,v 1.11 2006/07/11 13:15:56 hargrave Exp $
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
package org.osgi.service.http;

/**
 * A NamespaceException is thrown to indicate an error with the caller's request
 * to register a servlet or resources into the URI namespace of the Http
 * Service. This exception indicates that the requested alias already is in use.
 * 
 * @version $Revision: 1.11 $
 */
public class NamespaceException extends Exception {
    static final long serialVersionUID = 7235606031147877747L;
	/**
	 * Nested exception.
	 */
	private final Throwable	cause;

	/**
	 * Construct a <code>NamespaceException</code> object with a detail message.
	 * 
	 * @param message the detail message
	 */
	public NamespaceException(String message) {
		super(message);
		cause = null;
	}

	/**
	 * Construct a <code>NamespaceException</code> object with a detail message
	 * and a nested exception.
	 * 
	 * @param message The detail message.
	 * @param cause The nested exception.
	 */
	public NamespaceException(String message, Throwable cause) {
		super(message);
		this.cause = cause;
	}

	/**
	 * Returns the nested exception.
	 *
     * <p>This method predates the general purpose exception chaining mechanism.
     * The {@link #getCause()} method is now the preferred means of
     * obtaining this information.
	 * 
	 * @return the nested exception or <code>null</code> if there is no nested
	 *         exception.
	 */
	public Throwable getException() {
		return cause;
	}

	/**
	 * Returns the cause of this exception or <code>null</code> if no
	 * cause was specified when this exception was created.
	 *
	 * @return  The cause of this exception or <code>null</code> if no
	 * cause was specified.
	 * @since 1.2 
	 */
	public Throwable getCause() {
	    return cause;
	}

	/**
	 * The cause of this exception can only be set when constructed.
	 *
	 * @param cause Cause of the exception.
	 * @return This object.
	 * @throws java.lang.IllegalStateException
	 * This method will always throw an <code>IllegalStateException</code>
	 * since the cause of this exception can only be set when constructed.
	 * @since 1.2 
	 */
	public Throwable initCause(Throwable cause) {
		throw new IllegalStateException();
	}
}
