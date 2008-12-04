/* Copyright 2006 aQute SARL 
 * Licensed under the Apache License, Version 2.0, see http://www.apache.org/licenses/LICENSE-2.0 */
package aQute.lib.osgi;

import java.io.*;
import java.util.regex.Pattern;

public class FileResource implements Resource {
	File	file;
	String	extra;
	
	public FileResource(File file) {
		this.file = file;
	}

	public InputStream openInputStream() throws FileNotFoundException {
		return new FileInputStream(file);
	}

	public static void build(Jar jar, File directory, Pattern doNotCopy) {
		traverse(
				jar,
				directory.getAbsolutePath().length(),
				directory,
				doNotCopy);
	}

	public String toString() {
		return ":" + file.getName() + ":";
	}

	public void write(OutputStream out) throws IOException {
		copy(this, out);
	}

	static synchronized void copy(Resource resource, OutputStream out)
			throws IOException {
		InputStream in = resource.openInputStream();
		try {
			byte buffer[] = new byte[20000];
			int size = in.read(buffer);
			while (size > 0) {
				out.write(buffer, 0, size);
				size = in.read(buffer);
			}
		}
		finally {
			in.close();
		}
	}

	static void traverse(Jar jar, int rootlength, File directory,
			Pattern doNotCopy) {
		if (doNotCopy.matcher(directory.getName()).matches())
			return;

		File files[] = directory.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory())
				traverse(jar, rootlength, files[i], doNotCopy);
			else {
				String path = files[i].getAbsolutePath().substring(
						rootlength + 1);
				if (File.separatorChar != '/')
					path = path.replace(File.separatorChar, '/');
				jar.putResource(path, new FileResource(files[i]), true);
			}
		}
	}

	public long lastModified() {
		return file.lastModified();
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}
	
	public long size() {
	    return (int) file.length();
	}
}
