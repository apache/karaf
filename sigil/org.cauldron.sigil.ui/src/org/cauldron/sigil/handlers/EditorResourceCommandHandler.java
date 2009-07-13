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

package org.cauldron.sigil.handlers;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;

public abstract class EditorResourceCommandHandler extends AbstractResourceCommandHandler {
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Shell shell = HandlerUtil.getActiveShell(event);
		final IEditorPart editorPart = HandlerUtil.getActiveEditor(event);
		if ( editorPart != null ) {
			IEditorInput editorInput = editorPart.getEditorInput();
			
			if(!(editorInput instanceof IFileEditorInput)) {
				throw new ExecutionException("Editor input must be a file");
			}
			IFileEditorInput fileInput = (IFileEditorInput) editorInput;
			
			try {
				// Save the editor content (if dirty)
				IRunnableWithProgress saveOperation = new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
						if(editorPart.isDirty()) {
							if(MessageDialog.openQuestion(shell, "Save File", "The file contents must be saved before the command can be executed. Do you wish to save now?")) {
								editorPart.doSave(monitor);
							} else {
								throw new InterruptedException();
							}
						}
					}
				};
				new ProgressMonitorDialog(shell).run(false, true, saveOperation);
	
				// Execute on the file
				IFile file = fileInput.getFile();
				getResourceCommandHandler().execute(new IResource[] { file }, event);
			} catch (InvocationTargetException e) {
				throw new ExecutionException("Error saving file", e.getTargetException());
			} catch (InterruptedException e) {
				// Exit the command silently
			}
		}			
		return null;
	}

}
