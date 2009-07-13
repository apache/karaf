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

package org.cauldron.bld.core.internal.license;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Pattern;

import org.cauldron.bld.core.licence.ILicenseManager;
import org.cauldron.bld.core.licence.ILicensePolicy;
//import org.cauldron.sigil.model.project.ISigilProjectModel;

public class LicenseManager implements ILicenseManager {

	private HashMap<String, Pattern> licenses = new HashMap<String, Pattern>();
	private HashMap<String, LicensePolicy> policies = new HashMap<String, LicensePolicy>();
	private LicensePolicy defaultPolicy = new LicensePolicy(this);
	
	public void addLicense(String name, Pattern pattern) {
		licenses.put( name, pattern );
	}

	public void removeLicense(String name) {
		licenses.remove(name);
	}

	public Set<String> getLicenseNames() {
		return Collections.unmodifiableSet(licenses.keySet());
	}

	public Pattern getLicensePattern(String name) {
		return licenses.get( name );
	}

	public ILicensePolicy getDefaultPolicy() {
		return defaultPolicy;
	}

	//public ILicensePolicy getPolicy(ISigilProjectModel project) {
	//	synchronized( policies ) {
	//		LicensePolicy p = policies.get(project.getName());
	//		
	//		if ( p == null ) {
	//			p = new LicensePolicy(this, project);
	//			policies.put( project.getName(), p );
	//		}
	//		
	//		return p;
	//	}
	//}

}
