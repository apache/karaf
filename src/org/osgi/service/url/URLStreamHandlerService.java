/*
 * $Header: /cvshome/build/org.osgi.service.url/src/org/osgi/service/url/URLStreamHandlerService.java,v 1.6 2005/05/13 20:32:35 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2002, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.service.url;

import java.net.*;

/**
 * Service interface with public versions of the protected
 * <code>java.net.URLStreamHandler</code> methods.
 * <p>
 * The important differences between this interface and the
 * <code>URLStreamHandler</code> class are that the <code>setURL</code> method is
 * absent and the <code>parseURL</code> method takes a
 * {@link URLStreamHandlerSetter}object as the first argument. Classes
 * implementing this interface must call the <code>setURL</code> method on the
 * <code>URLStreamHandlerSetter</code> object received in the <code>parseURL</code>
 * method instead of <code>URLStreamHandler.setURL</code> to avoid a
 * <code>SecurityException</code>.
 * 
 * @see AbstractURLStreamHandlerService
 * 
 * @version $Revision: 1.6 $
 */
public interface URLStreamHandlerService {
	/**
	 * @see "java.net.URLStreamHandler.openConnection"
	 */
	public URLConnection openConnection(URL u) throws java.io.IOException;

	/**
	 * Parse a URL. This method is called by the <code>URLStreamHandler</code>
	 * proxy, instead of <code>java.net.URLStreamHandler.parseURL</code>, passing
	 * a <code>URLStreamHandlerSetter</code> object.
	 * 
	 * @param realHandler The object on which <code>setURL</code> must be invoked
	 *        for this URL.
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