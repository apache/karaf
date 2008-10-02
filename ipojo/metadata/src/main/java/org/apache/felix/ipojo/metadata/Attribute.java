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
package org.apache.felix.ipojo.metadata;

/**
 * An attribute is a key-value pair. It represents the attribute
 * of XML elements.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Attribute {

    /**
     * The name of the attribute.
     */
    private String m_name;

    /**
     * The value of the attribute.
     */
    private String m_value;

    /**
     * The namespace of the attribute.
     */
    private String m_nameSpace;

    /**
     * Creates an attribute.
     * @param name the name of the attribute.
     * @param value the value of the attribute.
     */
    public Attribute(String name, String value) {
        m_name = name.toLowerCase();
        m_value = value;
    }

    /**
     * Creates an attribute.
     * @param name the name of the attribute.
     * @param value the value of the attribute.
     * @param ns the namespace of the attribute.
     */
    public Attribute(String name, String ns, String value) {
        m_name = name.toLowerCase();
        m_value = value;
        if (ns != null && ns.length() > 0) {
            m_nameSpace = ns;
        }
    }

    /**
     * Gets the attribute name.
     * @return the name
     */
    public String getName() {
        return m_name;
    }

    /**
     * Gets attribute value.
     * @return the value
     */
    public String getValue() {
        return m_value;
    }

    /**
     * Gets attribute namespace.
     * @return the namespace
     */
    public String getNameSpace() {
        return m_nameSpace;
    }

}
