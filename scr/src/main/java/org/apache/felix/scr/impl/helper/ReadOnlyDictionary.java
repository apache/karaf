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
package org.apache.felix.scr.impl.helper;


import java.util.Dictionary;
import java.util.Enumeration;


/**
 * The <code>ReadOnlyDictionary</code> is a <code>Dictionary</code> whose
 * {@link #put(Object, Object)} and {@link #remove(Object)} methods have
 * no effect and always return <code>null</code>.
 */
public class ReadOnlyDictionary extends Dictionary
{

    private final Dictionary delegatee;


    public ReadOnlyDictionary( final Dictionary delegatee )
    {
        this.delegatee = delegatee;
    }


    public Enumeration elements()
    {
        return delegatee.elements();
    }


    public Object get( final Object key )
    {
        return delegatee.get( key );
    }


    public boolean isEmpty()
    {
        return delegatee.isEmpty();
    }


    public Enumeration keys()
    {
        return delegatee.keys();
    }


    /**
     * This method has no effect and always returns <code>null</code> as this
     * instance is read-only and cannot modify and properties.
     */
    public Object put( final Object key, final Object value )
    {
        return null;
    }


    /**
     * This method has no effect and always returns <code>null</code> as this
     * instance is read-only and cannot modify and properties.
     */
    public Object remove( final Object key )
    {
        return null;
    }


    public int size()
    {
        return delegatee.size();
    }


    public String toString()
    {
        return delegatee.toString();
    }
}
