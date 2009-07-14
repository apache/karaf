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

import java.util.concurrent.Callable;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.ui.eclipse.ui.SigilUI;
import org.apache.felix.sigil.ui.eclipse.ui.preferences.OptionalPrompt;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

public class ProjectUtils {
	public static boolean runTaskWithRebuildCheck(final Runnable task, Shell shell) {
		return runTaskWithRebuildCheck( new Callable<Boolean>() {
			public Boolean call() throws Exception {
				task.run();
				return true;
			}
		}, shell);
	}
	
	public static boolean runTaskWithRebuildCheck(Callable<Boolean> callable,
			Shell shell) {
		int result = checkRebuild(shell);
		if ( result == IDialogConstants.CANCEL_ID ) {
			return false;
		}
		else {
			try {
				if ( Boolean.TRUE == callable.call() ) {
					if ( result == IDialogConstants.YES_ID ) {
						SigilUI.runWorkspaceOperation( new WorkspaceModifyOperation() {
							@Override
							protected void execute(IProgressMonitor monitor) {
								SigilCore.rebuildAllBundleDependencies(monitor);
							}
						}, shell );
					}
					return true;
				}
				else {
					return false;
				}
			} catch (Exception e) {
				SigilCore.error( "Failed to run caller", e);
				return false;
			}
		}
	}
	
	private static int checkRebuild(Shell shell) {
		if ( SigilCore.getRoot().getProjects().isEmpty() ) {
			return IDialogConstants.NO_ID;
		}
		else {
			return OptionalPrompt.optionallyPromptWithCancel(SigilCore.getDefault().getPreferenceStore(), SigilCore.PREFERENCES_REBUILD_PROJECTS, "Rebuild", "Do you wish to rebuild all Sigil projects", shell );
		}
	}
}
