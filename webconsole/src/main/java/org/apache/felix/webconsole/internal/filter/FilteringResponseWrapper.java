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
package org.apache.felix.webconsole.internal.filter;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.ResourceBundle;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.felix.webconsole.VariableResolver;
import org.apache.felix.webconsole.WebConsoleUtil;


/**
 * The <code>FilteringResponseWrapper</code> wraps the response to provide a
 * filtering writer for HTML responses. The filtering writer filters the
 * response such that any string of the form <code>${some text}</code> is
 * replaced by a translation of the <i>some text</i> according to the
 * <code>ResourceBundle</code> provided in the constructor. If no translation
 * exists in the resource bundle, the text is written unmodifed (except the
 * wrapping <code>${}</code> characters are removed.
 */
public class FilteringResponseWrapper extends HttpServletResponseWrapper
{

    // the resource bundle providing translations for the output
    private final ResourceBundle locale;

    // the servlet request providing the variable resolver at the time
    // the getWriter() method is called
    private final ServletRequest request;

    // the writer sending output in this response
    private PrintWriter writer;


    /**
     * Creates a wrapper instance using the given resource bundle for
     * translations.
     *
     * @param response the response to wrap 
     * @param locale a resource bundle, that will be used for translation of the strings
     * @param request the original request - used to obtain the variable resolver
     */
    public FilteringResponseWrapper( final HttpServletResponse response, final ResourceBundle locale,
        final ServletRequest request )
    {
        super( response );
        this.locale = locale;
        this.request = request;
    }


    /**
     * Returns a <code>PrintWriter</code> for the response. If <code>text/html</code>
     * is being generated a filtering writer is returned which translates
     * strings enclosed in <code>${}</code> according to the resource bundle
     * configured for this response.
     *
     * @see javax.servlet.ServletResponseWrapper#getWriter()
     */
    public PrintWriter getWriter() throws IOException
    {
        if ( writer == null )
        {
            final PrintWriter base = super.getWriter();
            if ( doWrap() )
            {
                final VariableResolver resolver = WebConsoleUtil.getVariableResolver( request );
                final ResourceFilteringWriter filter = new ResourceFilteringWriter( base, locale, resolver );
                writer = new PrintWriter( filter );
            }
            else
            {
                writer = base;
            }
        }

        return writer;
    }


    private final boolean doWrap()
    {
        boolean doWrap = getContentType() != null && getContentType().indexOf( "text/html" ) >= 0;
        return doWrap;
    }

}
