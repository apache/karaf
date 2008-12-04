/* Copyright 2006 aQute SARL 
 * Licensed under the Apache License, Version 2.0, see http://www.apache.org/licenses/LICENSE-2.0 */
package aQute.lib.osgi;

import java.io.*;
import java.net.*;

public class URLResource implements Resource {
	URL	url;
	String	extra;
	
	public URLResource(URL url) {
		this.url = url;
	}

	public InputStream openInputStream() throws IOException {
		return url.openStream();
	}

	public String toString() {
		return ":" + url.getPath() + ":";
	}

	public void write(OutputStream out) throws IOException {
		FileResource.copy(this, out);
	}

	public long lastModified() {
		return -1;
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}
}
