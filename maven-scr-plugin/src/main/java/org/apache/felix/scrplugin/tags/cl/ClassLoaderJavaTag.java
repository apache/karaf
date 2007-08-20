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
package org.apache.felix.scrplugin.tags.cl;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scrplugin.Constants;
import org.apache.felix.scrplugin.om.Reference;
import org.apache.felix.scrplugin.tags.JavaClassDescription;
import org.apache.felix.scrplugin.tags.JavaField;
import org.apache.felix.scrplugin.tags.JavaTag;

/**
 * <code>ClassLoaderJavaTag.java</code>...
 *
 */
public class ClassLoaderJavaTag implements JavaTag {

    protected final JavaClassDescription description;
    protected final Reference reference;

    public ClassLoaderJavaTag(JavaClassDescription desc, Reference reference) {
        this.reference = reference;
        this.description = desc;
    }

    public JavaField getField() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.apache.felix.sandbox.scrplugin.tags.JavaTag#getJavaClassDescription()
     */
    public JavaClassDescription getJavaClassDescription() {
        return this.description;
    }

    /**
     * @see org.apache.felix.sandbox.scrplugin.tags.JavaTag#getName()
     */
    public String getName() {
        if ( this.reference != null ) {
            return Constants.REFERENCE;
        }
        return null;
    }

    /**
     * @see org.apache.felix.sandbox.scrplugin.tags.JavaTag#getNamedParameter(java.lang.String)
     */
    public String getNamedParameter(String name) {
        final Map map = this.getNamedParameterMap();
        if ( map != null ) {
            return (String)map.get(name);
        }
        return null;
    }

    /**
     * @see org.apache.felix.sandbox.scrplugin.tags.JavaTag#getNamedParameterMap()
     */
    public Map getNamedParameterMap() {
        if ( this.reference != null ) {
            final Map map = new HashMap();
            map.put(Constants.REFERENCE_BIND, this.reference.getBind());
            map.put(Constants.REFERENCE_CARDINALITY, this.reference.getCardinality());
            map.put(Constants.REFERENCE_INTERFACE, this.reference.getInterfacename());
            map.put(Constants.REFERENCE_NAME, this.reference.getName());
            map.put(Constants.REFERENCE_POLICY, this.reference.getPolicy());
            map.put(Constants.REFERENCE_TARGET, this.reference.getTarget());
            map.put(Constants.REFERENCE_UNDBIND, this.reference.getUnbind());
            return map;
        }
        return null;
    }

    /**
     * @see org.apache.felix.sandbox.scrplugin.tags.JavaTag#getParameters()
     */
    public String[] getParameters() {
        final Map map = this.getNamedParameterMap();
        if ( map != null ) {
            return (String[])map.keySet().toArray(new String[5]);
        }
        return new String[0];
    }

    /**
     * @see org.apache.felix.sandbox.scrplugin.tags.JavaTag#getSourceLocation()
     */
    public String getSourceLocation() {
        return "Compiled class: " + this.description.getName();
    }
}
