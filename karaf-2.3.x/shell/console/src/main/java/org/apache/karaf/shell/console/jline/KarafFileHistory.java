/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.console.jline;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import jline.console.history.FileHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Override the FileHistory impl to trap failures due to the
 * user does not having write access to the history file.
 */
public final class KarafFileHistory extends FileHistory {

	static final Logger LOGGER = LoggerFactory.getLogger(KarafFileHistory.class);
	boolean failed = false;
    boolean loading = false;

	public KarafFileHistory(File file) throws IOException {
		super(file);
	}

    @Override
    public void add(CharSequence item) {
        if (!loading) {
            item = item.toString().replaceAll("\\!", "\\\\!");
        }
        super.add(item);
    }

    @Override
    public void load(Reader reader) throws IOException {
        loading = true;
        try {
            super.load(reader);
        } finally {
            loading = false;
        }
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