/*
 * Copyright (c) OSGi Alliance (2002, 2008). All Rights Reserved.
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
package org.osgi.service.io;

import java.io.*;

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;

/**
 * The Connector Service should be called to create and open
 * <code>javax.microedition.io.Connection</code> objects.
 * 
 * When an <code>open*</code> method is called, the implementation of the
 * Connector Service will examine the specified name for a scheme. The Connector
 * Service will then look for a Connection Factory service which is registered
 * with the service property <code>IO_SCHEME</code> which matches the scheme. The
 * <code>createConnection</code> method of the selected Connection Factory will
 * then be called to create the actual <code>Connection</code> object.
 * 
 * <p>
 * If more than one Connection Factory service is registered for a particular
 * scheme, the service with the highest ranking (as specified in its
 * <code>service.ranking</code> property) is called. If there is a tie in ranking,
 * the service with the lowest service ID (as specified in its
 * <code>service.id</code> property), that is the service that was registered
 * first, is called. This is the same algorithm used by
 * <code>BundleContext.getServiceReference</code>.
 * 
 * @version $Revision: 5673 $
 */
public interface ConnectorService {
	/**
	 * Read access mode.
	 * 
	 * @see "javax.microedition.io.Connector.READ"
	 */
	public static final int	READ		= Connector.READ;
	/**
	 * Write access mode.
	 * 
	 * @see "javax.microedition.io.Connector.WRITE"
	 */
	public static final int	WRITE		= Connector.WRITE;
	/**
	 * Read/Write access mode.
	 * 
	 * @see "javax.microedition.io.Connector.READ_WRITE"
	 */
	public static final int	READ_WRITE	= Connector.READ_WRITE;

	/**
	 * Create and open a <code>Connection</code> object for the specified name.
	 * 
	 * @param name The URI for the connection.
	 * @return A new <code>javax.microedition.io.Connection</code> object.
	 * @throws IllegalArgumentException If a parameter is invalid.
	 * @throws javax.microedition.io.ConnectionNotFoundException If the
	 *         connection cannot be found.
	 * @throws IOException If some other kind of I/O error occurs.
	 * @see "javax.microedition.io.Connector.open(String name)"
	 */
	public Connection open(String name) throws IOException;

	/**
	 * Create and open a <code>Connection</code> object for the specified name and
	 * access mode.
	 * 
	 * @param name The URI for the connection.
	 * @param mode The access mode.
	 * @return A new <code>javax.microedition.io.Connection</code> object.
	 * @throws IllegalArgumentException If a parameter is invalid.
	 * @throws javax.microedition.io.ConnectionNotFoundException If the
	 *         connection cannot be found.
	 * @throws IOException If some other kind of I/O error occurs.
	 * @see "javax.microedition.io.Connector.open(String name, int mode)"
	 */
	public Connection open(String name, int mode) throws IOException;

	/**
	 * Create and open a <code>Connection</code> object for the specified name,
	 * access mode and timeouts.
	 * 
	 * @param name The URI for the connection.
	 * @param mode The access mode.
	 * @param timeouts A flag to indicate that the caller wants timeout
	 *        exceptions.
	 * @return A new <code>javax.microedition.io.Connection</code> object.
	 * @throws IllegalArgumentException If a parameter is invalid.
	 * @throws javax.microedition.io.ConnectionNotFoundException If the
	 *         connection cannot be found.
	 * @throws IOException If some other kind of I/O error occurs.
	 * @see "<code>javax.microedition.io.Connector.open</code>"
	 */
	public Connection open(String name, int mode, boolean timeouts)
			throws IOException;

	/**
	 * Create and open an <code>InputStream</code> object for the specified name.
	 * 
	 * @param name The URI for the connection.
	 * @return An <code>InputStream</code> object.
	 * @throws IllegalArgumentException If a parameter is invalid.
	 * @throws javax.microedition.io.ConnectionNotFoundException If the
	 *         connection cannot be found.
	 * @throws IOException If some other kind of I/O error occurs.
	 * @see "javax.microedition.io.Connector.openInputStream(String name)"
	 */
	public InputStream openInputStream(String name) throws IOException;

	/**
	 * Create and open a <code>DataInputStream</code> object for the specified
	 * name.
	 * 
	 * @param name The URI for the connection.
	 * @return A <code>DataInputStream</code> object.
	 * @throws IllegalArgumentException If a parameter is invalid.
	 * @throws javax.microedition.io.ConnectionNotFoundException If the
	 *         connection cannot be found.
	 * @throws IOException If some other kind of I/O error occurs.
	 * @see "javax.microedition.io.Connector.openDataInputStream(String name)"
	 */
	public DataInputStream openDataInputStream(String name) throws IOException;

	/**
	 * Create and open an <code>OutputStream</code> object for the specified name.
	 * 
	 * @param name The URI for the connection.
	 * @return An <code>OutputStream</code> object.
	 * @throws IllegalArgumentException If a parameter is invalid.
	 * @throws javax.microedition.io.ConnectionNotFoundException If the
	 *         connection cannot be found.
	 * @throws IOException If some other kind of I/O error occurs.
	 * @see "javax.microedition.io.Connector.openOutputStream(String name)"
	 */
	public OutputStream openOutputStream(String name) throws IOException;

	/**
	 * Create and open a <code>DataOutputStream</code> object for the specified
	 * name.
	 * 
	 * @param name The URI for the connection.
	 * @return A <code>DataOutputStream</code> object.
	 * @throws IllegalArgumentException If a parameter is invalid.
	 * @throws javax.microedition.io.ConnectionNotFoundException If the
	 *         connection cannot be found.
	 * @throws IOException If some other kind of I/O error occurs.
	 * @see "javax.microedition.io.Connector.openDataOutputStream(String name)"
	 */
	public DataOutputStream openDataOutputStream(String name)
			throws IOException;
}
