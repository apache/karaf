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
package org.apache.felix.scrplugin.tags.annotation.sling;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.felix.scrplugin.Constants;
import org.apache.felix.scrplugin.helper.StringUtils;
import org.apache.felix.scrplugin.tags.JavaClassDescription;
import org.apache.felix.scrplugin.tags.annotation.defaulttag.AbstractTag;

import com.thoughtworks.qdox.model.Annotation;

/**
 * Description of a java tag for components.
 */
public class SlingServletPropertyTag extends AbstractTag {

    protected final String name;
    protected final String[] values;

    /**
     * @param name Property name
     * @param values Property values
     * @param desc Description
     */
    public SlingServletPropertyTag(Annotation annotation, String name, String[] values, JavaClassDescription desc) {
        super(annotation, desc, null);
        this.name = name;
        this.values = values;
    }

    @Override
    public String getName() {
        return Constants.PROPERTY;
    }

    @Override
    public Map<String, String> createNamedParameterMap() {
        final SortedMap<String, String> map = new TreeMap<String, String>();

        map.put(Constants.PROPERTY_NAME, this.name);

        if (this.values == null || this.values.length == 0) {
            map.put(Constants.PROPERTY_VALUE, "");
        } else if (this.values.length == 1) {
            map.put(Constants.PROPERTY_VALUE, this.values[0]);
        } else {
            for (int i = 0; i < this.values.length; i++) {
                // generate index number with trailing zeros to ensure correct sort order in map
                String index = StringUtils.leftPad(Integer.toString(i), 10, "0");
                map.put(Constants.PROPERTY_MULTIVALUE_PREFIX + '.' + index, this.values[i]);
            }
        }

        return map;
    }

}
