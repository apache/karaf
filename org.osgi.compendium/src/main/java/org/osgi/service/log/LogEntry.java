/*
 * Copyright (c) OSGi Alliance (2000, 2008). All Rights Reserved.
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
package org.osgi.service.log;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * Provides methods to access the information contained in an individual Log
 * Service log entry.
 * 
 * <p>
 * A <code>LogEntry</code> object may be acquired from the
 * <code>LogReaderService.getLog</code> method or by registering a
 * <code>LogListener</code> object.
 * 
 * @ThreadSafe
 * @version $Revision: 5654 $
 * @see LogReaderService#getLog
 * @see LogListener
 */
public interface LogEntry {
	/**
	 * Returns the bundle that created this <code>LogEntry</code> object.
	 * 
	 * @return The bundle that created this <code>LogEntry</code> object;
	 *         <code>null</code> if no bundle is associated with this
	 *         <code>LogEntry</code> object.
	 */
	public Bundle getBundle();

	/**
	 * Returns the <code>ServiceReference</code> object for the service associated
	 * with this <code>LogEntry</code> object.
	 * 
	 * @return <code>ServiceReference</code> object for the service associated
	 *         with this <code>LogEntry</code> object; <code>null</code> if no
	 *         <code>ServiceReference</code> object was provided.
	 */
	public ServiceReference getServiceReference();

	/**
	 * Returns the severity level of this <code>LogEntry</code> object.
	 * 
	 * <p>
	 * This is one of the severity levels defined by the <code>LogService</code>
	 * interface.
	 * 
	 * @return Severity level of this <code>LogEntry</code> object.
	 * 
	 * @see LogService#LOG_ERROR
	 * @see LogService#LOG_WARNING
	 * @see LogService#LOG_INFO
	 * @see LogService#LOG_DEBUG
	 */
	public int getLevel();

	/**
	 * Returns the human readable message associated with this <code>LogEntry</code>
	 * object.
	 * 
	 * @return <code>String</code> containing the message associated with this
	 *         <code>LogEntry</code> object.
	 */
	public String getMessage();

	/**
	 * Returns the exception object associated with this <code>LogEntry</code>
	 * object.
	 * 
	 * <p>
	 * In some implementations, the returned exception may not be the original
	 * exception. To avoid references to a bundle defined exception class, thus
	 * preventing an uninstalled bundle from being garbage collected, the Log
	 * Service may return an exception object of an implementation defined
	 * Throwable subclass. The returned object will attempt to provide as much
	 * information as possible from the original exception object such as the
	 * message and stack trace.
	 * 
	 * @return <code>Throwable</code> object of the exception associated with this
	 *         <code>LogEntry</code>;<code>null</code> if no exception is
	 *         associated with this <code>LogEntry</code> object.
	 */
	public Throwable getException();

	/**
	 * Returns the value of <code>currentTimeMillis()</code> at the time this
	 * <code>LogEntry</code> object was created.
	 * 
	 * @return The system time in milliseconds when this <code>LogEntry</code>
	 *         object was created.
	 * @see "System.currentTimeMillis()"
	 */
	public long getTime();
}
