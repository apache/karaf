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

package org.cauldron.sigil.preferences;


import org.cauldron.sigil.SigilCore;
import org.cauldron.sigil.internal.model.repository.RepositoryConfiguration;
import org.cauldron.sigil.model.common.VersionRangeBoundingRule;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class SigilPreferencesInitializer extends AbstractPreferenceInitializer {
	
	public static final String[] EXCLUDED_RESOURCES = new String[] {
		".project", ".classpath", ".settings"
	};
	
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = SigilCore.getDefault().getPreferenceStore();

		store.setDefault(SigilCore.OSGI_INSTALL_CHECK_PREFERENCE, true);

		store.setDefault(SigilCore.DEFAULT_VERSION_LOWER_BOUND, VersionRangeBoundingRule.Micro.name());
		store.setDefault(SigilCore.DEFAULT_VERSION_UPPER_BOUND, VersionRangeBoundingRule.Any.name());

		store.setDefault(SigilCore.DEFAULT_EXCLUDED_RESOURCES, PrefsUtils.arrayToString(EXCLUDED_RESOURCES));
		
		store.setDefault(SigilCore.PREFERENCES_NOASK_OSGI_INSTALL, false);
		
		store.setDefault(SigilCore.PREFERENCES_ADD_IMPORT_FOR_EXPORT, PromptablePreference.Prompt.name());
		
		store.setDefault(SigilCore.PREFERENCES_REBUILD_PROJECTS, PromptablePreference.Prompt.name() );
		
		store.setDefault(RepositoryConfiguration.REPOSITORY_DEFAULT_SET, "org.cauldron.sigil.core.workspaceprovider" );
	}
}
