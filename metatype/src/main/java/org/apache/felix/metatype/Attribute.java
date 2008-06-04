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


/**
 * The <code>Attribute</code> TODO
 *
 * @author fmeschbe
 * @version $Rev$, $Date$
 */
public class Attribute
{

    private String adRef;
    private String[] content;


    public String getAdRef()
    {
        return adRef;
    }


    public void setAdRef( String adRef )
    {
        this.adRef = adRef;
    }


    public String[] getContent()
    {
        return ( String[] ) content.clone();
    }


    public void addContent( String[] added )
    {
        if ( added != null && added.length > 0 )
        {
            if ( content == null )
            {
                content = ( String[] ) added.clone();
            }
            else
            {
                String[] newContent = new String[content.length + added.length];
                System.arraycopy( content, 0, newContent, 0, content.length );
                System.arraycopy( added, 0, newContent, content.length, added.length );
                content = newContent;
            }
        }
    }


    public void addContent( String content, boolean split )
    {
        if ( content != null )
        {
            if ( split )
            {
            	addContent( AD.splitList( content ) );
            }
            else
            {
            	addContent( new String[] { content } );
            }
        }
    }
    
}
