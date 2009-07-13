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

import org.cauldron.sigil.actions.PruneProjectDependenciesAction;
import org.cauldron.sigil.actions.ResolveProjectDependenciesAction;
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

public class ToolsSection extends SigilSection {
	
	public ToolsSection(SigilPage page, Composite parent, ISigilProjectModel project) throws CoreException {
		super( page, parent, project );
	}    

    protected void createSection(Section section,FormToolkit toolkit ) {
        setTitle("Tools");
        
		Composite body = createTableWrapBody(1, toolkit);

        toolkit.createLabel( body, "Tools to help manage this project:" );
        
        Hyperlink launch = toolkit.createHyperlink( body, "Resolve missing dependencies", SWT.NULL );
        launch.setHref( "resolve" );
        launch.addHyperlinkListener(this);
        
        Hyperlink debug = toolkit.createHyperlink( body, "Prune unused dependencies", SWT.NULL );
        debug.setHref( "prune" );
        debug.addHyperlinkListener(this);
    }

	public void linkActivated(HyperlinkEvent e) {
		String href = (String) e.getHref();
		if ( "resolve".equals( href ) ) {
			handleResolve();
		}
		else if ( "prune".equals( href ) ) {
			handlePrune();
		}
	}

	private void handlePrune() {
		new PruneProjectDependenciesAction(getProjectModel()).run();
	}

	private void handleResolve() {
		final ISigilProjectModel project = getProjectModel();
		new ResolveProjectDependenciesAction(project, true).run();
	}

	public void linkEntered(HyperlinkEvent e) {
	}

	public void linkExited(HyperlinkEvent e) {
	}
	
}
