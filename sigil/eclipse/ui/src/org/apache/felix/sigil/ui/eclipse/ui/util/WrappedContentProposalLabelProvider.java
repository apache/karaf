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

package org.apache.felix.sigil.ui.eclipse.ui.util;


import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;


public class WrappedContentProposalLabelProvider<E> extends LabelProvider
{

    private final IElementDescriptor<? super E> descriptor;
    private final ModelLabelProvider projectLabelProvider;


    public WrappedContentProposalLabelProvider( IElementDescriptor<? super E> descriptor )
    {
        this.descriptor = descriptor;
        projectLabelProvider = new ModelLabelProvider();
    }


    @SuppressWarnings("unchecked")
    private E adapt( Object element )
    {
        E result;
        if ( element instanceof WrappedContentProposal<?> )
        {
            WrappedContentProposal<?> proposal = (org.apache.felix.sigil.ui.eclipse.ui.util.WrappedContentProposal<?> ) element;
            result = ( E ) proposal.getElement();
        }
        else
        {
            result = ( E ) element;
        }
        return result;
    }


    @Override
    public Image getImage( Object element )
    {
        Object value = adapt( element );
        return projectLabelProvider.getImage( value );
    }


    @Override
    public String getText( Object element )
    {
        return descriptor.getLabel( adapt( element ) );
    }
}
