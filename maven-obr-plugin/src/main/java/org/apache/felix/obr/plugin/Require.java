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
package org.apache.felix.obr.plugin;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * this class store a Require tag.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Require {

    /**
     * store the extend attribute.
     */
    private String m_extend;

    /**
     * store the multiple attribute.
     */
    private String m_multiple;

    /**
     * store the optional attribute.
     */
    private String m_optional;

    /**
     * store the name attribute.
     */
    private String m_name;

    /**
     * store the filter attribute.
     */
    private String m_filter;

    /**
     * store the value of the tag.
     */
    private String m_value;

    /**
     * get the extend attribute.
     * @return a string which contains the value of the boolean
     */
    public String getExtend() {
        return m_extend;
    }

    /**
     * set the extend attribute.
     * @param extend new value for the extend attribute
     */
    public void setExtend(String extend) {
        this.m_extend = extend;
    }

    /**
     * get the filter attribute.
     * @return m_filter value
     */
    public String getFilter() {
        return m_filter;
    }

    /**
     * set the filter attribute.
     * @param filter new value for filter
     */
    public void setFilter(String filter) {
        this.m_filter = filter;
    }

    /**
     * get multiple attribute.
     * @return m_multiple value
     */
    public String getMultiple() {
        return m_multiple;
    }

    /**
     * set multiple attribute.
     * @param multiple new value for m_multiple
     */
    public void setMultiple(String multiple) {
        this.m_multiple = multiple;
    }

    /**
     * get name attribute.
     * @return m_name value
     */
    public String getName() {
        return m_name;
    }

    /**
     * set name attribute.
     * @param name new value for m_name
     */
    public void setName(String name) {
        this.m_name = name;
    }

    /**
     * get the optional attribute.
     * @return m_optional value
     */
    public String getOptional() {
        return m_optional;
    }

    /**
     * set the optional attribute.
     * @param optionnal new value for m_optional
     */
    public void setOptional(String optionnal) {
        this.m_optional = optionnal;
    }

    /**
     * get value of the tag.
     * @return value of this tag
     */
    public String getValue() {
        return m_value;
    }

    /**
     * set the value of the tag.
     * @param value new value for this tag
     */
    public void setValue(String value) {
        this.m_value = value;
    }

    /**
     * transform this object to Node.
     * 
     * @param father father document for create Node
     * @return node
     */
    public Node getNode(Document father) {
        Element require = father.createElement("require");
        require.setAttribute("name", this.getName());
        require.setAttribute("filter", this.getFilter());
        require.setAttribute("extend", this.getExtend());
        require.setAttribute("multiple", this.getMultiple());
        require.setAttribute("optional", this.getOptional());
        XmlHelper.setTextContent(require,this.getValue());

        return require;
    }

}
