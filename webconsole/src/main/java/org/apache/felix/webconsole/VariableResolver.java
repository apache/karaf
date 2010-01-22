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
package org.apache.felix.webconsole;


/**
 * The <code>VariableResolver</code> interface is a very simple interface which
 * may be implemented by Web Console plugins to provide replacement values for
 * variables in the generated content.
 * <p>
 * The main use of such a variable resolve is when a plugin is using a static
 * template which provides slots to place dynamically generated content
 * parts.
 */
public interface VariableResolver
{

    /**
     * Default implementation of the {@link VariableResolver} interface whose
     * {@link #get(String)} method always returns <code>null</code>.
     */
    VariableResolver DEFAULT = new VariableResolver()
    {
        public String get( String variable )
        {
            return null;
        }
    };


    /**
     * Return a replacement value for the named variable or <code>null</code>
     * if no replacement is available.
     *
     * @param variable The name of the variable for which to return a
     *      replacement.
     * @return The replacement value or <code>null</code> if no replacement is
     *      available.
     */
    String get( String variable );

}
