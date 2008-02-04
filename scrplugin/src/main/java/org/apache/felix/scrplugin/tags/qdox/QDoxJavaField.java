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

import java.lang.reflect.Field;

import org.apache.felix.scrplugin.tags.JavaField;
import org.apache.felix.scrplugin.tags.JavaTag;

import com.thoughtworks.qdox.model.DocletTag;

/**
 * <code>QDoxJavaField.java</code>...
 *
 */
public class QDoxJavaField implements JavaField {

    protected final com.thoughtworks.qdox.model.JavaField field;

    protected final QDoxJavaClassDescription description;

    public QDoxJavaField(com.thoughtworks.qdox.model.JavaField f, QDoxJavaClassDescription d) {
        this.field = f;
        this.description = d;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaField#getInitializationExpression()
     */
    public String getInitializationExpression() {
        final Class c = this.description.getCompiledClass();
        try {
            final Field field = c.getDeclaredField(this.getName());
            field.setAccessible(true);
            final Object value = field.get(null);
            if ( value != null ) {
                return value.toString();
            }
            return null;
        } catch (NoClassDefFoundError e) {
            // ignore and try qdox
        } catch (Exception e) {
            // ignore and try qdox
        }
        String value = this.field.getInitializationExpression();
        if ( value != null ) {
            int pos = value.indexOf("\"");
            if ( pos != -1 ) {
                try {
                    value = value.substring(pos + 1);
                    value = value.substring(0, value.lastIndexOf("\""));
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    // ignore this as this is a qdox problem
                    value = this.field.getInitializationExpression();
                }
            }
        }
        return value;
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
        final DocletTag tag = this.field.getTagByName(name);
        if ( tag == null ) {
            return null;
        }
        return new QDoxJavaTag(tag, this.description, this);
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaField#getType()
     */
    public String getType() {
        return this.field.getType().getValue();
    }
}
