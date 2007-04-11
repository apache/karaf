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
package org.apache.felix.metatype;


import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * The <code>OCD</code> class represents the <code>OCD</code> element of the
 * meta type descriptor.
 *
 * @author fmeschbe
 */
public class OCD
{

    private String id;
    private String name;
    private String description;
    private Map attributes;
    private Map icons;


    public String getID()
    {
        return id;
    }


    public void setId( String id )
    {
        this.id = id;
    }


    public String getName()
    {
        return name;
    }


    public void setName( String name )
    {
        this.name = name;
    }


    public String getDescription()
    {
        return description;
    }


    public void setDescription( String description )
    {
        this.description = description;
    }


    public Map getIcons()
    {
        return icons;
    }


    /**
     * 
     * @param size
     * @param icon The icon, either an URL or a string designating a resource
     *      which may be a localized string
     */
    public void addIcon( Integer size, String icon )
    {
        if ( icon != null )
        {
            if ( icons == null )
            {
                icons = new HashMap();
            }

            icons.put( size, icon );
        }
    }


    public Map getAttributeDefinitions()
    {
        return attributes;
    }


    public void addAttributeDefinition( AD attribute )
    {
        if ( attribute != null )
        {
            if ( attributes == null )
            {
                attributes = new LinkedHashMap();
            }

            attributes.put( attribute.getID(), attribute );
        }
    }
}
