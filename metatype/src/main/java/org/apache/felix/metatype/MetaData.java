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
 * The <code>MetaData</code> class represents the <code>MetaData</code>
 * element of the meta type descriptor.
 * 
 * @author fmeschbe
 */
public class MetaData
{

    private String localePrefix;
    private Map objectClassDefinitions;
    private Map designates;


    public String getLocalePrefix()
    {
        return localePrefix;
    }


    public void setLocalePrefix( String localePrefix )
    {
        this.localePrefix = localePrefix;
    }


    public Map getObjectClassDefinitions()
    {
        return objectClassDefinitions;
    }


    public void addObjectClassDefinition( OCD objectClassDefinition )
    {
        if ( objectClassDefinition != null )
        {
            if ( objectClassDefinitions == null )
            {
                objectClassDefinitions = new LinkedHashMap();
            }

            objectClassDefinitions.put( objectClassDefinition.getID(), objectClassDefinition );
        }
    }


    public Map getDesignates()
    {
        return designates;
    }


    public void addDesignate( Designate designate )
    {
        if ( designate != null )
        {
            if ( designates == null )
            {
                designates = new HashMap();
            }

            designates.put( designate.getPid(), designate );
        }
    }
}
