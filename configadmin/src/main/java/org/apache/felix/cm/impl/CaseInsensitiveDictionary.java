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
package org.apache.felix.cm.impl;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;


/**
 * The <code>CaseInsensitiveDictionary</code> is a
 * <code>java.util.Dictionary</code> which conforms to the requirements laid
 * out by the Configuration Admin Service Specification requiring the property
 * names to keep case but to ignore case when accessing the properties.
 */
class CaseInsensitiveDictionary extends Dictionary
{

    /**
     * The backend dictionary with lower case keys.
     */
    private Hashtable internalMap;

    /**
     * Mapping of lower case keys to original case keys as last used to set a
     * property value.
     */
    private Hashtable originalKeys;


    CaseInsensitiveDictionary()
    {
        internalMap = new Hashtable();
        originalKeys = new Hashtable();
    }


    CaseInsensitiveDictionary( Dictionary props )
    {
        this();

        Enumeration keys = props.keys();
        while ( keys.hasMoreElements() )
        {
            Object key = keys.nextElement();

            // check the correct syntax of the key
            checkKey( key );

            // check uniqueness of key
            String lowerCase = ( ( String ) key ).toLowerCase();
            if ( internalMap.containsKey( lowerCase ) )
            {
                throw new IllegalArgumentException( "Key [" + key + "] already present in different case" );
            }

            // check the value
            Object value = props.get( key );
            value = checkValue( value );

            // add the key/value pair
            internalMap.put( lowerCase, value );
            originalKeys.put( lowerCase, key );
        }
    }


    CaseInsensitiveDictionary( CaseInsensitiveDictionary props, boolean deepCopy )
    {
        Hashtable tmp = new Hashtable( Math.max( 2 * props.internalMap.size(), 11 ), 0.75f );
        if ( deepCopy )
        {
            Iterator entries = props.internalMap.entrySet().iterator();
            while ( entries.hasNext() )
            {
                Map.Entry entry = ( Map.Entry ) entries.next();
                Object value = entry.getValue();
                if ( value.getClass().isArray() )
                {
                    // copy array
                    int length = Array.getLength( value );
                    Object newValue = Array.newInstance( value.getClass().getComponentType(), length );
                    System.arraycopy( value, 0, newValue, 0, length );
                    value = newValue;
                }
                else if ( value instanceof Collection )
                {
                    // copy collection, create Vector
                    // a Vector is created because the R4 and R4.1 specs
                    // state that the values must be simple, array or
                    // Vector. And even though we accept Collection nowadays
                    // there might be clients out there still written against
                    // R4 and R4.1 spec expecting Vector
                    value = new Vector( ( Collection ) value );
                }
                tmp.put( entry.getKey(), value );
            }
        }
        else
        {
            tmp.putAll( props.internalMap );
        }
        
        internalMap = tmp;
        originalKeys = new Hashtable( props.originalKeys );
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Dictionary#elements()
     */
    public Enumeration elements()
    {
        return Collections.enumeration( internalMap.values() );
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Dictionary#get(java.lang.Object)
     */
    public Object get( Object key )
    {
        if ( key == null )
        {
            throw new NullPointerException( "key" );
        }

        String stringKey = String.valueOf( key ).toLowerCase();
        return internalMap.get( stringKey );
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Dictionary#isEmpty()
     */
    public boolean isEmpty()
    {
        return internalMap.isEmpty();
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Dictionary#keys()
     */
    public Enumeration keys()
    {
        return Collections.enumeration( originalKeys.values() );
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Dictionary#put(java.lang.Object, java.lang.Object)
     */
    public Object put( Object key, Object value )
    {
        if ( key == null || value == null )
        {
            throw new NullPointerException( "key or value" );
        }

        checkKey( key );
        value = checkValue( value );

        String lowerCase = String.valueOf( key ).toLowerCase();
        originalKeys.put( lowerCase, key );
        return internalMap.put( lowerCase, value );
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Dictionary#remove(java.lang.Object)
     */
    public Object remove( Object key )
    {
        if ( key == null )
        {
            throw new NullPointerException( "key" );
        }

        String lowerCase = String.valueOf( key ).toLowerCase();
        originalKeys.remove( lowerCase );
        return internalMap.remove( lowerCase );
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Dictionary#size()
     */
    public int size()
    {
        return internalMap.size();
    }


    // ---------- internal -----------------------------------------------------

    /**
     * Ensures the <code>key</code> complies with the <em>symbolic-name</em>
     * production of the OSGi core specification (1.3.2):
     * 
     * <pre>
     * symbolic-name :: = token('.'token)*
     * digit    ::= [0..9]
     * alpha    ::= [a..zA..Z]
     * alphanum ::= alpha | digit
     * token    ::= ( alphanum | ’_’ | ’-’ )+
     * </pre>
     * 
     * If the key does not comply an <code>IllegalArgumentException</code> is
     * thrown.
     * 
     * @param key
     *            The configuration property key to check.
     * @throws IllegalArgumentException
     *             if the key does not comply with the symbolic-name production.
     */
    static void checkKey( Object keyObject )
    {
        if ( !( keyObject instanceof String ) )
        {
            throw new IllegalArgumentException( "Key [" + keyObject + "] must be a String" );
        }

        String key = ( String ) keyObject;
        if ( key.startsWith( "." ) || key.endsWith( "." ) )
        {
            throw new IllegalArgumentException( "Key [" + key + "] must not start or end with a dot" );
        }

        int lastDot = Integer.MIN_VALUE;
        for ( int i = 0; i < key.length(); i++ )
        {
            char c = key.charAt( i );
            if ( c == '.' )
            {
                if ( lastDot == i - 1 )
                {
                    throw new IllegalArgumentException( "Key [" + key + "] must not have consecutive dots" );
                }
                lastDot = i;
            }
            else if ( ( c < '0' || c > '9' ) && ( c < 'a' || c > 'z' ) && ( c < 'A' || c > 'Z' ) && c != '_'
                && c != '-' )
            {
                throw new IllegalArgumentException( "Key [" + key + "] contains illegal character" );
            }
        }
    }


    static Object checkValue( Object value )
    {
        Class type;
        if ( value == null )
        {
            // null is illegal
            throw new IllegalArgumentException( "Value must not be null" );

        }
        else if ( value.getClass().isArray() )
        {
            // check simple or primitive
            type = value.getClass().getComponentType();

            // check for primitive type (simple types are checked below)
            // note: void[] cannot be created, so we ignore this here
            if ( type.isPrimitive() )
            {
                return value;
            }

        }
        else if ( value instanceof Collection )
        {
            // check simple
            Collection collection = ( Collection ) value;
            if ( collection.isEmpty() )
            {
                throw new IllegalArgumentException( "Collection must not be empty" );
            }

            // ensure all elements have the same type and to internal list
            Collection internalValue = new ArrayList( collection.size() );
            type = null;
            for ( Iterator ci = collection.iterator(); ci.hasNext(); )
            {
                Object el = ci.next();
                if ( el == null )
                {
                    throw new IllegalArgumentException( "Collection must not contain null elements" );
                }
                if ( type == null )
                {
                    type = el.getClass();
                }
                else if ( type != el.getClass() )
                {
                    throw new IllegalArgumentException( "Collection element types must not be mixed" );
                }
                internalValue.add( el );
            }
            value = internalValue;
        }
        else
        {
            // get the type to check (must be simple)
            type = value.getClass();

        }

        // check for simple type
        if ( type == String.class || type == Integer.class || type == Long.class || type == Float.class
            || type == Double.class || type == Byte.class || type == Short.class || type == Character.class
            || type == Boolean.class )
        {
            return value;
        }

        // not a valid type
        throw new IllegalArgumentException( "Value [" + value + "] has unsupported (base-) type " + type );
    }


    // ---------- Object Overwrites --------------------------------------------

    public String toString()
    {
        return internalMap.toString();
    }

}
