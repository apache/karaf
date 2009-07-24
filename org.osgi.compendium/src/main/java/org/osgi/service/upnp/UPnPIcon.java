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
package org.osgi.service.upnp;

import java.io.IOException;
import java.io.InputStream;

/**
 * A UPnP icon representation.
 * 
 * Each UPnP device can contain zero or more icons.
 * 
 * @version $Revision: 5673 $
 */
public interface UPnPIcon {
	/**
	 * Returns the MIME type of the icon.
	 * 
	 * This method returns the format in which the icon graphics, read from the
	 * <code>InputStream</code> object obtained by the <code>getInputStream()</code>
	 * method, is encoded.
	 * <p>
	 * The format of the returned string is in accordance to RFC2046. A list of
	 * valid MIME types is maintained by the <a
	 * href="http://www.iana.org/assignments/media-types/">IANA</a>.
	 * <p>
	 * Typical values returned include: "image/jpeg" or "image/gif"
	 * 
	 * @return The MIME type of the encoded icon.
	 */
	String getMimeType();

	/**
	 * Returns the width of the icon in pixels.
	 * 
	 * If the actual width of the icon is unknown, -1 is returned.
	 * 
	 * @return The width in pixels, or -1 if unknown.
	 */
	int getWidth();

	/**
	 * Returns the height of the icon in pixels.
	 * 
	 * If the actual height of the icon is unknown, -1 is returned.
	 * 
	 * @return The height in pixels, or -1 if unknown.
	 */
	int getHeight();

	/**
	 * Returns the size of the icon in bytes.
	 * 
	 * This method returns the number of bytes of the icon available to read
	 * from the <code>InputStream</code> object obtained by the
	 * <code>getInputStream()</code> method. If the actual size can not be
	 * determined, -1 is returned.
	 * 
	 * @return The icon size in bytes, or -1 if the size is unknown.
	 */
	int getSize();

	/**
	 * Returns the color depth of the icon in bits.
	 * 
	 * @return The color depth in bits. If the actual color depth of the icon is
	 *         unknown, -1 is returned.
	 */
	int getDepth();

	/**
	 * Returns an <code>InputStream</code> object for the icon data.
	 * 
	 * The <code>InputStream</code> object provides a way for a client to read the
	 * actual icon graphics data. The number of bytes available from this
	 * <code>InputStream</code> object can be determined via the
	 * <code>getSize()</code> method. The format of the data encoded can be
	 * determined by the MIME type availble via the <code>getMimeType()</code>
	 * method.
	 * 
	 * @return An InputStream to read the icon graphics data from.
	 * @throws IOException If the <code>InputStream</code> cannot be returned.
	 * @see UPnPIcon#getMimeType()
	 */
	InputStream getInputStream() throws IOException;
}
