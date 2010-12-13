package org.apache.karaf.diagnostic.common;

import java.io.File;

import org.apache.karaf.diagnostic.core.DumpDestination;
import org.apache.karaf.diagnostic.core.DumpProvider;
import org.apache.karaf.diagnostic.core.common.FileDump;

public class LogDumpProvider implements DumpProvider {

	public void createDump(DumpDestination destination) throws Exception {
		File logDir = new File("data/log");
		File[] listFiles = logDir.listFiles();

		for (File file : listFiles) {
			destination.add(new FileDump(file));
		}
	}

}
