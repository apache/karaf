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
package org.apache.felix.bundlerepository.impl.wrapper;

import org.apache.felix.bundlerepository.Requirement;

public class RequirementWrapper implements org.osgi.service.obr.Requirement {

    final Requirement requirement;

    public RequirementWrapper(Requirement requirement) {
        this.requirement = requirement;
    }

    public String getName() {
        return requirement.getName();
    }

    public String getFilter() {
        return requirement.getFilter();
    }

    public boolean isMultiple() {
        return requirement.isMultiple();
    }

    public boolean isOptional() {
        return requirement.isOptional();
    }

    public boolean isExtend() {
        return requirement.isExtend();
    }

    public String getComment() {
        return requirement.getComment();
    }

    public boolean isSatisfied(org.osgi.service.obr.Capability capability) {
        return requirement.isSatisfied(Wrapper.unwrap(capability));
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RequirementWrapper that = (RequirementWrapper) o;

        if (requirement != null ? !requirement.equals(that.requirement) : that.requirement != null) return false;

        return true;
    }

    public int hashCode() {
        return requirement != null ? requirement.hashCode() : 0;
    }
}
