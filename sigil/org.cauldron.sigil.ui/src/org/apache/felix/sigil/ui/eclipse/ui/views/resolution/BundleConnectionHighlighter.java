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

package org.apache.felix.sigil.ui.eclipse.ui.views.resolution;

import java.util.Set;

import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.core.widgets.GraphItem;
import org.eclipse.zest.core.widgets.GraphNode;

public class BundleConnectionHighlighter implements ISelectionChangedListener {

	private BundleResolverView view;
	
	public BundleConnectionHighlighter(BundleResolverView view) {
		this.view = view;
	}
	
	public void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		if ( !selection.isEmpty() ) {
			IStructuredSelection str = (IStructuredSelection) selection;

			Object sel = str.getFirstElement();
			
			if ( sel instanceof ISigilBundle ) {
				BundleGraph graph = (BundleGraph) view.getBundlegraph();

				ISigilBundle selected = (ISigilBundle) sel;
				Set<ISigilBundle> connected = graph.getTargets(selected);

				highlightLinks(graph, selected, connected);
				highlightBundles(graph, selected, connected);
			}
			else if ( sel instanceof Link ) {
				GraphConnection c = (GraphConnection) findGraphItem(sel);
				if ( c != null ) {
					c.unhighlight();
					c.setHighlightColor(ColorConstants.blue);
					c.highlight();
				}
			}
		}
		else {
			// TODO clear highlights...
		}
	}

	private void highlightBundles(BundleGraph graph, ISigilBundle selected, Set<ISigilBundle> connected) {
		for ( ISigilBundle bundle : graph.getBundles() ) {
			GraphNode node = (GraphNode) findGraphItem(bundle);
			if ( node != null ) {
				node.unhighlight();

				if ( bundle == selected ) {
					node.setHighlightColor(ColorConstants.yellow);
					node.highlight();
				}
				else if ( view.isDisplayed(BundleResolverView.DEPENDENTS) ) {
					if ( connected.contains(bundle) ) {
						node.setHighlightColor(ColorConstants.lightBlue);
						node.highlight();
					}
				}
			}
		}
	}
	
	private void highlightLinks(BundleGraph graph, ISigilBundle selected, Set<ISigilBundle> connected) {
		for ( Link l : graph.getLinks() ) {
			GraphConnection c = (GraphConnection) findGraphItem(l);
			if ( c != null ) {
				c.unhighlight();

				if ( view.isDisplayed(BundleResolverView.DEPENDENTS) ) {
					if ( l.getSource() == selected && connected.contains( l.getTarget() ) ) {
						c.setHighlightColor(ColorConstants.lightBlue);
						c.highlight();
					}
				}
			}
		}
	}

	private GraphItem findGraphItem(Object l) {
		try {
			return view.getGraphViewer().findGraphItem(l);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			// temporary fix for issue 
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=242523
			return null;
		}
	}

}
