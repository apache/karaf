package org.apache.karaf.diagnostic.core;

/**
 * Destination for created dumps.
 * 
 * @author ldywicki
 */
public interface DumpDestination {

	void add(Dump ... dump) throws Exception;

	void save() throws Exception;

}
