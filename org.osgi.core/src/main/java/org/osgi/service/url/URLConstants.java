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

/**
 * Defines standard names for property keys associated with
 * {@link URLStreamHandlerService} and <code>java.net.ContentHandler</code>
 * services.
 * 
 * <p>
 * The values associated with these keys are of type
 * <code>java.lang.String[]</code> or <code>java.lang.String</code>, unless
 * otherwise indicated.
 * 
 * @version $Revision: 5673 $
 */
public interface URLConstants {
	/**
	 * Service property naming the protocols serviced by a
	 * URLStreamHandlerService. The property's value is a protocol name or an
	 * array of protocol names.
	 */
	public static final String	URL_HANDLER_PROTOCOL	= "url.handler.protocol";
	/**
	 * Service property naming the MIME types serviced by a
	 * java.net.ContentHandler. The property's value is a MIME type or an array
	 * of MIME types.
	 */
	public static final String	URL_CONTENT_MIMETYPE	= "url.content.mimetype";
}
