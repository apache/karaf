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
package org.apache.felix.scrplugin.om.metatype;

import java.util.Map;

public class AttributeDefinition {

    public static final String DEFAULT_TYPE = "String";

    protected String id;

    protected String type = DEFAULT_TYPE;

    protected String defaultValue;

    protected String name;

    protected String[] defaultMultiValue;

    protected String description;

    protected Integer cardinality;

    protected Map options;

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        // do not overwrite default or currently set type
        if (type != null) {
            this.type = type;
        }
    }

    public Object getDefaultValue() {
        return this.defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        if (defaultValue != null) {
            this.defaultValue = defaultValue;
            this.defaultMultiValue = null;
        }
    }

    public void setDefaultMultiValue(String[] values) {
        if (values != null) {
            this.defaultValue = null;
            this.defaultMultiValue = values;
            if (values.length > 0 && this.cardinality == null ) {
                this.cardinality = new Integer(Integer.MAX_VALUE);
            }
        }
    }

    public String[] getDefaultMultiValue() {
        return this.defaultMultiValue;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCardinality() {
        return this.cardinality;
    }

    public void setCardinality(Integer cardinality) {
        this.cardinality = cardinality;
    }

    public Map getOptions() {
        return this.options;
    }

    public void setOptions(Map options) {
        this.options = options;
    }
}
