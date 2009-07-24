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

import java.util.Enumeration;

/**
 * Provides methods to retrieve <code>LogEntry</code> objects from the log.
 * <p>
 * There are two ways to retrieve <code>LogEntry</code> objects:
 * <ul>
 * <li>The primary way to retrieve <code>LogEntry</code> objects is to register a
 * <code>LogListener</code> object whose <code>LogListener.logged</code> method will
 * be called for each entry added to the log.
 * <li>To retrieve past <code>LogEntry</code> objects, the <code>getLog</code>
 * method can be called which will return an <code>Enumeration</code> of all
 * <code>LogEntry</code> objects in the log.
 * 
 * @ThreadSafe
 * @version $Revision: 5654 $
 * @see LogEntry
 * @see LogListener
 * @see LogListener#logged(LogEntry)
 */
public interface LogReaderService {
	/**
	 * Subscribes to <code>LogEntry</code> objects.
	 * 
	 * <p>
	 * This method registers a <code>LogListener</code> object with the Log Reader
	 * Service. The <code>LogListener.logged(LogEntry)</code> method will be
	 * called for each <code>LogEntry</code> object placed into the log.
	 * 
	 * <p>
	 * When a bundle which registers a <code>LogListener</code> object is stopped
	 * or otherwise releases the Log Reader Service, the Log Reader Service must
	 * remove all of the bundle's listeners.
	 * 
	 * <p>
	 * If this Log Reader Service's list of listeners already contains a
	 * listener <code>l</code> such that <code>(l==listener)</code>, this method
	 * does nothing.
	 * 
	 * @param listener A <code>LogListener</code> object to register; the
	 *        <code>LogListener</code> object is used to receive <code>LogEntry</code>
	 *        objects.
	 * @see LogListener
	 * @see LogEntry
	 * @see LogListener#logged(LogEntry)
	 */
	public void addLogListener(LogListener listener);

	/**
	 * Unsubscribes to <code>LogEntry</code> objects.
	 * 
	 * <p>
	 * This method unregisters a <code>LogListener</code> object from the Log
	 * Reader Service.
	 * 
	 * <p>
	 * If <code>listener</code> is not contained in this Log Reader Service's list
	 * of listeners, this method does nothing.
	 * 
	 * @param listener A <code>LogListener</code> object to unregister.
	 * @see LogListener
	 */
	public void removeLogListener(LogListener listener);

	/**
	 * Returns an <code>Enumeration</code> of all <code>LogEntry</code> objects in
	 * the log.
	 * 
	 * <p>
	 * Each element of the enumeration is a <code>LogEntry</code> object, ordered
	 * with the most recent entry first. Whether the enumeration is of all
	 * <code>LogEntry</code> objects since the Log Service was started or some
	 * recent past is implementation-specific. Also implementation-specific is
	 * whether informational and debug <code>LogEntry</code> objects are included
	 * in the enumeration.
	 * @return An <code>Enumeration</code> of all <code>LogEntry</code> objects in
	 * the log.
	 */
	public Enumeration getLog();
}
