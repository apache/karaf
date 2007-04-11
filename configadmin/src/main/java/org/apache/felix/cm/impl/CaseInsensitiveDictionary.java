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


import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


/**
 * The <code>CaseInsensitiveDictionary</code> is a
 * <code>java.util.Dictionary</code> which conforms to the requirements laid
 * out by the Configuration Admin Service Specification requiring the property
 * names to keep case but to ignore case when accessing the properties.
 *
 * @author fmeschbe
 */
class CaseInsensitiveDictionary extends Dictionary
{

    /**
     * The backend dictionary with lower case keys.
     */
    private Hashtable internalMap;

    /**
     * Mapping of lower case keys to original case keys as last used to set
     * a property value.
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
            if ( !( key instanceof String ) )
            {
                throw new IllegalArgumentException( "Key [" + key + "] must be a String" );
            }

            // check uniqueness of key
            String lowerCase = ( ( String ) key ).toLowerCase();
            if ( internalMap.containsKey( lowerCase ) )
            {
                throw new IllegalArgumentException( "Key [" + key + "] already present in different case" );
            }

            // check the value
            Object value = props.get( key );
            checkValue( value );

            // add the key/value pair
            internalMap.put( lowerCase, value );
            originalKeys.put( lowerCase, key );
        }
    }


    CaseInsensitiveDictionary( CaseInsensitiveDictionary props )
    {
        internalMap = new Hashtable( props.internalMap );
        originalKeys = new Hashtable( props.originalKeys );
    }


    /* (non-Javadoc)
     * @see java.util.Dictionary#elements()
     */
    public Enumeration elements()
    {
        return Collections.enumeration( internalMap.values() );
    }


    /* (non-Javadoc)
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


    /* (non-Javadoc)
     * @see java.util.Dictionary#isEmpty()
     */
    public boolean isEmpty()
    {
        return internalMap.isEmpty();
    }


    /* (non-Javadoc)
     * @see java.util.Dictionary#keys()
     */
    public Enumeration keys()
    {
        return Collections.enumeration( originalKeys.values() );
    }


    /* (non-Javadoc)
     * @see java.util.Dictionary#put(java.lang.Object, java.lang.Object)
     */
    public Object put( Object key, Object value )
    {
        if ( key == null || value == null )
        {
            throw new NullPointerException( "key or value" );
        }

        if ( !( key instanceof String ) )
        {
            throw new IllegalArgumentException( "Key [" + key + "] must be a String" );
        }

        checkValue( value );

        String lowerCase = String.valueOf( key ).toLowerCase();
        originalKeys.put( lowerCase, key );
        return internalMap.put( lowerCase, value );
    }


    /* (non-Javadoc)
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


    /* (non-Javadoc)
     * @see java.util.Dictionary#size()
     */
    public int size()
    {
        return internalMap.size();
    }


    //---------- internal -----------------------------------------------------

    static void checkValue( Object value )
    {
        Class type;
        if ( value instanceof Object[] )
        {
            // check simple or primitive
            type = value.getClass().getComponentType();

            // check for primitive type
            if ( type == Integer.TYPE || type == Long.TYPE || type == Float.TYPE || type == Double.TYPE
                || type == Byte.TYPE || type == Short.TYPE || type == Character.TYPE || type == Boolean.TYPE )
            {
                return;
            }

        }
        else if ( value instanceof Vector )
        {
            // check simple
            Vector vector = ( Vector ) value;
            if ( vector.isEmpty() )
            {
                throw new IllegalArgumentException( "Vector must not be empty" );
            }

            // ensure all elements have the same type
            type = null;
            for ( int i = 0; i < vector.size(); i++ )
            {
                Object el = vector.get( i );
                if ( el == null )
                {
                    throw new IllegalArgumentException( "Vector must not contain null elements" );
                }
                if ( type == null )
                {
                    type = el.getClass();
                }
                else if ( type != el.getClass() )
                {
                    throw new IllegalArgumentException( "Vector element types must not be mixed" );
                }
            }

        }
        else if ( value != null )
        {
            // get the type to check (must be simple)
            type = value.getClass();

        }
        else
        {
            // null is illegal
            throw new IllegalArgumentException( "Value must not be null" );
        }

        // check for simple type
        if ( type == String.class || type == Integer.class || type == Long.class || type == Float.class
            || type == Double.class || type == Byte.class || type == Short.class || type == Character.class
            || type == Boolean.class )
        {
            return;
        }

        // not a valid type
        throw new IllegalArgumentException( "Value [" + value + "] has unsupported (base-) type " + type );
    }


    //---------- Object Overwrites --------------------------------------------

    public String toString()
    {
        return internalMap.toString();
    }

}
