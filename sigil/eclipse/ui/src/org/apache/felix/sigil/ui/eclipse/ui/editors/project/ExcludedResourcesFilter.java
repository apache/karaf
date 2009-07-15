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

package org.apache.felix.sigil.ui.eclipse.ui.editors.project;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.preferences.PrefsUtils;
import org.apache.felix.sigil.utils.GlobCompiler;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class ExcludedResourcesFilter extends ViewerFilter {

	private final Set<Pattern> exclusionSet = new HashSet<Pattern>();
	
	public ExcludedResourcesFilter() {
		loadExclusions();
	}

	public final synchronized void loadExclusions() {
		exclusionSet.clear();
		IPreferenceStore store = SigilCore.getDefault().getPreferenceStore();
		String[] exclusions = PrefsUtils.stringToArray(store.getString(SigilCore.DEFAULT_EXCLUDED_RESOURCES));
		for (String exclusion : exclusions) {
			exclusionSet.add(GlobCompiler.compile(exclusion));
		}
	}

	@Override
	public synchronized boolean select(Viewer viewer, Object parentElement, Object element) {
		IResource file = (IResource) element;
		String path = file.getName();
		for ( Pattern p :exclusionSet ) {
			if ( p.matcher(path).matches() ) {
				return false;
			}
		}
		return true;
	}

}
