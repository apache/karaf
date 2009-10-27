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
import org.apache.felix.sigil.ui.eclipse.ui.SigilUI;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;


public class BundleGraphLabelProvider extends LabelProvider
{

    private BundleResolverView view;


    public BundleGraphLabelProvider( BundleResolverView view )
    {
        this.view = view;
    }


    @Override
    public String getText( Object element )
    {
        if ( element instanceof Link )
        {
            Link l = ( Link ) element;
            if ( l.isSatisfied() )
            {
                if ( view.isDisplayed( BundleResolverView.LINK_LABELS ) )
                {
                    return view.getLinkText( ( Link ) element );
                }
                else
                {
                    return "";
                }
            }
            else
            {
                return view.getLinkText( ( Link ) element );
            }
        }
        else if ( element instanceof ISigilBundle )
        {
            ISigilBundle b = ( ISigilBundle ) element;
            return b.getBundleInfo().getSymbolicName() + ": " + b.getBundleInfo().getVersion();
        }
        else if ( element instanceof Link.Unsatisfied )
        {
            return "unsatisfied";
        }
        else
        {
            return "unknown:" + element;
        }
    }


    @Override
    public Image getImage( Object element )
    {
        Image result = null;
        if ( element instanceof ISigilBundle )
        {
            result = SigilUI.cacheImage( "icons/bundle.gif", BundleGraphLabelProvider.class.getClassLoader() );
        }
        else if ( element instanceof Link.Unsatisfied )
        {
            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
        }

        return result;
    }
}
