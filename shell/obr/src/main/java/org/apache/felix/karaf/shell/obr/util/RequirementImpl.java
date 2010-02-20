/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.karaf.shell.obr.util;

import org.osgi.framework.Filter;
import org.osgi.service.obr.Capability;
import org.osgi.service.obr.Requirement;

/**
 * @version $Rev$ $Date$
 */
public class RequirementImpl implements Requirement {
    private final String name;
    private final Filter filter;
    private final boolean multiple;
    private final boolean optional;
    private final boolean extend;
    private final String comment;

    public RequirementImpl(String name, Filter filter) {
        this(name, filter, false, false, false, null);
    }

    public RequirementImpl(String name, Filter filter, boolean multiple, boolean optional, boolean extend, String comment) {
        this.name = name;
        this.filter = filter;
        this.multiple = multiple;
        this.optional = optional;
        this.extend = extend;
        this.comment = comment;
    }

    public String getName() {
        return name;
    }

    public String getFilter() {
        return filter.toString();
    }

    public boolean isMultiple() {
        return multiple;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean isExtend() {
        return extend;
    }

    public String getComment() {
        return comment;
    }

    public boolean isSatisfied(Capability capability) {
        return filter.match(new MapToDictionary(capability.getProperties()));
    }
}
