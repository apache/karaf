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
import org.apache.felix.scrplugin.om.*;
import org.apache.felix.scrplugin.tags.*;

/**
 * <code>ClassLoaderJavaTag.java</code>...
 *
 */
public class ClassLoaderJavaTag implements JavaTag {

    protected final JavaClassDescription description;
    protected final Reference reference;
    protected final Property property;
    protected final Interface interf;
    protected boolean isServiceFactory;

    public ClassLoaderJavaTag(JavaClassDescription desc, Reference reference) {
        this.description = desc;
        this.reference = reference;
        this.interf = null;
        this.property = null;
    }

    public ClassLoaderJavaTag(JavaClassDescription desc, Property property) {
        this.description = desc;
        this.property = property;
        this.reference = null;
        this.interf = null;
    }

    public ClassLoaderJavaTag(JavaClassDescription desc, Interface i, boolean isSF) {
        this.interf = i;
        this.description = desc;
        this.property = null;
        this.reference = null;
        this.isServiceFactory = isSF;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaTag#getField()
     */
    public JavaField getField() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaTag#getJavaClassDescription()
     */
    public JavaClassDescription getJavaClassDescription() {
        return this.description;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaTag#getName()
     */
    public String getName() {
        if ( this.reference != null ) {
            return Constants.REFERENCE;
        } else if ( this.property != null ) {
            return Constants.PROPERTY;
        } else if ( this.interf != null ) {
            return Constants.SERVICE;
        }
        return null;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaTag#getNamedParameter(java.lang.String)
     */
    public String getNamedParameter(String name) {
        final Map map = this.getNamedParameterMap();
        if ( map != null ) {
            return (String)map.get(name);
        }
        return null;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaTag#getNamedParameterMap()
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
            map.put(Constants.REFERENCE_CHECKED, String.valueOf(this.reference.isChecked()));
            map.put(Constants.REFERENCE_STRATEGY, this.reference.getStrategy());
            return map;
        } else if ( this.property != null ) {
            final Map map = new HashMap();
            map.put(Constants.PROPERTY_TYPE, this.property.getType());
            map.put(Constants.PROPERTY_NAME, this.property.getName());
            final String[] values = this.property.getMultiValue();
            if ( values != null ) {
                for(int i=0; i<values.length;i++) {
                    map.put(Constants.PROPERTY_MULTIVALUE_PREFIX + '.' + i, values[i]);
                }
            } else {
                map.put(Constants.PROPERTY_VALUE, this.property.getValue());
            }
            map.put(Constants.PROPERTY_PRIVATE, String.valueOf(property.isPrivate()));
            if ( this.property.getLabel() != null ) {
                map.put(Constants.PROPERTY_LABEL, this.property.getLabel());
            }
            if ( this.property.getDescription() != null ) {
                map.put(Constants.PROPERTY_DESCRIPTION, this.property.getDescription());
            }
            if ( this.property.getCardinality() != null ) {
                map.put(Constants.PROPERTY_CARDINALITY, this.property.getCardinality());
            }
            return map;
        } else if ( this.interf != null ) {
            final Map map = new HashMap();
            map.put(Constants.SERVICE_INTERFACE, this.interf.getInterfacename());
            if ( this.isServiceFactory ) {
                map.put(Constants.SERVICE_FACTORY, "true");
            }
            return map;
        }
        return null;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaTag#getParameters()
     */
    public String[] getParameters() {
        final Map map = this.getNamedParameterMap();
        if ( map != null ) {
            return (String[])map.keySet().toArray(new String[5]);
        }
        return new String[0];
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaTag#getSourceLocation()
     */
    public String getSourceLocation() {
        return "Compiled class: " + this.description.getName();
    }
}
