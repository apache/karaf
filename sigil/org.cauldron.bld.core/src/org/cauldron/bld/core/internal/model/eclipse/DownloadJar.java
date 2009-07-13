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

package org.cauldron.bld.core.internal.model.eclipse;

import java.util.HashSet;
import java.util.Set;

import org.cauldron.sigil.model.AbstractCompoundModelElement;
import org.cauldron.sigil.model.eclipse.IDownloadJar;
import org.eclipse.core.runtime.IPath;

public class DownloadJar extends AbstractCompoundModelElement implements IDownloadJar {

	private static final long serialVersionUID = 1L;

	private Set<IPath> entries = new HashSet<IPath>();
	
	public DownloadJar() {
		super("RMI Classpath Download Jar");
	}
	
	public void addEntry(IPath entry) {
		entries.add( entry );
	}
	
	public void removeEntry(IPath entry) {
		entries.remove( entry );
	}
	
	public Set<IPath> getEntrys() {
		return entries;
	}

	public void clearEntries() {
		entries.clear();
	}
}
