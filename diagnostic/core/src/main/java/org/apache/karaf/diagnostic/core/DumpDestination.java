package org.apache.karaf.diagnostic.core;

import java.io.OutputStream;

/**
 * Destination for created dumps.
 * 
 * @author ldywicki
 */
public interface DumpDestination {

	OutputStream add(String name) throws Exception;

	void save() throws Exception;

}
