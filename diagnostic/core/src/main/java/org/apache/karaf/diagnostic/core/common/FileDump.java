package org.apache.karaf.diagnostic.core.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.karaf.diagnostic.core.Dump;

public class FileDump implements Dump {

	private final File file;

	public FileDump(File file) {
		this.file = file;
	}

	public InputStream createResource() throws Exception {
		return new FileInputStream(file);
	}

	public String getName() {
		return file.getName();
	}

}
