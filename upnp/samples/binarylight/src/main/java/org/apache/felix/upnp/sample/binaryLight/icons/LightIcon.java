/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.upnp.sample.binaryLight.icons;
import java.io.IOException;
import java.io.InputStream;

import org.osgi.service.upnp.UPnPIcon;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/

public class LightIcon implements UPnPIcon {

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPIcon#getMimeType()
	 */
	public String getMimeType() {
		return "image/gif";
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPIcon#getWidth()
	 */
	public int getWidth() {
		return 32;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPIcon#getHeight()
	 */
	public int getHeight() {
		return 32;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPIcon#getSize()
	 */
	public int getSize() {
		return 1072;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPIcon#getDepth()
	 */
	public int getDepth() {
		return 16;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPIcon#getInputStream()
	 */
	public InputStream getInputStream() throws IOException {
		return LightIcon.class.getResourceAsStream("images/LightOn.gif");
	}
}
