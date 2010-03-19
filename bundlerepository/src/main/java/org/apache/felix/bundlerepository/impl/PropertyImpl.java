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
package org.apache.felix.bundlerepository.impl;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.felix.bundlerepository.Property;
import org.apache.felix.utils.version.VersionTable;

public class PropertyImpl implements Property
{
    private final String name;
    private final String type;
    private final String value;

    public PropertyImpl(String name, String type, String value)
    {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public String getName()
    {
        return name;
    }

    public String getType()
    {
        return type;
    }

    public String getValue()
    {
        return value;
    }

    public Object getConvertedValue()
    {
        return convert(value, type);
    }

    private static Object convert(String value, String type)
    {
        try
        {
            if (value != null && type != null)
            {
                if (VERSION.equalsIgnoreCase(type))
                {
                    return VersionTable.getVersion(value);
                }
                else if (URI.equalsIgnoreCase(type))
                {
                    return new URI(value);
                }
                else if (URL.equalsIgnoreCase(type))
                {
                    return new URL(value);
                }
                else if (LONG.equalsIgnoreCase(type))
                {
                    return new Long(value);
                }
                else if (DOUBLE.equalsIgnoreCase(type))
                {
                    return new Double(value);
                }
                else if (SET.equalsIgnoreCase(type))
                {
                    StringTokenizer st = new StringTokenizer(value, ",");
                    Set s = new HashSet();
                    while (st.hasMoreTokens())
                    {
                        s.add(st.nextToken().trim());
                    }
                    return s;
                }
            }
            return value;
        }
        catch (Exception e)
        {
            IllegalArgumentException ex = new IllegalArgumentException();
            ex.initCause(e);
            throw ex;
        }
    }
}