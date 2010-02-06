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
package org.apache.felix.dm.annotation.plugin.bnd;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * Helper class used to represent a data structure which is complying to the MetaType XML.
 * We use XStream in order to generating the XML from this class.
 */
public class MetaType
{
    /**
     * The list of Object Class Definitions used to group the attributes of a given
     * set of properties.
     */
    private List<OCD> m_ocdList = new ArrayList<OCD>();

    /**
     * The list of Designate elements.
     */
    private List<Designate> m_designateList = new ArrayList<Designate>();

    /**
     * The default localization directory.
     */
    private final static String LOCALIZATION = "OSGI-INF/metatype/metatype";

    /**
     * Adds an Object Class Definition into this meta type.
     * @param ocd the Object Class Definition.
     */
    public void add(OCD ocd)
    {
        m_ocdList.add(ocd);
    }

    /**
     * Adds a Designate element, which maps a PID to an OCD.
     * @param designate the Designate element.
     */
    public void add(Designate designate)
    {
        m_designateList.add(designate);
    }

    /**
     * Returns the number of OCD contained in this meta type.
     * @return the number of OCD contained in this meta type.
     */
    public int getSize()
    {
        return m_ocdList.size();
    }

    /**
     * Generates an XML representation of this metatype.
     * @param pw a PrintWriter where the XML is written
     */
    public void writeTo(PrintWriter pw)
    {
        XStream xStream = new XStream();
        xStream.processAnnotations(new Class[] { OCD.class, AD.class, Option.class,
                Designate.class, OBject.class });
        StringBuilder xml = new StringBuilder("");
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<metatype:MetaData xmlns:metatype=\"http://www.osgi.org/xmlns/metatype/v1.0.0\" localization=\""
            + LOCALIZATION + "\">\n");
        for (OCD ocd : m_ocdList)
        {
            xml.append(xStream.toXML(ocd) + "\n");
        }
        for (Designate designate : m_designateList)
        {
            xml.append(xStream.toXML(designate) + "\n");
        }
        xml.append("</metatype:MetaData>");
        pw.println(xml);
    }

    /**
     * An Object Class Definition, which contains a set of Attributes properies.
     */
    @XStreamAlias("OCD")
    public static class OCD
    {
        @XStreamAsAttribute
        String id;

        @XStreamAsAttribute
        String name;

        @XStreamAsAttribute
        String description;

        @XStreamImplicit(itemFieldName = "AD")
        List<AD> attributes = new ArrayList<AD>();

        OCD(String pid, String name, String desc)
        {
            this.id = pid;
            this.name = name;
            this.description = desc;
        }

        public void add(AD ad)
        {
            attributes.add(ad);
        }
    }

    /**
     * An Attribute Definition, which describes a given Properties
     */
    @SuppressWarnings("serial")
    @XStreamAlias("AD")
    public static class AD
    {
        @XStreamAsAttribute
        String id;

        @XStreamAsAttribute
        String type;

        @XStreamAsAttribute
        @XStreamAlias("default")
        String defaults;

        @XStreamAsAttribute
        String name;

        @XStreamAsAttribute
        String description;

        @XStreamImplicit(itemFieldName = "")
        List<Option> options = new ArrayList<Option>();

        @XStreamAsAttribute
        Integer cardinality;

        @XStreamAsAttribute
        Boolean required;

        private final static Map<String, String> _allowedTypes = new HashMap<String, String>()
        {
            {
                put(String.class.getName(), "String");
                put(Long.class.getName(), "Long");
                put(Integer.class.getName(), "Integer");
                put(Character.class.getName(), "Char");
                put(Byte.class.getName(), "Byte");
                put(Double.class.getName(), "Double");
                put(Float.class.getName(), "Float");
                put(Boolean.class.getName(), "Boolean");
            }
        };

        public AD(String id, String type, Object[] defaults, String name, String desc, Integer cardinality, Boolean required)
        {
            this.id = id;
            this.type = (type == null) ? "String" : getType(type);
            this.name = name;
            this.description = desc;
            this.cardinality = cardinality;
            this.required = required;

            if (defaults != null)
            {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < defaults.length; i++)
                {
                    sb.append(defaults[i].toString());
                    if (i < defaults.length - 1)
                    {
                        sb.append(",");
                    }
                }
                this.defaults = sb.toString();

                // Check if the number of default values is consistent with the cardinality.
                if (cardinality != null)
                {
                    int max = (cardinality.intValue() == 0) ? 1 : Math.abs(cardinality.intValue());
                    if (defaults.length > max)
                    {
                        throw new IllegalArgumentException("number of default values ("
                            + defaults.length + ") is inconsistent with cardinality ("
                            + cardinality + ")");
                    }
                }
            }
        }

        private String getType(String t)
        {
            String result = _allowedTypes.get(t);
            if (result == null)
            {
                throw new IllegalArgumentException("Invalid Property type: " + type);
            }
            return result;
        }

        public void add(Option option)
        {
            options.add(option);
        }
    }

    /**
     * An Option datastructure, which can be associated with an Attribute.
     */
    @XStreamAlias("Option")
    public static class Option
    {
        @XStreamAsAttribute
        String value;

        @XStreamAsAttribute
        String label;

        Option(String value, String label)
        {
            this.value = value;
            this.label = label;
        }
    }

    /**
     * A Designate element, which maps a PID to a given Object Class Definition.
     */
    @XStreamAlias("Designate")
    public static class Designate
    {
        @XStreamAsAttribute
        String pid;

        @XStreamAlias(value = "Object")
        OBject object;

        public Designate(String pid)
        {
            this.pid = pid;
            this.object = new OBject(pid);
        }
    }

    /**
     * A definition of an instance.
     */
    @XStreamAlias("Object")
    public static class OBject
    {
        @XStreamAsAttribute
        String ocdref;

        OBject(String ocdref)
        {
            this.ocdref = ocdref;
        }
    }
}
