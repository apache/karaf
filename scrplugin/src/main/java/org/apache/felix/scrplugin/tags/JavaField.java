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
package org.apache.felix.scrplugin.tags;

/**
 * <code>JavaField.java</code>...
 *
 */
public interface JavaField {

    /** The name of the field. */
    String getName();

    /** The type of the field. */
    String getType();

    /**
     * Return the given tag.
     * @param name The tag name.
     * @return The tag or null.
     */
    JavaTag getTagByName(String name);

    /**
     * Return the initial value if this is a static constant.
     * If this field is not an array, an array of length 1 is
     * returned with the value. If this field is an array,
     * the array of values is returned.
     * @return The initial value of the field.
     */
    String[] getInitializationExpression();
}
