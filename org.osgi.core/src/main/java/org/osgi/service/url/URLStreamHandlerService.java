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

package org.osgi.service.url;

import java.net.*;

/**
 * Service interface with public versions of the protected
 * <code>java.net.URLStreamHandler</code> methods.
 * <p>
 * The important differences between this interface and the
 * <code>URLStreamHandler</code> class are that the <code>setURL</code>
 * method is absent and the <code>parseURL</code> method takes a
 * {@link URLStreamHandlerSetter} object as the first argument. Classes
 * implementing this interface must call the <code>setURL</code> method on the
 * <code>URLStreamHandlerSetter</code> object received in the
 * <code>parseURL</code> method instead of
 * <code>URLStreamHandler.setURL</code> to avoid a
 * <code>SecurityException</code>.
 * 
 * @see AbstractURLStreamHandlerService
 * 
 * @ThreadSafe
 * @version $Revision: 5673 $
 */
public interface URLStreamHandlerService {
	/**
	 * @see "java.net.URLStreamHandler.openConnection"
	 */
	public URLConnection openConnection(URL u) throws java.io.IOException;

	/**
	 * Parse a URL. This method is called by the <code>URLStreamHandler</code>
	 * proxy, instead of <code>java.net.URLStreamHandler.parseURL</code>,
	 * passing a <code>URLStreamHandlerSetter</code> object.
	 * 
	 * @param realHandler The object on which <code>setURL</code> must be
	 *        invoked for this URL.
	 * @see "java.net.URLStreamHandler.parseURL"
	 */
	public void parseURL(URLStreamHandlerSetter realHandler, URL u,
			String spec, int start, int limit);

	/**
	 * @see "java.net.URLStreamHandler.toExternalForm"
	 */
	public String toExternalForm(URL u);

	/**
	 * @see "java.net.URLStreamHandler.equals(URL, URL)"
	 */
	public boolean equals(URL u1, URL u2);

	/**
	 * @see "java.net.URLStreamHandler.getDefaultPort"
	 */
	public int getDefaultPort();

	/**
	 * @see "java.net.URLStreamHandler.getHostAddress"
	 */
	public InetAddress getHostAddress(URL u);

	/**
	 * @see "java.net.URLStreamHandler.hashCode(URL)"
	 */
	public int hashCode(URL u);

	/**
	 * @see "java.net.URLStreamHandler.hostsEqual"
	 */
	public boolean hostsEqual(URL u1, URL u2);

	/**
	 * @see "java.net.URLStreamHandler.sameFile"
	 */
	public boolean sameFile(URL u1, URL u2);
}
