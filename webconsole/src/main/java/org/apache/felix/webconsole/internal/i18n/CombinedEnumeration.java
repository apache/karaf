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
package org.apache.felix.webconsole.internal.i18n;


import java.util.Enumeration;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 * The <code>CombinedEnumeration</code> combines two enumerations into a single
 * one first returning everything from the first enumeration and then from the
 * second enumeration with a single limitation: entries are only returned once.
 * So if both enumerations would produce the same result, say "123", only the
 * first would be returned.
 */
class CombinedEnumeration implements Enumeration
{

    // the first enumeration to iterate
    private final Enumeration first;

    // the second enumeration to iterate once the first is exhausted
    private final Enumeration second;

    // the set of values already returned to prevent duplicate entries
    private final Set seenKeys;

    // preview to the next return value for nextElement(), null at the end
    private Object nextKey;


    CombinedEnumeration( final Enumeration first, final Enumeration second )
    {
        this.first = first;
        this.second = second;

        this.seenKeys = new HashSet();
        this.nextKey = seek();
    }


    public boolean hasMoreElements()
    {
        return nextKey != null;
    }


    public Object nextElement()
    {
        if ( !hasMoreElements() )
        {
            throw new NoSuchElementException();
        }

        Object result = nextKey;
        nextKey = seek();
        return result;
    }


    /**
     * Check the enumerations for the next element to return. If no more
     * (unique) element is available, null is returned. The element returned
     * is also added to the set of seen elements to prevent duplicate provision
     */
    private Object seek()
    {
        while ( first.hasMoreElements() )
        {
            final Object next = first.nextElement();
            if ( !seenKeys.contains( next ) )
            {
                seenKeys.add( next );
                return next;
            }
        }
        while ( second.hasMoreElements() )
        {
            final Object next = second.nextElement();
            if ( !seenKeys.contains( next ) )
            {
                seenKeys.add( next );
                return next;
            }
        }
        return null;
    }
}