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

package org.cauldron.sigil.cheatsheets.actions;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.cheatsheets.ICheatSheetAction;
import org.eclipse.ui.cheatsheets.ICheatSheetManager;
import org.eclipse.ui.part.FileEditorInput;
import org.osgi.framework.Bundle;

public class CopyResourceFromPlugin extends Action implements ICheatSheetAction {

	private String targetProject;
	private String targetFolder;
	private String sourceBundle;
	private String sourcePath;
	private String editorID;
	
	public void run(String[] params, ICheatSheetManager manager) {
		if ( params != null && params.length > 4 )  {
			targetProject = params[0];
			targetFolder = params[1];
			sourceBundle= params[2];
			sourcePath = params[3];
			editorID = params[4];
		}
		
		WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
			@Override
			protected void execute(IProgressMonitor monitor) throws CoreException {
				try {
					Bundle b = Platform.getBundle(sourceBundle);
					
				    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
				    IProject project = workspaceRoot.getProject(targetProject);
				    IPath path = new Path( targetFolder ).append( sourcePath.substring( sourcePath.lastIndexOf( '/' ) ) );
					IFile file = project.getFile( path );
					
					if ( !file.exists() ) {
						mkdirs( (IFolder) file.getParent(), monitor );
						
						InputStream in = FileLocator.openStream(b, new Path(sourcePath), false);
						file.create(in, true, monitor);
					}
					
					IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					FileEditorInput input = new FileEditorInput(file); 
					window.getActivePage().openEditor(input, editorID);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		
		try {
			new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(false, false, op);
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void mkdirs(IFolder folder, IProgressMonitor monitor) throws CoreException {
		IContainer parent = folder.getParent();
		if ( !parent.exists() ) {
			mkdirs((IFolder) parent, monitor);			
		}
		
		if ( !folder.exists() ) {
			folder.create(true, true, monitor);
		}
		
	}		
}
