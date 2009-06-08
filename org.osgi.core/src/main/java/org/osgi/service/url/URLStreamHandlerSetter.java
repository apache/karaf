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

import java.net.URL;

/**
 * Interface used by <code>URLStreamHandlerService</code> objects to call the
 * <code>setURL</code> method on the proxy <code>URLStreamHandler</code>
 * object.
 * 
 * <p>
 * Objects of this type are passed to the
 * {@link URLStreamHandlerService#parseURL} method. Invoking the
 * <code>setURL</code> method on the <code>URLStreamHandlerSetter</code>
 * object will invoke the <code>setURL</code> method on the proxy
 * <code>URLStreamHandler</code> object that is actually registered with
 * <code>java.net.URL</code> for the protocol.
 * 
 * @ThreadSafe
 * @version $Revision: 5673 $
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
