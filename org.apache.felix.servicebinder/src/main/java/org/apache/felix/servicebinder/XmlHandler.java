/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.servicebinder;

import java.util.Properties;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.servicebinder.parser.ParseException;

/**
 * Simple content handler that builds a list of service descriptors
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class XmlHandler
{
    /**
     * 
     * @uml.property name="parentDescriptor"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    private InstanceMetadata parentDescriptor = null;

    /**
     * 
     * @uml.property name="currentDescriptor"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    private InstanceMetadata currentDescriptor = null;

    private List descriptors = new ArrayList();

    XmlHandler()
    {
    }

    /**
     * Method called when a tag opens
     *
     * @param   uri
     * @param   localName
     * @param   qName
     * @param   attrib
     * @exception   ParseException
    **/
    public void startElement(String uri,String localName,String qName,Properties attrib) throws ParseException
    {
        if (qName.equals("instance") || qName.equals("component"))
        {
            currentDescriptor = new InstanceMetadata(attrib.getProperty("class"),parentDescriptor);
            descriptors.add(currentDescriptor);
        }
        if (qName.equals("service")) // will be deprecated
        {
            if(currentDescriptor == null)
            {
                return;
            }
            currentDescriptor.addInterface(attrib.getProperty("interface"));
        }
        if (qName.equals("provides"))
        {
            if(currentDescriptor == null)
            {
                return;
            }
            currentDescriptor.addInterface(attrib.getProperty("service"));
        }
        if (qName.equals("property"))
        {

            if(currentDescriptor == null)
            {
                return;
            }
            PropertyMetadata prop = new PropertyMetadata(attrib.getProperty("name"),
                attrib.getProperty("type"),
                attrib.getProperty("value"));
            currentDescriptor.addProperty(prop);
        }
        if (qName.equals("requires"))
        {
            if(currentDescriptor == null)
            {
                return;
            }

            DependencyMetadata dd=new DependencyMetadata(attrib.getProperty("service"),
                attrib.getProperty("cardinality"),attrib.getProperty("policy"),attrib.getProperty("filter"),
                attrib.getProperty("bind-method"),attrib.getProperty("unbind-method"));

            currentDescriptor.addDependency(dd);
        }
        
        if (qName.equals("instantiates"))
        {
            GenericActivator.error("ERROR: Version 1.1 does not support factories");
        }

    }

    /**
    * Method called when a tag closes
    *
    * @param   uri
    * @param   localName
    * @param   qName
    * @exception   ParseException
    */
    public void endElement(java.lang.String uri,java.lang.String localName,java.lang.String qName) throws ParseException
    {
        if (qName.equals("instantiates") || qName.equals("component"))
        {
            currentDescriptor = parentDescriptor;
            parentDescriptor = null;
        }
    }

    /**
    * Called to retrieve the service descriptors
    *
    * @return   A list of service descriptors
    */
    List getInstanceMetadatas()
    {
        return descriptors;
    }
}
