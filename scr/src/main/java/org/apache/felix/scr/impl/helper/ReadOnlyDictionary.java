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


import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;


/**
 * The <code>ReadOnlyDictionary</code> is a <code>Dictionary</code> whose
 * {@link #put(Object, Object)} and {@link #remove(Object)} methods have
 * no effect and always return <code>null</code>.
 */
public class ReadOnlyDictionary extends Dictionary implements Map
{

    private final Hashtable m_delegatee;


    public ReadOnlyDictionary( final Dictionary delegatee )
    {
        if ( delegatee instanceof Hashtable )
        {
            this.m_delegatee = ( Hashtable ) delegatee;
        }
        else
        {
            this.m_delegatee = new Hashtable();
            for ( Enumeration ke = delegatee.elements(); ke.hasMoreElements(); )
            {
                Object key = ke.nextElement();
                this.m_delegatee.put( key, delegatee.get( key ) );
            }
        }
    }


    //---------- Dictionary API

    public Enumeration elements()
    {
        return m_delegatee.elements();
    }


    public Object get( final Object key )
    {
        return m_delegatee.get( key );
    }


    public boolean isEmpty()
    {
        return m_delegatee.isEmpty();
    }


    public Enumeration keys()
    {
        return m_delegatee.keys();
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
        return m_delegatee.size();
    }


    public String toString()
    {
        return m_delegatee.toString();
    }


    //---------- Map API

    public void clear()
    {
        // nop, this map is read only
    }


    public boolean containsKey( Object key )
    {
        return m_delegatee.containsKey( key );
    }


    public boolean containsValue( Object value )
    {
        return m_delegatee.containsValue( value );
    }


    public Set entrySet()
    {
        return Collections.unmodifiableSet( m_delegatee.entrySet() );
    }


    public Set keySet()
    {
        return Collections.unmodifiableSet( m_delegatee.keySet() );
    }


    public void putAll( Map m )
    {
        // nop, this map is read only
    }


    public Collection values()
    {
        return Collections.unmodifiableCollection( m_delegatee.values() );
    }
}
