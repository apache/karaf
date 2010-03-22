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


import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.json.JSONException;
import org.json.JSONWriter;


/**
 * The <code>WebConsoleUtil</code> provides various utility methods for use
 * by Web Console plugins.
 */
public final class WebConsoleUtil
{

    private WebConsoleUtil()
    {
        /* no instantiation */
    }

    /**
     * Returns the {@link VariableResolver} for the given request.
     * <p>
     * If no resolver has yet be created for the requests, an instance of the
     * {@link DefaultVariableResolver} is created with preset properties,
     * placed into the request and returned. The preset properties are
     * <code>appRoot</code> set to the value of the
     * {@link WebConsoleConstants#ATTR_APP_ROOT} request attribute and
     * <code>pluginRoot</code> set to the value of the
     * {@link WebConsoleConstants#ATTR_PLUGIN_ROOT} request attribute.
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

        final DefaultVariableResolver resolver = new DefaultVariableResolver();
        resolver.put( "appRoot", request.getAttribute( WebConsoleConstants.ATTR_APP_ROOT ) );
        resolver.put( "pluginRoot", request.getAttribute( WebConsoleConstants.ATTR_PLUGIN_ROOT ) );
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


    /**
     * An utility method, that is used to filter out simple parameter from file
     * parameter when multipart transfer encoding is used.
     *
     * This method processes the request and sets a request attribute
     * {@link AbstractWebConsolePlugin#ATTR_FILEUPLOAD}. The attribute value is a {@link Map}
     * where the key is a String specifying the field name and the value
     * is a {@link org.apache.commons.fileupload.FileItem}.
     *
     * @param request the HTTP request coming from the user
     * @param name the name of the parameter
     * @return if not multipart transfer encoding is used - the value is the
     *  parameter value or <code>null</code> if not set. If multipart is used,
     *  and the specified parameter is field - then the value of the parameter
     *  is returned.
     */
    public static final String getParameter( HttpServletRequest request, String name )
    {
        // just get the parameter if not a multipart/form-data POST
        if ( !FileUploadBase.isMultipartContent( new ServletRequestContext( request ) ) )
        {
            return request.getParameter( name );
        }

        // check, whether we already have the parameters
        Map params = ( Map ) request.getAttribute( AbstractWebConsolePlugin.ATTR_FILEUPLOAD );
        if ( params == null )
        {
            // parameters not read yet, read now
            // Create a factory for disk-based file items
            DiskFileItemFactory factory = new DiskFileItemFactory();
            factory.setSizeThreshold( 256000 );

            // Create a new file upload handler
            ServletFileUpload upload = new ServletFileUpload( factory );
            upload.setSizeMax( -1 );

            // Parse the request
            params = new HashMap();
            try
            {
                List items = upload.parseRequest( request );
                for ( Iterator fiter = items.iterator(); fiter.hasNext(); )
                {
                    FileItem fi = ( FileItem ) fiter.next();
                    FileItem[] current = ( FileItem[] ) params.get( fi.getFieldName() );
                    if ( current == null )
                    {
                        current = new FileItem[]
                            { fi };
                    }
                    else
                    {
                        FileItem[] newCurrent = new FileItem[current.length + 1];
                        System.arraycopy( current, 0, newCurrent, 0, current.length );
                        newCurrent[current.length] = fi;
                        current = newCurrent;
                    }
                    params.put( fi.getFieldName(), current );
                }
            }
            catch ( FileUploadException fue )
            {
                // TODO: log
            }
            request.setAttribute( AbstractWebConsolePlugin.ATTR_FILEUPLOAD, params );
        }

        FileItem[] param = ( FileItem[] ) params.get( name );
        if ( param != null )
        {
            for ( int i = 0; i < param.length; i++ )
            {
                if ( param[i].isFormField() )
                {
                    return param[i].getString();
                }
            }
        }

        // no valid string parameter, fail
        return null;
    }

    /**
     * Utility method to handle relative redirects.
     * Some application servers like Web Sphere handle relative redirects differently
     * therefore we should make an absolute URL before invoking send redirect.
     *
     * @param request the HTTP request coming from the user
     * @param response the HTTP response, where data is rendered
     * @param redirectUrl the redirect URI.
     * @throws IOException If an input or output exception occurs
     * @throws IllegalStateException   If the response was committed or if a partial
     *  URL is given and cannot be converted into a valid URL
     */
    public static final void sendRedirect(final HttpServletRequest request,
                                final HttpServletResponse response,
                                String redirectUrl) throws IOException {
        // check for relative URL
        if ( !redirectUrl.startsWith("/") ) {
            String base = request.getContextPath() + request.getServletPath() + request.getPathInfo();
            int i = base.lastIndexOf('/');
            if (i > -1) {
                base = base.substring(0, i);
            } else {
                i = base.indexOf(':');
                base = (i > -1) ? base.substring(i + 1, base.length()) : "";
            }
            if (!base.startsWith("/")) {
                base = '/' + base;
            }
            redirectUrl = base + '/' + redirectUrl;

        }
        response.sendRedirect(redirectUrl);
    }

    /**
     * Sets response headers to force the client to not cache the response
     * sent back. This method must be called before the response is committed
     * otherwise it will have no effect.
     * <p>
     * This method sets the <code>Cache-Control</code>, <code>Expires</code>,
     * and <code>Pragma</code> headers.
     *
     * @param response The response for which to set the cache prevention
     */
    public static final void setNoCache(final HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.addHeader("Cache-Control", "no-store");
        response.addHeader("Cache-Control", "must-revalidate");
        response.addHeader("Cache-Control", "max-age=0");
        response.setHeader("Expires", "Thu, 01 Jan 1970 01:00:00 GMT");
        response.setHeader("Pragma", "no-cache");
    }

    /**
     * Escapes HTML special chars like: <>&\r\n and space
     *
     *
     * @param text the text to escape
     * @return the escaped text
     */
    public static final String escapeHtml(String text)
    {
        StringBuffer sb = new StringBuffer(text.length() * 4 / 3);
        synchronized (sb) // faster buffer operations
        {
            char ch, oldch = '_';
            for (int i = 0; i < text.length(); i++)
            {
                switch (ch = text.charAt(i))
                {
                    case '<':
                        sb.append("&lt;"); //$NON-NLS-1$
                        break;
                    case '>':
                        sb.append("&gt;"); //$NON-NLS-1$
                        break;
                    case '&':
                        sb.append("&amp;"); //$NON-NLS-1$
                        break;
                    case ' ':
                        sb.append("&nbsp;"); //$NON-NLS-1$
                        break;
                    case '\r':
                    case '\n':
                        if (oldch != '\r' && oldch != '\n') // don't add twice <br>
                            sb.append("<br/>\n"); //$NON-NLS-1$
                        break;
                    default:
                        sb.append(ch);
                }
                oldch = ch;
            }

            return sb.toString();
        }
    }

    /**
     * Retrieves a request parameter and converts it to int.
     *
     * @param request the HTTP request
     * @param name the name of the request parameter
     * @param _default the default value returned if the parameter is not set or is not a valid integer.
     * @return the request parameter if set and is valid integer, or the default value
     */
    public static final int getParameterInt(HttpServletRequest request, String name,
        int _default)
    {
        int ret = _default;
        String param = request.getParameter(name);
        try
        {
            if (param != null)
                ret = Integer.parseInt(param);
        }
        catch (NumberFormatException nfe)
        {
            // don't care, will return default
        }

        return ret;
    }

    /**
     * Writes a key-value pair in a JSON writer. Write is performed only if both key and
     * value are not null.
     *
     * @param jw the writer, where to write the data
     * @param key the key value, stored under 'key'
     * @param value the value stored under 'value'
     * @throws JSONException if the value cannot be serialized.
     */
    public static final void keyVal(JSONWriter jw, String key, Object value)
        throws JSONException
    {
        if (key != null && value != null)
        {
            jw.object();
            jw.key("key"); //$NON-NLS-1$
            jw.value(key);
            jw.key("value"); //$NON-NLS-1$
            jw.value(value);
            jw.endObject();
        }
    }


    /**
     * Decode the given value expected to be URL encoded.
     * <p>
     * This method first tries to use the Java 1.4 method
     * <code>URLDecoder.decode(String, String)</code> method and falls back to
     * the now deprecated <code>URLDecoder.decode(String, String)</code>
     * which uses the platform character set to decode the string. This is
     * because the platforms before 1.4 and most notably some OSGi Execution
     * Environments (such as Minimum EE) do not provide the newer method.
     *
     * @param value
     * @return
     */
    public static String urlDecode( final String value )
    {
        // shortcut for empty or missing values
        if ( value == null || value.length() == 0 )
        {
            return null;
        }

        try
        {
            return URLDecoder.decode( value, "UTF-8" );
        }
        catch ( Throwable t )
        {
            // expected NoSuchMethodError: if platform does not support it
            // expected UnsupportedEncoding (not really: UTF-8 is required)
            return URLDecoder.decode( value );
        }
    }
}
