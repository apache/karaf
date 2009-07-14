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

package org.apache.felix.sigil.core.internal.model.eclipse;

import org.apache.felix.sigil.model.AbstractModelElement;
import org.apache.felix.sigil.model.common.VersionRange;
import org.apache.felix.sigil.model.eclipse.ILibraryImport;

public class LibraryImport extends AbstractModelElement implements ILibraryImport {

	private static final long serialVersionUID = 1L;

	public LibraryImport() {
		super("Library Import");
	}

	private String libraryName;
	private VersionRange range = VersionRange.ANY_VERSION;
	
	public String getLibraryName() {
		return libraryName;
	}

	public VersionRange getVersions() {
		return range;
	}

	public void setLibraryName(String name) {
		this.libraryName = name;
	}

	public void setVersions(VersionRange range) {
		this.range = range;
	}

}
