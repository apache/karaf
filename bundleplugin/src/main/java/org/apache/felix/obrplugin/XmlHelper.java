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


import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Provide XML helper methods to support pre-Java5 runtimes
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class XmlHelper
{
    /**
     * based on public Java5 javadoc of org.w3c.dom.Node.getTextContent method
     */
    public static String getTextContent( Node node )
    {
        switch ( node.getNodeType() )
        {
            case Node.ELEMENT_NODE:
            case Node.ATTRIBUTE_NODE:
            case Node.ENTITY_NODE:
            case Node.ENTITY_REFERENCE_NODE:
            case Node.DOCUMENT_FRAGMENT_NODE:
                return mergeTextContent( node.getChildNodes() );
            case Node.TEXT_NODE:
            case Node.CDATA_SECTION_NODE:
            case Node.COMMENT_NODE:
            case Node.PROCESSING_INSTRUCTION_NODE:
                return node.getNodeValue();
            case Node.DOCUMENT_NODE:
            case Node.DOCUMENT_TYPE_NODE:
            case Node.NOTATION_NODE:
            default:
                return null;
        }
    }


    /**
     * based on the following quote from public Java5 javadoc of org.w3c.dom.Node.getTextContent method:
     * 
     * "concatenation of the textContent attribute value of every child node, excluding COMMENT_NODE and
     * PROCESSING_INSTRUCTION_NODE nodes. This is the empty string if the node has no children"
     */
    private static String mergeTextContent( NodeList nodes )
    {
        StringBuffer buf = new StringBuffer();
        for ( int i = 0; i < nodes.getLength(); i++ )
        {
            Node n = nodes.item( i );
            final String text;

            switch ( n.getNodeType() )
            {
                case Node.COMMENT_NODE:
                case Node.PROCESSING_INSTRUCTION_NODE:
                    text = null;
                    break;
                default:
                    text = getTextContent( n );
                    break;
            }

            if ( text != null )
            {
                buf.append( text );
            }
        }
        return buf.toString();
    }


    /**
     * based on public Java5 javadoc of org.w3c.dom.Node.setTextContent method
     */
    public static void setTextContent( Node node, final String text )
    {
        while ( node.hasChildNodes() )
        {
            node.removeChild( node.getFirstChild() );
        }

        if ( text != null && text.length() > 0 )
        {
            Node textNode = node.getOwnerDocument().createTextNode( text );
            node.appendChild( textNode );
        }
    }
}
