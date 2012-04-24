/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.diagnostic.core.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.karaf.diagnostic.core.DumpDestination;

/**
 * Class which packages dumps to given directory.
 */
public class DirectoryDumpDestination implements DumpDestination {

	/**
	 * Directory where dump files will be created.
	 */
	private File directory;

	public DirectoryDumpDestination(File file) {
		this.directory = file;

		if (!file.exists()) {
			file.mkdirs();
		} 
	}

	public OutputStream add(String name) throws Exception {
		File destination = new File(directory, name);
		if (name.contains("/") || name.contains("\\")) {
			// if name contains slashes we need to create sub directory
			destination.getParentFile().mkdirs();
		}
		return new FileOutputStream(destination);
	}

	public void save() throws Exception {
		// do nothing, all should be written to output streams
	}
	
}
