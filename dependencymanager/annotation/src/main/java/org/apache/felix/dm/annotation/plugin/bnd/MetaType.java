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

/**
 * Helper class used to generate an XML representation of a MetaType data structure.
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
        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        pw.println("<metatype:MetaData xmlns:metatype=\"http://www.osgi.org/xmlns/metatype/v1.0.0\" localization=\""
            + LOCALIZATION + "\">");
        for (OCD ocd : m_ocdList)
        {
            ocd.writeTo(pw);
        }
        for (Designate designate : m_designateList)
        {
            designate.writeTo(pw);
        }
        pw.println("</metatype:MetaData>");
    }

    private static void writeAttribute(String name, Object value, PrintWriter pw)
    {
        if (value != null)
        {
            pw.print(" " + name + "=" + "\"" + value.toString() + "\"");
        }
    }

    /**
     * An Object Class Definition, which contains a set of Attributes properies.
     */
    public static class OCD
    {
        String m_id;
        String m_name;
        String m_description;
        List<AD> m_attributes = new ArrayList<AD>();

        OCD(String pid, String name, String desc)
        {
            this.m_id = pid;
            this.m_name = name;
            this.m_description = desc;
        }

        public void add(AD ad)
        {
            m_attributes.add(ad);
        }

        public void writeTo(PrintWriter pw)
        {
            pw.print("   <OCD");
            writeAttribute("id", m_id, pw);
            writeAttribute("name", m_name, pw);
            writeAttribute("description", m_description, pw);
            if (m_attributes.size() == 0)
            {
                pw.println("/>");
            }
            else
            {
                pw.println(">");
                for (AD ad : m_attributes)
                {
                    ad.writeTo(pw);
                }
                pw.println("   </OCD>");
            }
        }
    }

    /**
     * An Attribute Definition, which describes a given Properties
     */
    @SuppressWarnings("serial")
    public static class AD
    {
        String m_id;
        String m_type;
        String m_defaults;
        String m_name;
        String m_description;
        Integer m_cardinality;
        Boolean m_required;
        List<Option> m_options = new ArrayList<Option>();

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
            this.m_id = id;
            this.m_type = (type == null) ? "String" : getType(type);
            this.m_name = name;
            this.m_description = desc;
            this.m_cardinality = cardinality;
            this.m_required = required;

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
                this.m_defaults = sb.toString();

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

        public void writeTo(PrintWriter pw)
        {
            pw.print("      <AD");
            writeAttribute("id", m_id, pw);
            writeAttribute("type", m_type, pw);
            writeAttribute("default", m_defaults, pw);
            writeAttribute("name", m_name, pw);
            writeAttribute("description", m_description, pw);
            writeAttribute("cardinality", m_cardinality, pw);
            if (m_options.size() == 0)
            {
                pw.println("/>");
            }
            else
            {
                pw.println(">");
                for (Option option : m_options)
                {
                    option.writeTo(pw);
                }
                pw.println("      </AD>");
            }
        }

        private String getType(String t)
        {
            String result = _allowedTypes.get(t);
            if (result == null)
            {
                throw new IllegalArgumentException("Invalid Property type: " + m_type);
            }
            return result;
        }

        public void add(Option option)
        {
            m_options.add(option);
        }
    }

    /**
     * An Option datastructure, which can be associated with an Attribute.
     */
    public static class Option
    {
        String m_value;
        String m_label;

        Option(String value, String label)
        {
            this.m_value = value;
            this.m_label = label;
        }

        public void writeTo(PrintWriter pw)
        {
            pw.print("         <Option");
            writeAttribute("value", m_value, pw);
            writeAttribute("label", m_label, pw);
            pw.println("/>");
        }
    }

    /**
     * A Designate element, which maps a PID to a given Object Class Definition.
     */
    public static class Designate
    {
        String m_pid;
        OBject m_object;

        public Designate(String pid)
        {
            this.m_pid = pid;
            this.m_object = new OBject(pid);
        }

        public void writeTo(PrintWriter pw)
        {
            pw.print("   <Designate");
            writeAttribute("pid", m_pid, pw);
            pw.println(">");
            m_object.writeTo(pw);
            pw.println("   </Designate>");
        }
    }

    /**
     * A definition of an instance.
     */
    public static class OBject
    {
        String m_ocdref;

        OBject(String ocdref)
        {
            this.m_ocdref = ocdref;
        }

        public void writeTo(PrintWriter pw)
        {
            pw.print("      <Object");
            writeAttribute("ocdref", m_ocdref, pw);
            pw.println("/>");
        }
    }
}
