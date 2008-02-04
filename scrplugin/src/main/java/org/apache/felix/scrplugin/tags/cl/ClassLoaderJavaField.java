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

import java.lang.reflect.Field;

import org.apache.felix.scrplugin.tags.*;

/**
 * <code>ClassLoaderJavaField.java</code>...
 *
 */
public class ClassLoaderJavaField implements JavaField {

    protected final Field field;

    protected final JavaClassDescription description;

    public ClassLoaderJavaField(Field f, JavaClassDescription d) {
        this.field = f;
        this.description = d;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaField#getInitializationExpression()
     */
    public String[] getInitializationExpression() {
        return ClassUtil.getInitializationExpression(this.field.getDeclaringClass(), this.field.getName());
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaField#getName()
     */
    public String getName() {
        return this.field.getName();
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaField#getTagByName(java.lang.String)
     */
    public JavaTag getTagByName(String name) {
        // TODO
        return null;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaField#getType()
     */
    public String getType() {
        return this.field.getType().getName();
    }
}
