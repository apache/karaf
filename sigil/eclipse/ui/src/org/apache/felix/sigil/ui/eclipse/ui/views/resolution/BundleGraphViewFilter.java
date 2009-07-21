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


import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;


public class BundleGraphViewFilter extends ViewerFilter
{

    private BundleResolverView view;


    public BundleGraphViewFilter( BundleResolverView view )
    {
        this.view = view;
    }


    @Override
    public boolean select( Viewer viewer, Object parentElement, Object element )
    {
        if ( !view.isDisplayed( BundleResolverView.LOCAL_LINKS ) )
        {
            if ( element instanceof Link )
            {
                Link l = ( Link ) element;
                return l.getSource() != l.getTarget();
            }
        }
        if ( !view.isDisplayed( BundleResolverView.SATISFIED ) )
        {
            if ( element instanceof Link )
            {
                Link l = ( Link ) element;
                return !l.isSatisfied();
            }
            else if ( element instanceof ISigilBundle )
            {
                ISigilBundle bundle = ( ISigilBundle ) element;
                for ( Link l : view.getBundlegraph().getLinks( bundle ) )
                {
                    if ( !l.isSatisfied() )
                    {
                        return true;
                    }
                }
                return false;
            }
        }

        if ( !view.isDisplayed( BundleResolverView.UNSATISFIED ) )
        {
            if ( element instanceof Link )
            {
                Link l = ( Link ) element;
                return l.isSatisfied();
            }
            else if ( element instanceof Link.Unsatisfied )
            {
                return false;
            }
        }
        return true;
    }

}
