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


import javax.servlet.ServletRequest;


/**
 * The <code>WebConsoleUtil</code> provides various utility methods for use
 * by Web Console plugins.
 */
public final class WebConsoleUtil
{

    /**
     * Returns the {@link VariableResolver} for the given request.
     * <p>
     *  If not resolver
     * has yet be created for the requets, an instance of the
     * {@link DefaultVariableResolver} is created, placed into the request and
     * returned.
     * <p>
     * <b>Note</b>: An object not implementing the {@link VariableResolver}
     * interface already stored as the
     * {@link WebConsoleConstants#ATTR_CONSOLE_VARIABLE_RESOLVER} attribute
     * will silently be replaced by the {@link DefaultVariableResolver}
     * instance.
     *
     * @param request The request whose attribute is returned (or set)
     *
     * @return The {@link VariableResolver} for the given request.
     */
    public static VariableResolver getVariableResolver( final ServletRequest request )
    {
        final Object resolverObj = request.getAttribute( WebConsoleConstants.ATTR_CONSOLE_VARIABLE_RESOLVER );
        if ( resolverObj instanceof VariableResolver )
        {
            return ( VariableResolver ) resolverObj;
        }

        final VariableResolver resolver = new DefaultVariableResolver();
        setVariableResolver( request, resolver );
        return resolver;
    }


    /**
     * Sets the {@link VariableResolver} as the
     * {@link WebConsoleConstants#ATTR_CONSOLE_VARIABLE_RESOLVER}
     * attribute in the given request. An attribute of that name already
     * existing is silently replaced.
     *
     * @param request The request whose attribute is set
     * @param resolver The {@link VariableResolver} to place into the request
     */
    public static void setVariableResolver( final ServletRequest request, final VariableResolver resolver )
    {
        request.setAttribute( WebConsoleConstants.ATTR_CONSOLE_VARIABLE_RESOLVER, resolver );
    }

}
