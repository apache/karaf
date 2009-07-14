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

package org.apache.felix.sigil.core.internal.license;

import org.apache.felix.sigil.core.licence.ILicensePolicy;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.eclipse.core.runtime.IProgressMonitor;

public class LicensePolicy implements ILicensePolicy {

	private LicenseManager licenseManager;
	
	public LicensePolicy(LicenseManager licenseManager) {
		this.licenseManager = licenseManager;
	}

	public boolean accept(ISigilBundle bundle) {
		return true;
	}

	public void addAllowed(String licenseName) {
		// TODO Auto-generated method stub
		
	}

	public void removeAllowed(String licenseName) {
		// TODO Auto-generated method stub
		
	}

	public void save(IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		
	}

}
