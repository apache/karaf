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
package org.apache.felix.obrplugin;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * this class describe the p element in a capability tag.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 * 
 */
public class PElement
{
    /**
     * store the v tag (value).
     */
    private String m_v;

    /**
     * store the t tag (type).
     */
    private String m_t;

    /**
     * store the n tag (name).
     */
    private String m_n;


    /**
     * get the n tag.
     * @return attribute n
     */
    public String getN()
    {
        return m_n;
    }


    /**
     * set the n tage.
     * @param n new value
     */
    public void setN( String n )
    {
        m_n = n;
    }


    /**
     * get the t tag.
     * @return attribute t
     */
    public String getT()
    {
        return m_t;
    }


    /**
     * set the t tag.
     * @param t new value
     */
    public void setT( String t )
    {
        m_t = t;
    }


    /**
     * get the v tag.
     * @return attribute v
     */
    public String getV()
    {
        return m_v;
    }


    /**
     * set the v tag.
     * @param v new value
     */
    public void setV( String v )
    {
        m_v = v;
    }


    /**
     * transform this object to node.
     * @param father father document for create Node
     * @return node
     */
    public Node getNode( Document father )
    {
        Element p = father.createElement( "p" );
        p.setAttribute( "n", getN() );
        if ( getT() != null )
        {
            p.setAttribute( "t", getT() );
        }

        if ( getV() != null )
        {
            p.setAttribute( "v", getV() );
        }

        return p;
    }
}
