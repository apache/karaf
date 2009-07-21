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


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.sigil.ui.eclipse.ui.editors.project.IElementDescriptor;
import org.apache.felix.sigil.ui.eclipse.ui.editors.project.WrappedContentProposal;
import org.apache.felix.sigil.utils.GlobCompiler;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;


public class ExclusionContentProposalProvider<T> implements IContentProposalProvider
{

    private final Collection<? extends T> elements;
    private final IFilter<? super T> filter;
    private final IElementDescriptor<? super T> descriptor;


    public ExclusionContentProposalProvider( Collection<? extends T> elements, IFilter<? super T> filter,
        IElementDescriptor<? super T> descriptor )
    {
        this.elements = elements;
        this.filter = filter;
        this.descriptor = descriptor;
    }


    public IContentProposal[] getProposals( String contents, int position )
    {
        String matchString = contents.substring( 0, position );
        Pattern pattern = GlobCompiler.compile( matchString );

        // Get a snapshot of the elements
        T[] elementArray;
        synchronized ( elements )
        {
            @SuppressWarnings("unchecked")
            T[] temp = ( T[] ) elements.toArray();
            elementArray = temp;
        }

        List<IContentProposal> result = new ArrayList<IContentProposal>();

        for ( T element : elementArray )
        {
            if ( filter != null && filter.select( element ) )
            {
                IContentProposal proposal = WrappedContentProposal.newInstance( element, descriptor );
                Matcher matcher = pattern.matcher( proposal.getContent() );
                if ( matcher.find() )
                {
                    result.add( proposal );
                }
            }
        }

        return result.toArray( new IContentProposal[result.size()] );
    }

}
