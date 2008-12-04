package aQute.lib.osgi;

import java.io.*;

public class JarResource implements Resource {
	Jar		jar;
	String extra;
	
	public JarResource(Jar jar ) {
		this.jar = jar;
	}
	
	public String getExtra() {
		return extra;
	}

	public long lastModified() {
		return jar.lastModified();
	}


	public void write(OutputStream out) throws IOException {
		jar.write(out);
	}
	
	public InputStream openInputStream() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		write(out);
		out.close();
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		return in;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}
	
	public Jar getJar() { 
	    return jar;
	}

}
