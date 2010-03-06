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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.dm.dependencies.PropertyMetaData;
import org.osgi.service.metatype.AttributeDefinition;

/**
 * DependencyManager PropertyMetaData Implementation. This class describes meta informations regarding
 * one given configuration property.
 */
public class PropertyMetaDataImpl implements PropertyMetaData
{
    /**
     * List of option labels (may be localized)
     */
    List m_optionsLabels = new ArrayList();
    
    /**
     * List of option values
     */
    List m_optionsValues = new ArrayList();
    
    /**
     * Property cardinality.
     * @see {@link AttributeDefinition#getCardinality()}
     */
    private int m_cardinality;
    
    /**
     * Valid default property values
     */
    private String[] m_defaults;
    
    /**
     * Property description.
     */
    private String m_description;
    
    /**
     * Property title.
     */
    private String m_heading;
    
    /**
     * Property unique Id
     */
    private String m_id;
    
    /**
     * Required flag.
     */
    private boolean m_required;
    
    /**
     * Property Type.
     * @see {@link AttributeDefinition#getType()}
     */
    private int m_type = AttributeDefinition.STRING;
    
    /**
     * Mapping between java types and valid MetaType types.
     * @see {@link AttributeDefinition#getType()}
     */
    private final static Map m_typeMapping = new HashMap()
    {
        {
            put(Boolean.class, new Integer(AttributeDefinition.BOOLEAN));
            put(Byte.class, new Integer(AttributeDefinition.BYTE));
            put(Character.class, new Integer(AttributeDefinition.CHARACTER));
            put(Double.class, new Integer(AttributeDefinition.FLOAT));
            put(Integer.class, new Integer(AttributeDefinition.INTEGER));
            put(Long.class, new Integer(AttributeDefinition.LONG));
            put(Short.class, new Integer(AttributeDefinition.SHORT));
            put(String.class, new Integer(AttributeDefinition.STRING));
        }
    };

    public PropertyMetaData addOption(String optionLabel, String optionValue)
    {
        m_optionsLabels.add(optionLabel);
        m_optionsValues.add(optionValue);
        return this;
    }

    public PropertyMetaData setCardinality(int cardinality)
    {
        m_cardinality = cardinality;
        return this;
    }

    public PropertyMetaData setDefaults(String[] defaults)
    {
        m_defaults = defaults;
        return this;
    }

    public PropertyMetaData setDescription(String description)
    {
        m_description = description;
        return this;
    }

    public PropertyMetaData setHeading(String heading)
    {
        m_heading = heading;
        return this;
    }

    public PropertyMetaData setId(String id)
    {
        m_id = id;
        return this;
    }

    public PropertyMetaData setRequired(boolean required)
    {
        m_required = required;
        return this;
    }

    public PropertyMetaData setType(Class classType)
    {
        Integer type = (Integer) m_typeMapping.get(classType);
        if (type == null)
        {
            throw new IllegalArgumentException("Invalid type: " + classType + ". Valid types are "
                + m_typeMapping.keySet());
        }
        m_type = type.intValue();
        return this;
    }

    public String[] getOptionLabels()
    {
        String[] optionLabels = new String[m_optionsLabels.size()];
        return (String[]) m_optionsLabels.toArray(optionLabels);
    }

    public String[] getOptionValues()
    {
        String[] optionValues = new String[m_optionsValues.size()];
        return (String[]) m_optionsValues.toArray(optionValues);
    }

    public int getCardinality()
    {
        return m_cardinality;
    }

    public String[] getDefaults()
    {
        return m_defaults;
    }

    public String getDescription()
    {
        return m_description;
    }

    public String getHeading()
    {
        return m_heading;
    }

    public String getId()
    {
        return m_id;
    }

    public boolean isRequired()
    {
        return m_required;
    }

    public int getType()
    {
        return m_type;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("cardinality=").append(m_cardinality);
        sb.append("; defaults="); 
        for (int i = 0; i < m_defaults.length; i ++) {
            sb.append(m_defaults[i]).append(" ");
        }
        sb.append("; description=").append(m_description);
        sb.append("; heading=").append(m_heading);
        sb.append("; id=").append(m_id);
        sb.append("; required=").append(m_required);
        sb.append("; type=").append(getType());
        sb.append("; optionLabels=");
        for (int i = 0; i < m_optionsLabels.size(); i ++) {
            sb.append(m_optionsLabels.get(i)).append(" ");
        }
        sb.append("; optionValues=");
        for (int i = 0; i < m_optionsValues.size(); i ++) {
            sb.append(m_optionsValues.get(i)).append(" ");
        }
        return sb.toString();
    }
}
