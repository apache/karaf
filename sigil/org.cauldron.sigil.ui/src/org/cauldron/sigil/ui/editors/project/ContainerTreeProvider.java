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

package org.cauldron.sigil.ui.editors.project;

import org.cauldron.sigil.ui.util.DefaultTreeContentProvider;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;

public class ContainerTreeProvider extends DefaultTreeContentProvider {

	private static final Object[] EMPTY = new Object[] {};
	
	public Object[] getChildren(Object parentElement) {
		if ( parentElement instanceof IContainer ) {
			IContainer f = (IContainer) parentElement;
			try {
				return f.members();
			} catch (CoreException e) {
				DebugPlugin.log( e.getStatus() );
			}
		}
		return EMPTY;	
	}

	public Object getParent(Object element) {
		IResource r = (IResource) element;
		return r.getParent();
	}

	public boolean hasChildren(Object element) {
		if ( element instanceof IContainer ) {
			return true;
		}
		else {
			return false;
		}
	}

	public Object[] getElements(Object inputElement) {
		IContainer container = (IContainer) inputElement;
		return getChildren(container);
	}

}
