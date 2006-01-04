/*
 * $Header: /cvshome/build/org.osgi.service.url/src/org/osgi/service/url/URLStreamHandlerSetter.java,v 1.6 2005/05/13 20:32:35 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2002, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.service.url;

import java.net.URL;

/**
 * Interface used by <code>URLStreamHandlerService</code> objects to call the
 * <code>setURL</code> method on the proxy <code>URLStreamHandler</code> object.
 * 
 * <p>
 * Objects of this type are passed to the
 * {@link URLStreamHandlerService#parseURL}method. Invoking the <code>setURL</code>
 * method on the <code>URLStreamHandlerSetter</code> object will invoke the
 * <code>setURL</code> method on the proxy <code>URLStreamHandler</code> object that
 * is actually registered with <code>java.net.URL</code> for the protocol.
 * 
 * @version $Revision: 1.6 $
 */
public interface URLStreamHandlerSetter {
	/**
	 * @see "java.net.URLStreamHandler.setURL(URL,String,String,int,String,String)"
	 * 
	 * @deprecated This method is only for compatibility with handlers written
	 *             for JDK 1.1.
	 */
	public void setURL(URL u, String protocol, String host, int port,
			String file, String ref);

	/**
	 * @see "java.net.URLStreamHandler.setURL(URL,String,String,int,String,String,String,String)"
	 */
	public void setURL(URL u, String protocol, String host, int port,
			String authority, String userInfo, String path, String query,
			String ref);
}