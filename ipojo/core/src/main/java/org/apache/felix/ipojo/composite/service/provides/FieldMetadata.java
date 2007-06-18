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
package org.apache.felix.ipojo.composite.service.provides;

/**
 * Field used inside a composition.
 * This class contains all information useful for the generation.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FieldMetadata {

    /**
     * Name of the field.
     */
    private String m_name;

    /**
     * Is the field an array?
     */
    private boolean m_isAggregate = false;

    /**
     * Interface of the field.
     */
    private SpecificationMetadata m_specification;

    /**
     * Is the field useful in this composition.
     */
    private boolean m_isUseful;

    /**
     * Is the dependency for this field optional.
     */
    private boolean m_isOptional = false;

    /**
     * Constructor.
     * @param specification : interface of the field.
     */
    public FieldMetadata(SpecificationMetadata specification) {
        super();
        this.m_specification = specification;
        if (m_specification.isAggregate()) {
            m_isAggregate = true;
        }
    }

    public boolean isAggregate() {
        return m_isAggregate;
    }

    public void setAggregate(boolean aggregate) {
        m_isAggregate = aggregate;
    }

    public String getName() {
        return m_name;
    }

    public void setName(String name) {
        this.m_name = name;
    }

    public SpecificationMetadata getSpecification() {
        return m_specification;
    }

    public void setSpecification(SpecificationMetadata specification) {
        this.m_specification = specification;
    }

    public boolean isUseful() {
        return m_isUseful;
    }

    public void setUseful(boolean useful) {
        m_isUseful = useful;
    }

    public boolean isOptional() {
        return m_isOptional;
    }

    public void setOptional(boolean opt) {
        m_isOptional = opt;
    }

}
