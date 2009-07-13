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

package org.cauldron.bld.core.repository;

import org.eclipse.core.runtime.IProgressMonitor;
import org.cauldron.sigil.repository.IResolutionMonitor;

public class ProgressWrapper implements IProgressMonitor {

	private IResolutionMonitor monitor;
	
	public ProgressWrapper(IResolutionMonitor monitor) {
		this.monitor = monitor;
	}

	public boolean isCanceled() {
		return monitor.isCanceled();
	}

	public void beginTask(String name, int totalWork) {
		// TODO Auto-generated method stub

	}

	public void done() {
		// TODO Auto-generated method stub

	}

	public void internalWorked(double work) {
		// TODO Auto-generated method stub

	}

	public void setCanceled(boolean value) {
		// TODO Auto-generated method stub

	}

	public void setTaskName(String name) {
		// TODO Auto-generated method stub

	}

	public void subTask(String name) {
		// TODO Auto-generated method stub

	}

	public void worked(int work) {
		// TODO Auto-generated method stub

	}
}
