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

import org.osgi.service.metatype.AttributeDefinition;

public class AttributeDefinitionImpl implements AttributeDefinition
{
    private PropertyMetaDataImpl m_propertyMetaData;
    private Resource m_resource;

    public AttributeDefinitionImpl(PropertyMetaDataImpl propertyMetaData, Resource resource)
    {
        m_propertyMetaData = propertyMetaData;
        m_resource = resource;
    }

    public int getCardinality()
    {
        return m_propertyMetaData.getCardinality();
    }

    public String[] getDefaultValue()
    {
        return m_propertyMetaData.getDefaults();
    }

    public String getDescription()
    {
        return m_resource.localize(m_propertyMetaData.getDescription());
    }

    public String getID()
    {
        return m_propertyMetaData.getId();
    }

    public String getName()
    {
        return m_resource.localize(m_propertyMetaData.getHeading());
    }

    public String[] getOptionLabels()
    {
        String[] labels = m_propertyMetaData.getOptionLabels();
        if (labels != null)
        {
            for (int i = 0; i < labels.length; i++)
            {
                labels[i] = m_resource.localize(labels[i]);
            }
        }
        return labels;
    }

    public String[] getOptionValues()
    {
        return m_propertyMetaData.getOptionValues();
    }

    public int getType()
    {
        return m_propertyMetaData.getType();
    }

    public String validate(String value)
    {
        return null;
    }
}
