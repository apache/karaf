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


import org.eclipse.jface.viewers.Viewer;
import org.eclipse.zest.core.viewers.IGraphContentProvider;


public class BundleGraphContentProvider implements IGraphContentProvider
{

    public Object[] getElements( Object input )
    {
        BundleGraph graph = ( BundleGraph ) input;
        return graph.getLinks().toArray();
    }


    public Object getDestination( Object element )
    {
        Link l = ( Link ) element;
        return l.isSatisfied() ? l.getTarget() : new Link.Unsatisfied();
    }


    public Object getSource( Object element )
    {
        Link l = ( Link ) element;
        return l.getSource();
    }


    public void dispose()
    {
        // TODO Auto-generated method stub

    }


    public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
    {

    }

}
