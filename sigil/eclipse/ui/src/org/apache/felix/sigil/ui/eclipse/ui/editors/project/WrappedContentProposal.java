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

package org.apache.felix.sigil.ui.eclipse.ui.editors.project;


import org.eclipse.jface.fieldassist.IContentProposal;


public class WrappedContentProposal<T> implements IContentProposal
{

    private final T element;
    private final IElementDescriptor<? super T> descriptor;


    private WrappedContentProposal( T element, IElementDescriptor<? super T> descriptor )
    {
        this.element = element;
        this.descriptor = descriptor;
    }


    public static <T> WrappedContentProposal<T> newInstance( T element, IElementDescriptor<? super T> descriptor )
    {
        return new WrappedContentProposal<T>( element, descriptor );
    }


    public String getContent()
    {
        return descriptor.getName( element );
    }


    public int getCursorPosition()
    {
        return 0;
    }


    public String getDescription()
    {
        return null;
    }


    public String getLabel()
    {
        return descriptor.getLabel( element );
    }


    public T getElement()
    {
        return element;
    }


    @Override
    public String toString()
    {
        return getLabel();
    }
}
