package org.apache.karaf.shell.console.impl.jline;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jline.console.history.FileHistory;

/**
 * Override the FileHistory impl to trap failures due to the
 * user does not having write access to the history file.
 */
public final class KarafFileHistory extends FileHistory {
	static final Logger LOGGER = LoggerFactory.getLogger(KarafFileHistory.class);
	boolean failed = false;

	public KarafFileHistory(File file) throws IOException {
		super(file);
	}

	@Override
	public void flush() throws IOException {
	    if( !failed ) {
	        try {
	            super.flush();
	        } catch (IOException e) {
	            failed = true;
	            LOGGER.debug("Could not write history file: "+ getFile(), e);
	        }
	    }
	}

	@Override
	public void purge() throws IOException {
	    if( !failed ) {
	        try {
	            super.purge();
	        } catch (IOException e) {
	            failed = true;
	            LOGGER.debug("Could not delete history file: "+ getFile(), e);
	        }
	    }
	}
}