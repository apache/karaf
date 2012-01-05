/*
 * Copyright (c) OSGi Alliance (2010). All Rights Reserved.
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

package org.osgi.framework.hooks.weaving;

/**
 * A weaving exception used to indicate that the class load should be failed but
 * the weaving hook must not be blacklisted by the framework.
 * 
 * <p>
 * This exception conforms to the general purpose exception chaining mechanism.
 * 
 * @version $Id: eb38b85f6ed66ec445fb2f0ee7143df021327a9a $
 */

public class WeavingException extends RuntimeException {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Creates a {@code WeavingException} with the specified message and
	 * exception cause.
	 * 
	 * @param msg The associated message.
	 * @param cause The cause of this exception.
	 */
	public WeavingException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * Creates a {@code WeavingException} with the specified message.
	 * 
	 * @param msg The message.
	 */
	public WeavingException(String msg) {
		super(msg);
	}
}
