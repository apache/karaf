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

package org.apache.felix.sigil.ui.eclipse.ui.editors.project;

import java.util.Set;

import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.ui.eclipse.ui.util.ModelLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Table;

public class ProjectTableViewer extends TableViewer {

	private ModelLabelProvider labelProvider;

	public ProjectTableViewer(Table table) {
		super(table);
		labelProvider = new ModelLabelProvider();
		setLabelProvider(labelProvider);
	}

	@Override
	public void setContentProvider(IContentProvider provider) {
		super.setContentProvider(provider);
		setInput(getTable());
	}

	public void setUnresolvedElements(Set<? extends IModelElement> elements) {
		labelProvider.setUnresolvedElements(elements);
	}

	@Override
	public ModelLabelProvider getLabelProvider() {
		return labelProvider;
	}
	
	
}