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
package org.apache.felix.scrplugin.tags.annotation;

import java.util.List;

import org.apache.felix.scrplugin.tags.*;

/**
 * Description of a java field
 */
public class AnnotationJavaField implements JavaField {

    protected final com.thoughtworks.qdox.model.JavaField field;

    protected final AnnotationJavaClassDescription description;

    /**
     * @param field Field
     * @param description description
     */
    public AnnotationJavaField(com.thoughtworks.qdox.model.JavaField field, AnnotationJavaClassDescription description) {
        this.field = field;
        this.description = description;
    }

    /**
     * @see JavaField#getInitializationExpression()
     */
    public String[] getInitializationExpression() {
        return ClassUtil.getInitializationExpression(this.description.getCompiledClass(), this.getName());
    }

    /**
     * @see JavaField#getName()
     */
    public String getName() {
        return this.field.getName();
    }

    /**
     * @see JavaField#getTagByName(String)
     */
    public JavaTag getTagByName(String name) {
        for(com.thoughtworks.qdox.model.Annotation annotation : this.field.getAnnotations()) {
            List<JavaTag> tags =  description.getManager().getAnnotationTagProviderManager().getTags(annotation, this.description, this);
            for (JavaTag tag : tags) {
                if (tag.getName().equals(name)) {
                    return tag;
                }
            }
        }

        return null;
    }

    /**
     * @see JavaField#getType()
     */
    public String getType() {
        return this.field.getType().getValue();
    }

}
