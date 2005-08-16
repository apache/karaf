/*
 * $Header: /cvshome/build/org.osgi.service.url/src/org/osgi/service/url/URLConstants.java,v 1.6 2005/05/13 20:32:35 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2002, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.service.url;

/**
 * Defines standard names for property keys associated with
 * {@link URLStreamHandlerService}and <code>java.net.ContentHandler</code>
 * services.
 * 
 * <p>
 * The values associated with these keys are of type <code>java.lang.String[]</code>,
 * unless otherwise indicated.
 * 
 * @version $Revision: 1.6 $
 */
public interface URLConstants {
	/**
	 * Service property naming the protocols serviced by a
	 * URLStreamHandlerService. The property's value is an array of protocol
	 * names.
	 */
	public static final String	URL_HANDLER_PROTOCOL	= "url.handler.protocol";
	/**
	 * Service property naming the MIME types serviced by a
	 * java.net.ContentHandler. The property's value is an array of MIME types.
	 */
	public static final String	URL_CONTENT_MIMETYPE	= "url.content.mimetype";
}