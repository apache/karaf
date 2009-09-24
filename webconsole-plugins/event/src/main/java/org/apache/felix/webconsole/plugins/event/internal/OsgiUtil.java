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
package org.apache.felix.webconsole.plugins.event.internal;

import java.util.*;

/**
 * The <code>OsgiUtil</code> is a utility class providing some usefull utility
 * methods.
 */
public class OsgiUtil {

    public static boolean toBoolean(Dictionary props, String key, boolean defaultValue)
    {
        Object propValue = toObject( props, key );
        if (propValue instanceof Boolean)
        {
            return ((Boolean) propValue).booleanValue();
        }
        else if (propValue != null)
        {
            return Boolean.valueOf(String.valueOf(propValue)).booleanValue();
        }

        return defaultValue;
    }

    public static String toString(Dictionary props, String key, String defaultValue)
    {
        Object propValue = toObject( props, key );
        return (propValue != null) ? propValue.toString() : defaultValue;
    }

    public static int toInteger(Dictionary props, String key, int defaultValue)
    {
        Object propValue = toObject( props, key );
        if (propValue instanceof Integer)
        {
            return ((Integer) propValue).intValue();
        }
        else if (propValue != null)
        {
            try
            {
                return Integer.valueOf(String.valueOf(propValue)).intValue();
            }
            catch (NumberFormatException nfe)
            {
                // don't care, fall through to default value
            }
        }

        return defaultValue;
    }

    public static Object toObject(Dictionary props, String key)
    {
        if (props == null || key == null )
        {
            return null;
        }
        final Object propValue = props.get(key);
        if ( propValue == null )
        {
            return null;
        }
        else if (propValue.getClass().isArray())
        {
            Object[] prop = (Object[]) propValue;
            return prop.length > 0 ? prop[0] : null;
        }
        else if (propValue instanceof Collection)
        {
            Collection prop = (Collection) propValue;
            return prop.isEmpty() ? null : prop.iterator().next();
        }
        else
        {
            return propValue;
        }
    }

    public static String[] toStringArray(Dictionary props, String key, String[] defaultArray)
    {
        if (props == null || key == null )
        {
            return defaultArray;
        }
        final Object propValue = props.get(key);
        if (propValue == null)
        {
            // no value at all
            return defaultArray;

        }
        else if (propValue instanceof String)
        {
            // single string
            return new String[] { (String) propValue };

        }
        else if (propValue instanceof String[])
        {
            // String[]
            return (String[]) propValue;

        }
        else if (propValue.getClass().isArray())
        {
            // other array
            Object[] valueArray = (Object[]) propValue;
            List values = new ArrayList(valueArray.length);
            for(int i=0; i<valueArray.length; i++)
            {
                final Object value = valueArray[i];
                if (value != null)
                {
                    values.add(value.toString());
                }
            }
            return (String[]) values.toArray(new String[values.size()]);

        }
        else if (propValue instanceof Collection)
        {
            // collection
            Collection valueCollection = (Collection) propValue;
            List valueList = new ArrayList(valueCollection.size());
            final Iterator i = valueCollection.iterator();
            while ( i.hasNext() )
            {
                final Object value = i.next();
                if (value != null)
                {
                    valueList.add(value.toString());
                }
            }
            return (String[]) valueList.toArray(new String[valueList.size()]);
        }

        return defaultArray;
    }
}
