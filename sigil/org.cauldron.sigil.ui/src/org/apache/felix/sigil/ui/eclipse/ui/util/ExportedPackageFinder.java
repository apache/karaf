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

package org.apache.felix.sigil.ui.eclipse.ui.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.model.IModelWalker;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.progress.IJobRunnable;

public class ExportedPackageFinder implements IJobRunnable {
	
	private final IAccumulator<? super IPackageExport> accumulator;
	private final ISigilProjectModel sigil;

	public ExportedPackageFinder(ISigilProjectModel sigil, IAccumulator<? super IPackageExport> accumulator) {
		this.sigil = sigil;
		this.accumulator = accumulator;
	}

	public IStatus run(final IProgressMonitor monitor) {
		final List<IPackageExport> exports = new ArrayList<IPackageExport>(ResourcesDialogHelper.UPDATE_BATCH_SIZE);
		final IModelWalker walker = new IModelWalker() {
			public boolean visit(IModelElement element) {
				if ( element instanceof IPackageExport ) {
					IPackageExport pkgExport = (IPackageExport) element;
					exports.add(pkgExport);

					if(exports.size() >= ResourcesDialogHelper.UPDATE_BATCH_SIZE) {
						accumulator.addElements(exports);
						exports.clear();
					}
				}
				return !monitor.isCanceled();
			}
		};
		SigilCore.getRepositoryManager(sigil).visit(walker);
		if(exports.size() > 0) {
			accumulator.addElements(exports);
		}

		return Status.OK_STATUS;
	}
}