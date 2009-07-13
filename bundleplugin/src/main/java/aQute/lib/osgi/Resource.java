/* Copyright 2006 aQute SARL 
 * Licensed under the Apache License, Version 2.0, see http://www.apache.org/licenses/LICENSE-2.0 */
package aQute.lib.osgi;

import java.io.*;

public interface Resource {
	InputStream openInputStream() throws IOException ;
	void write(OutputStream out) throws IOException;
	long lastModified();
	void setExtra(String extra);
	String getExtra();	
}
