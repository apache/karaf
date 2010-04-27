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

import java.util.*;

import org.apache.felix.scrplugin.Constants;
import org.apache.felix.scrplugin.helper.StringUtils;
import org.apache.felix.scrplugin.tags.JavaClassDescription;
import org.apache.felix.scrplugin.tags.annotation.defaulttag.AbstractTag;

import com.thoughtworks.qdox.model.Annotation;

/**
 * A property tag.
 */
public class SlingServletPropertyTag extends AbstractTag {

    protected final String name;
    protected final String[] values;
    protected final String type;
    protected final Boolean isPrivate;

    /**
     * @param name Property name
     * @param values Property values
     * @param desc Description
     */
    public SlingServletPropertyTag(Annotation annotation, String name, String[] values, JavaClassDescription desc) {
        super(annotation, desc, null);
        this.name = name;
        this.values = values;
        this.type = null;
        this.isPrivate = null;
    }

    /**
     * @param name Property name
     * @param value Property value
     * @param desc Description
     */
    public SlingServletPropertyTag(final Annotation annotation,
            final String name,
            final String value,
            final JavaClassDescription desc,
            final String type,
            final boolean isPrivate) {
        super(annotation, desc, null);
        this.name = name;
        this.values = new String[] {value};
        this.type = type;
        this.isPrivate = isPrivate;
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

        if ( this.type != null ) {
            map.put(Constants.PROPERTY_TYPE, type);
        }
        if ( this.isPrivate != null ) {
            map.put(Constants.PROPERTY_PRIVATE, String.valueOf(this.isPrivate));
        }

        return map;
    }

}
