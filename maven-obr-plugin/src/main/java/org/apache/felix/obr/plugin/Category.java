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


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * describe and store category node.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */

public class Category
{
    /**
     * id of the category.
     */
    private String m_id;


    /**
     * get the id attribute.
     * 
     * @return id
     */
    public String getId()
    {
        return m_id;
    }


    /**
     * set the id attribute.
     * @param id new id value
     */
    public void setId( String id )
    {
        m_id = id;
    }


    /**
     * transform this object to node.
     * @param father father document for create Node
     * @return node
     */
    public Node getNode( Document father )
    {
        Element category = father.createElement( "category" );
        category.setAttribute( "id", getId() );
        return category;
    }
}
