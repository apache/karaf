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

package org.apache.felix.sigil.ui.eclipse.ui.preferences.repository;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryType;
import org.apache.felix.sigil.ui.eclipse.ui.SigilUI;
import org.apache.felix.sigil.ui.eclipse.ui.wizard.repository.RepositoryWizard;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

public class WizardHelper {
	public static RepositoryWizard loadWizard(IRepositoryType type) throws CoreException {
		IConfigurationElement e = findWizardConfig(type.getId());
		
		if ( e == null ) {
			throw SigilCore.newCoreException("No wizard registered for repository " + type, null);
		}
		
		return (RepositoryWizard) e.createExecutableExtension("class");
	}
	
	public static boolean hasWizard(IRepositoryType type) {
		return findWizardConfig(type.getId()) != null;
	}	
		
	private static IConfigurationElement findWizardConfig(String id) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();		
		IExtensionPoint p = registry.getExtensionPoint(SigilUI.REPOSITORY_WIZARD_EXTENSION_POINT_ID);
		
		for ( IExtension e : p.getExtensions() ) {
			for ( IConfigurationElement c : e.getConfigurationElements() ) {
				if ( id.equals( c.getAttribute("repository") ) ) {
					return c;
				}
			}
		}
		
		return null;
	}
	
	
}
