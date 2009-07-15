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

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;

public class Link {	
	public static class Unsatisfied {

	}

	private ISigilBundle source;
	private ISigilBundle target;
	
	private LinkedList<IModelElement> requirements = new LinkedList<IModelElement>();
	private static final Comparator<IModelElement> comparator = new Comparator<IModelElement>() {

		public int compare(IModelElement o1, IModelElement o2) {
			if (o1 instanceof IRequiredBundle) {
				if ( o2 instanceof IRequiredBundle) {
					return compareBundles( (IRequiredBundle) o1, (IRequiredBundle) o2 );
				}
				else {
					return -1;
				}
			}
			else {
				if ( o2 instanceof IRequiredBundle ) {
					return 1;
				}
				else {
					return compareNonBundles( o1, o2 );
				}
			}
		}

		private int compareNonBundles(IModelElement o1, IModelElement o2) {
			if (o1 instanceof IPackageImport) {
				if ( o2 instanceof IPackageImport) {
					return compareImports( (IPackageImport) o1, (IPackageImport) o2 );
				}
				else {
					return -1;
				}
			}
			else {
				if ( o2 instanceof IPackageImport ) {
					return 1;
				}
				else {
					return 0;
				}
			}
		}

		private int compareImports(IPackageImport o1, IPackageImport o2) {
			return o1.getPackageName().compareTo( o2.getPackageName() );
		}

		private int compareBundles(IRequiredBundle o1, IRequiredBundle o2) {
			return o1.getSymbolicName().compareTo( o2.getSymbolicName() );
		}
		
	};
	
	public Link(ISigilBundle source, ISigilBundle target) {
		this.source = source;
		this.target = target;
	}

	public ISigilBundle getSource() {
		return source;
	}
	
	public ISigilBundle getTarget() {
		return target;
	}
	
	public boolean isSatisfied() {
		return target != null;
	}

	public void addRequirement(IModelElement requirement) {
		requirements.add(requirement);
		Collections.sort(requirements, comparator);
	}
	
	public String toString() {
		return "Link[" + source + "->" + target + "]";
	}

	public List<IModelElement> getRequirements() {
		return requirements;
	}

	public boolean isOptional() {
		for ( IModelElement e : requirements ) {
			if ( e instanceof IPackageImport ) {
				IPackageImport pi = (IPackageImport) e;
				if ( !pi.isOptional() ) {
					return false;
				}
			}
			else if ( e instanceof IRequiredBundle ) {
				IRequiredBundle rb = (IRequiredBundle) e;
				if ( !rb.isOptional() ) {
					return false;
				}
			}
		}
		return true;
	}
}
