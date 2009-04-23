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

import org.apache.felix.scrplugin.tags.JavaField;
import org.apache.felix.scrplugin.tags.JavaTag;

import com.thoughtworks.qdox.model.Annotation;

/**
 * Interface for provider classes, that map java annotations to {@link JavaTag}
 * implementations.
 */
public interface AnnotationTagProvider {

    /**
     * Maps a annotation to one or many {@link JavaTag} implementations.
     * @param pAnnotation Java annotation
     * @param description Annotations-based java class description
     * @param field Reference to field (set on field-level annotations, null on
     *            other annotations)
     * @return List of tag implementations. Return empty list if this provider
     *         cannot map the annotation to any tag instance.
     */
    List<JavaTag> getTags(Annotation pAnnotation, AnnotationJavaClassDescription description, JavaField field);
}
