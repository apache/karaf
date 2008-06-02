/*
 * $Header: /cvshome/build/org.osgi.service.io/src/org/osgi/service/io/ConnectionFactory.java,v 1.9 2006/07/12 21:22:12 hargrave Exp $
 *
 * Copyright (c) OSGi Alliance (2002, 2006). All Rights Reserved.
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

import java.io.IOException;

import javax.microedition.io.Connection;

/**
 * A Connection Factory service is called by the implementation of the Connector
 * Service to create <code>javax.microedition.io.Connection</code> objects which
 * implement the scheme named by <code>IO_SCHEME</code>.
 * 
 * When a <code>ConnectorService.open</code> method is called, the implementation
 * of the Connector Service will examine the specified name for a scheme. The
 * Connector Service will then look for a Connection Factory service which is
 * registered with the service property <code>IO_SCHEME</code> which matches the
 * scheme. The {@link #createConnection} method of the selected Connection
 * Factory will then be called to create the actual <code>Connection</code>
 * object.
 * 
 * @version $Revision: 1.9 $
 */
public interface ConnectionFactory {
	/**
	 * Service property containing the scheme(s) for which this Connection
	 * Factory can create <code>Connection</code> objects. This property is of
	 * type <code>String[]</code>.
	 */
	public static final String	IO_SCHEME	= "io.scheme";

	/**
	 * Create a new <code>Connection</code> object for the specified URI.
	 * 
	 * @param name The full URI passed to the <code>ConnectorService.open</code>
	 *        method
	 * @param mode The mode parameter passed to the
	 *        <code>ConnectorService.open</code> method
	 * @param timeouts The timeouts parameter passed to the
	 *        <code>ConnectorService.open</code> method
	 * @return A new <code>javax.microedition.io.Connection</code> object.
	 * @throws IOException If a <code>javax.microedition.io.Connection</code>
	 *         object can not not be created.
	 */
	public Connection createConnection(String name, int mode, boolean timeouts)
			throws IOException;
}
