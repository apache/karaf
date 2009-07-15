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

package org.apache.felix.sigil.utils.properties;

import org.apache.felix.sigil.utils.SigilUtils;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPart;

public class EditorPropertyTester extends PropertyTester {

	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		IWorkbenchPart part = (IWorkbenchPart) receiver;

		boolean result = false;
		
		if(part instanceof IEditorPart) {
			IEditorInput input = ((IEditorPart) part).getEditorInput();
			if(input instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) input).getFile();
				
				if("isEditorOfType".equals(property)) {
					result = SigilUtils.isResourceType(file, (String) expectedValue);
				}
			}
		}
		
		return result;
	}

}
