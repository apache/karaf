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
package org.apache.felix.scrplugin.tags.annotation.defaulttag;

import java.util.Map;

import org.apache.felix.scrplugin.tags.*;

/**
 * Description of a java tag for components.
 */
public abstract class AbstractTag implements JavaTag {

    protected final JavaClassDescription description;

    protected final JavaField field;

    protected Map<String, String> parameters;

    /**
     * @param desc Description
     * @param field Field
     */
    public AbstractTag(JavaClassDescription desc, JavaField field) {
        this.description = desc;
        this.field = field;
    }

    /**
     * @see JavaTag#getNamedParameter(String)
     */
    public String getNamedParameter(String name) {
        final Map<String, String> map = this.getNamedParameterMap();
        if (map != null) {
            return map.get(name);
        }
        return null;
    }

    /**
     * @see JavaTag#getParameters()
     */
    public String[] getParameters() {
        final Map<?, ?> map = this.getNamedParameterMap();
        if (map != null) {
            return map.keySet().toArray(new String[5]);
        }
        return new String[0];
    }

    /**
     * @see JavaTag#getSourceLocation()
     */
    public String getSourceLocation() {
        return "Java annotations in " + this.description.getName();
    }

    /**
     * @see JavaTag#getJavaClassDescription()
     */
    public JavaClassDescription getJavaClassDescription() {
        return this.description;
    }

    /**
     * @see JavaTag#getField()
     */
    public JavaField getField() {
        return this.field;
    }

    /**
     * Maps an empty or null string value to null
     * @param value String value
     * @return Non-empty string value or null
     */
    protected String emptyToNull(String value) {
        if (value == null || value.length() == 0) {
            return null;
        }
        return value;
    }

    /**
     * @see JavaTag#getName()
     */
    public abstract String getName();

    /**
     * @see org.apache.felix.scrplugin.tags.JavaTag#getNamedParameterMap()
     */
    public Map<String, String> getNamedParameterMap() {
        if ( this.parameters == null ) {
            this.parameters = this.createNamedParameterMap();
        }
        return this.parameters;
    }

    /**
     * Create the parameter map.
     * @see JavaTag#getNamedParameterMap()
     */
    protected abstract Map<String, String> createNamedParameterMap();

}
