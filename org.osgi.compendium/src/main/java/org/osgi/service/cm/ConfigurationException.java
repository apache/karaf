/*
 * Copyright (c) OSGi Alliance (2001, 2008). All Rights Reserved.
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
package org.osgi.service.cm;

/**
 * An <code>Exception</code> class to inform the Configuration Admin service
 * of problems with configuration data.
 * 
 * @version $Revision: 6083 $
 */
public class ConfigurationException extends Exception {
	static final long	serialVersionUID	= -1690090413441769377L;

	private final String		property;
	private final String		reason;

	/**
	 * Create a <code>ConfigurationException</code> object.
	 * 
	 * @param property name of the property that caused the problem,
	 *        <code>null</code> if no specific property was the cause
	 * @param reason reason for failure
	 */
	public ConfigurationException(String property, String reason) {
		super(property + " : " + reason);
		this.property = property;
		this.reason = reason;
	}

	/**
	 * Create a <code>ConfigurationException</code> object.
	 * 
	 * @param property name of the property that caused the problem,
	 *        <code>null</code> if no specific property was the cause
	 * @param reason reason for failure
	 * @param cause The cause of this exception.
	 * @since 1.2
	 */
	public ConfigurationException(String property, String reason,
			Throwable cause) {
		super(property + " : " + reason, cause);
		this.property = property;
		this.reason = reason;
	}

	/**
	 * Return the property name that caused the failure or null.
	 * 
	 * @return name of property or null if no specific property caused the
	 *         problem
	 */
	public String getProperty() {
		return property;
	}

	/**
	 * Return the reason for this exception.
	 * 
	 * @return reason of the failure
	 */
	public String getReason() {
		return reason;
	}
	
	/**
	 * Returns the cause of this exception or <code>null</code> if no cause was
	 * set.
	 * 
	 * @return The cause of this exception or <code>null</code> if no cause was
	 *         set.
	 * @since 1.2
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
	 * @since 1.2
	 */
	public Throwable initCause(Throwable cause) {
		return super.initCause(cause);
	}
}
