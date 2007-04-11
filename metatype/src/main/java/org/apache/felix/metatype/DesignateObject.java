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


import java.util.ArrayList;
import java.util.List;


/**
 * The <code>DesignateObject</code> class represents the <code>Object</code> element of
 * the meta type descriptor.
 * 
 * @author fmeschbe
 */
public class DesignateObject
{

    private String ocdRef;
    private List attributes;


    public String getOcdRef()
    {
        return ocdRef;
    }


    public void setOcdRef( String ocdRef )
    {
        this.ocdRef = ocdRef;
    }


    public List getAttributes()
    {
        return attributes;
    }


    public void addAttribute( Attribute attribute )
    {
        if ( attribute != null )
        {
            if ( attributes == null )
            {
                attributes = new ArrayList();
            }
            attributes.add( attribute );
        }
    }
}
