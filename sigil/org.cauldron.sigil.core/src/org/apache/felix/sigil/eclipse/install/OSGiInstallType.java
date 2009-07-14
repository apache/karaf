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

package org.apache.felix.sigil.eclipse.install;

import java.util.Arrays;

import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.graphics.Image;

public class OSGiInstallType implements IOSGiInstallType {

	private String id;
	private String name;
	private String version;
	private String mainClass;
	private String[] classPath;
	private IPath javaDocLocation;
	private IPath sourceLocation;
	private IPath[] defaultBundleLocations;
	private Image icon;
	
	public Image getIcon() {
		return icon;
	}

	public void setIcon(Image icon) {
		this.icon = icon;
	}

	public String getId() {
		return id;
	}
	
	public String[] getClassPath() {
		return classPath;
	}

	public IPath[] getDefaultBundleLocations() {
		return defaultBundleLocations;
	}

	public IPath getJavaDocLocation() {
		return javaDocLocation;
	}

	public String getMainClass() {
		return mainClass;
	}

	public String getName() {
		return name;
	}

	public IPath getSourceLocation() {
		return sourceLocation;
	}

	public String getVersion() {
		return version;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	public void setClassPath(String[] classPath) {
		this.classPath = classPath;
	}

	public void setJavaDocLocation(IPath javaDocLocation) {
		this.javaDocLocation = javaDocLocation;
	}

	public void setSourceLocation(IPath sourceLocation) {
		this.sourceLocation = sourceLocation;
	}

	public void setDefaultBundleLocations(IPath[] defaultBundleLocations) {
		this.defaultBundleLocations = defaultBundleLocations;
	}
	
	public String toString() {
		return "OSGiInstallType[\n" + 
			"name=" + name + "\n" +
			"version=" + version + "\n" +
			"mainClass=" + mainClass + "\n" +
			"classPath=" + Arrays.asList(classPath) + "\n" +
			"javaDocLocation=" + javaDocLocation + "\n" +
			"sourceLocation=" + sourceLocation + "\n" +
			"defaultBundleLocations=" + Arrays.asList(defaultBundleLocations) + "\n" +
			"]";
	}
}
