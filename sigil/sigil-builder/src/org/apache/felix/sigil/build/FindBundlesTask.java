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
package org.apache.felix.sigil.build;

import java.io.File;
import java.io.FilenameFilter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.osgi.framework.Version;

public class FindBundlesTask extends Task {

	private File dir;
	private String symbolicName;
	private String property;

	public File getDir() {
		return dir;
	}

	public void setDir(File dir) {
		this.dir = dir;
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	public void setSymbolicName(String symbolicName) {
		this.symbolicName = symbolicName;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	@Override
	public void execute() throws BuildException {
		System.out.println("Searching " + dir + " for bundle '" + symbolicName + "'");
		final String prefix = symbolicName + "_";
		String[] files = dir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith(prefix);
			}
		});
		if (files == null)
		    files = new String[0];

		System.out.println("Found " + files.length + " file(s) starting with " + symbolicName);

		Version highest = null;
		for (String filename : files) {
			System.out.println("Testing " + filename);
			// Drop the prefix
			int startIndex = prefix.length();

			// Drop the ".jar" suffix if present
			int endIndex = filename.length();
			if (filename.toLowerCase().endsWith(".jar")) {
				endIndex -= 4;
			}

			String versionString = filename.substring(startIndex, endIndex);
			System.out.println("Version string is '" + versionString + "'");

			Version version = new Version(versionString);
			if (highest == null || version.compareTo(highest) > 0) {
				highest = version;
			}
		}

		if (highest == null) {
			throw new BuildException("No matches for symbolic name '"
					+ symbolicName + "'");
		}

		getProject().setNewProperty(property, highest.toString());
	}

}
