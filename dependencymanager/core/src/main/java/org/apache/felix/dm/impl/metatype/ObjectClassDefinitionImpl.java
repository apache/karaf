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
package org.apache.felix.dm.impl.metatype;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * ObjectClassDefinition implementation.
 */
public class ObjectClassDefinitionImpl implements ObjectClassDefinition
{
    // Our OCD name (may be localized)
    private String m_name;
    
    // Our OCD description (may be localized)
    private String m_description;
    
    // Our OCD id
    private String m_id;
    
    // The list of Properties MetaData objects (from DependencyManager API)
    private List m_propertiesMetaData;
    
    // The localized resource that can be used when localizing some parameters
    private Resource m_resource;

    public ObjectClassDefinitionImpl(String id, String name, String description, List propertiesMetaData, Resource resource)
    {
        m_id = id;
        m_name = name;
        m_description = description;
        m_propertiesMetaData = propertiesMetaData;
        m_resource = resource;
    }

    // --------------------- ObjectClassDefinition ----------------------------------------

    public AttributeDefinition[] getAttributeDefinitions(int filter)
    {
        List attrs = new ArrayList();
        for (int i = 0; i < m_propertiesMetaData.size(); i++)
        {
            PropertyMetaDataImpl metaData = (PropertyMetaDataImpl) m_propertiesMetaData.get(i);
            switch (filter)
            {
                case ObjectClassDefinition.ALL:
                    attrs.add(new AttributeDefinitionImpl(metaData, m_resource));
                    break;
                case ObjectClassDefinition.OPTIONAL:
                    if (!metaData.isRequired())
                    {
                        attrs.add(new AttributeDefinitionImpl(metaData, m_resource));
                    }
                    break;
                case ObjectClassDefinition.REQUIRED:
                    if (metaData.isRequired())
                    {
                        attrs.add(new AttributeDefinitionImpl(metaData, m_resource));
                    }
                    break;
            }
        }

        AttributeDefinition[] array = new AttributeDefinitionImpl[attrs.size()];
        return (AttributeDefinition[]) attrs.toArray(array);
    }

    public String getDescription()
    {
        return m_resource.localize(m_description);
    }

    public String getID()
    {
        return m_id;
    }

    public InputStream getIcon(int size) throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getName()
    {
        return m_resource.localize(m_name);
    }
}
