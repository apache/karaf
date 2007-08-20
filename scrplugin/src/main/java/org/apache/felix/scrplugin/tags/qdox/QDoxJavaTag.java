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
package org.apache.felix.scrplugin.tags.qdox;

import java.util.Map;

import org.apache.felix.scrplugin.tags.JavaClassDescription;
import org.apache.felix.scrplugin.tags.JavaField;
import org.apache.felix.scrplugin.tags.JavaTag;

import com.thoughtworks.qdox.model.DocletTag;

/**
 * <code>QDoxJavaTag.java</code>...
 *
 */
public class QDoxJavaTag implements JavaTag {

    protected final DocletTag docletTag;

    protected final JavaClassDescription description;

    protected final JavaField field;

    public QDoxJavaTag(DocletTag t, JavaClassDescription desc) {
        this(t, desc, null);
    }

    public QDoxJavaTag(DocletTag t, JavaClassDescription desc, JavaField field) {
        this.docletTag = t;
        this.description = desc;
        this.field = field;
    }

    /**
     * @see org.apache.felix.sandbox.scrplugin.tags.JavaTag#getName()
     */
    public String getName() {
        return this.docletTag.getName();
    }

    /**
     * @see org.apache.felix.sandbox.scrplugin.tags.JavaTag#getNamedParameter(java.lang.String)
     */
    public String getNamedParameter(String arg0) {
        return this.docletTag.getNamedParameter(arg0);
    }

    /**
     * @see org.apache.felix.sandbox.scrplugin.tags.JavaTag#getParameters()
     */
    public String[] getParameters() {
        return this.docletTag.getParameters();
    }

    /**
     * @see org.apache.felix.sandbox.scrplugin.tags.JavaTag#getSourceLocation()
     */
    public String getSourceLocation() {
        return this.docletTag.getContext().getSource().getURL() + ", line " + this.docletTag.getLineNumber();
    }

    /**
     * @see org.apache.felix.sandbox.scrplugin.tags.JavaTag#getJavaClassDescription()
     */
    public JavaClassDescription getJavaClassDescription() {
        return this.description;
    }

    /**
     * @see org.apache.felix.sandbox.scrplugin.tags.JavaTag#getNamedParameterMap()
     */
    public Map getNamedParameterMap() {
        return this.docletTag.getNamedParameterMap();
    }

    /**
     * @see org.apache.felix.sandbox.scrplugin.tags.JavaTag#getField()
     */
    public JavaField getField() {
        return this.field;
    }
}
