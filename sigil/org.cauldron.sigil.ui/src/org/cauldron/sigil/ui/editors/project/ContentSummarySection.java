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

import org.cauldron.sigil.model.project.ISigilProjectModel;
import org.cauldron.sigil.ui.form.SigilPage;
import org.cauldron.sigil.ui.form.SigilSection;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;

public class ContentSummarySection extends SigilSection {

	public ContentSummarySection(SigilPage page, Composite parent, ISigilProjectModel project) throws CoreException {
		super( page, parent, project );
	}

	@Override
	protected void createSection(Section section, FormToolkit toolkit) {
		setTitle( "Project Content");
		
		Composite body = createTableWrapBody(2, toolkit);
		Hyperlink link = toolkit.createHyperlink( body, "Contents:", SWT.NONE );
		link.setHref( ContentsForm.PAGE_ID );
		link.addHyperlinkListener(this);
		toolkit.createLabel( body, "Manage the content that this bundle provides." );
		
		link = toolkit.createHyperlink( body, "Dependencies:", SWT.NONE );
		link.setHref( DependenciesForm.PAGE_ID );
		link.addHyperlinkListener(this);
		toolkit.createLabel( body, "Manage the dependencies that this bundle needs to run." );
		
		link = toolkit.createHyperlink( body, "Exports:", SWT.NONE );
		link.setHref( ExportsForm.PAGE_ID );
		link.addHyperlinkListener(this);
		toolkit.createLabel( body, "Manage the resources that this bundle exports." );
	}

	@Override
	public void linkActivated(HyperlinkEvent e) {
		getPage().getEditor().setActivePage( (String) e.getHref() );
	}
}
