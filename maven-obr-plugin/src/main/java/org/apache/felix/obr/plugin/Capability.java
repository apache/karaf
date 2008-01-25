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
package org.apache.felix.obr.plugin;


import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * This class describe and store capability node.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Capability
{

    /**
     * m_name: name of the capability.
     */
    private String m_name;

    /**
     * m_p: List of PElement.
     */
    private List m_p = new ArrayList();


    /**
     * get the name attribute.
     * 
     * @return name attribute
     */
    public String getName()
    {
        return m_name;
    }


    /**
     * set the name attribute.
     * 
     * @param name new name value
     *            
     */
    public void setName( String name )
    {
        m_name = name;
    }


    /**
     * return the capabilities.
     * 
     * @return List of PElement
     */
    public List getP()
    {
        return m_p;
    }


    /**
     * set the capabilities.
     * 
     * @param mp List of PElement
     *            
     */
    public void setP( List mp )
    {
        m_p = mp;
    }


    /**
     * add one element in List.
     * 
     * @param pelement PElement
     *            
     */
    public void addP( PElement pelement )
    {
        m_p.add( pelement );
    }


    /**
     * transform this object to Node.
     * 
     * @param father father document for create Node
     * @return node
     */
    public Node getNode( Document father )
    {
        Element capability = father.createElement( "capability" );
        capability.setAttribute( "name", getName() );
        for ( int i = 0; i < getP().size(); i++ )
        {
            capability.appendChild( ( ( PElement ) ( getP().get( i ) ) ).getNode( father ) );
        }
        return capability;
    }

}
