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

package org.cauldron.sigil.ui.views.resolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.cauldron.sigil.model.IModelElement;
import org.cauldron.sigil.model.eclipse.ISigilBundle;

public class BundleGraph {

	private HashMap<ISigilBundle, LinkedList<Link>> lookup = new HashMap<ISigilBundle, LinkedList<Link>>();
	private LinkedList<Link> links = new LinkedList<Link>();
	private HashSet<ISigilBundle> bundles = new HashSet<ISigilBundle>();
	
	public void startResolution(IModelElement requirement) {
	}

	public void endResolution(IModelElement requirement, ISigilBundle target) {
		ISigilBundle source = requirement.getAncestor(ISigilBundle.class);

		bundles.add(source);
		bundles.add(target);

		LinkedList<Link> links = lookup.get(source);

		if ( links == null ) {
			links = new LinkedList<Link>();
			lookup.put(source, links);
		}

		Link l = null;
		for ( Link c : links ) {
			if ( c.getTarget() == target ) {
				l = c;
				break;
			}
		}

		if ( l == null ) {
			l = new Link(source, target);
			links.add(l);
			this.links.add(l);
		}

		l.addRequirement(requirement);
	}
	
	public List<Link> getLinks() {
		return links;
	}

	public Set<ISigilBundle> getBundles() {
		return bundles;
	}

	public Set<ISigilBundle> getTargets(ISigilBundle bundle) {
		HashSet<ISigilBundle> targets = new HashSet<ISigilBundle>();
		
		for ( Link l : getLinks(bundle) ) {
			targets.add(l.getTarget());
		}
		
		return targets;
	}

	public List<Link> getLinks(ISigilBundle selected) {
		List<Link> l = lookup.get(selected);
		return l == null ? Collections.<Link>emptyList() : l;
	}

	public List<Link> getDependentLinks(ISigilBundle bundle) {
		ArrayList<Link> found = new ArrayList<Link>(links.size());
		
		for  (Link l : links) {
			if ( l.getTarget() == bundle ) {
				found.add( l );
			}
		}
		
		found.trimToSize();
		
		return found;
	}
}
