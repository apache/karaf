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

package org.cauldron.sigil.ui.views;

import org.cauldron.sigil.model.osgi.IPackageExport;
import org.cauldron.sigil.model.osgi.IPackageImport;
import org.cauldron.sigil.model.osgi.IRequiredBundle;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

public class ModelElementComparator extends ViewerComparator {
	private static final int EXPORT_GROUP = 0;
	private static final int IMPORT_GROUP = 1;
	private static final int REQUIRE_GROUP = 2;
	private static final int OTHER_GROUP = 4;
	public int category(Object element) {
		if ( element instanceof IPackageImport ) {
			return IMPORT_GROUP;
		}
		else if ( element instanceof IPackageExport ) {
			return EXPORT_GROUP;
		}
		else if ( element instanceof IRequiredBundle ) {
			return REQUIRE_GROUP;
		}
		else {
			return OTHER_GROUP;
		}
	}
	@SuppressWarnings("unchecked")
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
        int cat1 = category(e1);
        int cat2 = category(e2);

        if (cat1 != cat2) {
			return cat1 - cat2;
		}
        
        if ( cat1 == OTHER_GROUP ) {
            String name1;
            String name2;

            if (viewer == null || !(viewer instanceof ContentViewer)) {
                name1 = e1.toString();
                name2 = e2.toString();
            } else {
                IBaseLabelProvider prov = ((ContentViewer) viewer)
                        .getLabelProvider();
                if (prov instanceof ILabelProvider) {
                    ILabelProvider lprov = (ILabelProvider) prov;
                    name1 = lprov.getText(e1);
                    name2 = lprov.getText(e2);
                } else {
                    name1 = e1.toString();
                    name2 = e2.toString();
                }
            }
            if (name1 == null) {
    			name1 = "";//$NON-NLS-1$
    		}
            if (name2 == null) {
    			name2 = "";//$NON-NLS-1$
    		}

            // use the comparator to compare the strings
            return getComparator().compare(name1, name2);
        }
        else {
        	Comparable c1 = (Comparable) e1;
        	Comparable c2 = (Comparable) e2;
        	return c1.compareTo(c2);
        }
	}
}
