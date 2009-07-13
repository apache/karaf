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

package org.cauldron.sigil.repository;

import org.eclipse.core.runtime.IProgressMonitor;
import org.cauldron.sigil.model.IModelElement;
import org.cauldron.sigil.model.eclipse.ISigilBundle;

public class ResolutionMonitorAdapter implements IResolutionMonitor {

	private IProgressMonitor monitor;
	
	public ResolutionMonitorAdapter(IProgressMonitor monitor) {
		this.monitor = monitor;
	}

	public boolean isCanceled() {
		return monitor.isCanceled();
	}

	public void startResolution(IModelElement requirement) {
		monitor.subTask( "Resolving " + requirement);
	}

	public void endResolution(IModelElement requirement, ISigilBundle provider) {
		monitor.subTask( (provider == null ? "Failed to resolve " : "Resolved ") + requirement);
	}

}
