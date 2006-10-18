/*
 * $Header: /cvshome/build/org.osgi.service.prefs/src/org/osgi/service/prefs/BackingStoreException.java,v 1.12 2006/07/11 13:15:55 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2001, 2006). All Rights Reserved.
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
package org.osgi.service.prefs;

/**
 * Thrown to indicate that a preferences operation could not complete because of
 * a failure in the backing store, or a failure to contact the backing store.
 * 
 * @version $Revision: 1.12 $
 */
public class BackingStoreException extends Exception {
    static final long serialVersionUID = -1415637364122829574L;
	/**
	 * Nested exception.
	 */
	private final Throwable	cause;

	/**
	 * Constructs a <code>BackingStoreException</code> with the specified detail
	 * message.
	 * 
	 * @param s The detail message.
	 */
	public BackingStoreException(String s) {
		super(s);
		this.cause = null;
	}
	
	/**
	 * Constructs a <code>BackingStoreException</code> with the specified detail
	 * message.
	 * 
	 * @param s The detail message.
	 * @param cause The cause of the exception. May be <code>null</code>.
	 * @since 1.1 
	 */
	public BackingStoreException(String s, Throwable cause) {
		super(s);
		this.cause = cause;
	}
	
	/**
	 * Returns the cause of this exception or <code>null</code> if no cause was
	 * specified when this exception was created.
	 * 
	 * @return The cause of this exception or <code>null</code> if no cause was
	 *         specified.
	 * @since 1.1 
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
	 * @since 1.1 
	 */
	public Throwable initCause(Throwable cause) {
		throw new IllegalStateException();
	}

}
