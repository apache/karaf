/*
 *  Copyright 2009 Marcin.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.apache.felix.karaf.webconsole.admin;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;

/**
 * TODO: remove this class when upgrading to webconsole 1.2.12
 */
public abstract class AbstractResourceAwareWebConsolePlugin extends AbstractWebConsolePlugin {

    public static final String GET_RESOURCE_METHOD_NAME = "getResource";

    private final Method getResourceMethod;

    {
        getResourceMethod = getGetResourceMethod();
    }

    /**
     * Renders the web console page for the request. This consist of the following
     * five parts called in order:
     * <ol>
     * <li>Send back a requested resource
     * <li>{@link #startResponse(HttpServletRequest, HttpServletResponse)}</li>
     * <li>{@link #renderTopNavigation(HttpServletRequest, PrintWriter)}</li>
     * <li>{@link #renderContent(HttpServletRequest, HttpServletResponse)}</li>
     * <li>{@link #endResponse(PrintWriter)}</li>
     * </ol>
     * <p>
     * <b>Note</b>: If a resource is sent back for the request only the first
     * step is executed. Otherwise the first step is a null-operation actually
     * and the latter four steps are executed in order.
     */
    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
            IOException
    {
        if ( !spoolResource( request, response ) )
        {
            PrintWriter pw = startResponse( request, response );
            renderTopNavigation( request, pw );
            renderContent( request, response );
            endResponse( pw );
        }
    }

    protected Object getResourceProvider() {
        return this;
    }

    /**
     * Returns a method which is called on the
     * {@link #getResourceProvider() resource provder} class to return an URL
     * to a resource which may be spooled when requested. The method has the
     * following signature:
     * <pre>
     * [modifier] URL getResource(String path);
     * </pre>
     * Where the <i>[modifier]</i> may be <code>public</code>, <code>protected</code>
     * or <code>private</code> (if the method is declared in the class of the
     * resource provider). It is suggested to use the <code>private</code>
     * modifier if the method is declared in the resource provider class or
     * the <code>protected</code> modifier if the method is declared in a
     * base class of the resource provider.
     *
     * @return The <code>getResource(String)</code> method or <code>null</code>
     *      if the {@link #getResourceProvider() resource provider} is
     *      <code>null</code> or does not provide such a method.
     */
    private Method getGetResourceMethod()
    {
        Method tmpGetResourceMethod = null;

        Object resourceProvider = getResourceProvider();
        if ( resourceProvider != null )
        {
            try
            {
                Class cl = resourceProvider.getClass();
                while ( tmpGetResourceMethod == null && cl != Object.class )
                {
                    Method[] methods = cl.getDeclaredMethods();
                    for ( int i = 0; i < methods.length; i++ )
                    {
                        Method m = methods[i];
                        if ( GET_RESOURCE_METHOD_NAME.equals( m.getName() ) && m.getParameterTypes().length == 1
                            && m.getParameterTypes()[0] == String.class && m.getReturnType() == URL.class )
                        {
                            // ensure modifier is protected or public or the private
                            // method is defined in the plugin class itself
                            int mod = m.getModifiers();
                            if ( Modifier.isProtected( mod ) || Modifier.isPublic( mod )
                                || ( Modifier.isPrivate( mod ) && cl == resourceProvider.getClass() ) )
                            {
                                m.setAccessible( true );
                                tmpGetResourceMethod = m;
                                break;
                            }
                        }
                    }
                    cl = cl.getSuperclass();
                }
            }
            catch ( Throwable t )
            {
                tmpGetResourceMethod = null;
            }
        }

        return tmpGetResourceMethod;
    }

    /**
     * If the request addresses a resource which may be served by the
     * <code>getResource</code> method of the
     * {@link #getResourceProvider() resource provider}, this method serves it
     * and returns <code>true</code>. Otherwise <code>false</code> is returned.
     * <code>false</code> is also returned if the resource provider has no
     * <code>getResource</code> method.
     * <p>
     * If <code>true</code> is returned, the request is considered complete and
     * request processing terminates. Otherwise request processing continues
     * with normal plugin rendering.
     *
     * @param request The request object
     * @param response The response object
     * @return <code>true</code> if the request causes a resource to be sent back.
     *
     * @throws IOException If an error occurrs accessing or spooling the resource.
     */
    private boolean spoolResource( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        // no resource if no resource accessor
        if ( getResourceMethod == null )
        {
            return false;
        }

        String pi = request.getPathInfo();
        InputStream ins = null;
        try
        {

            // check for a resource, fail if none
            URL url = ( URL ) getResourceMethod.invoke( getResourceProvider(), new Object[]
                { pi } );
            if ( url == null )
            {
                return false;
            }

            // open the connection and the stream (we use the stream to be able
            // to at least hint to close the connection because there is no
            // method to explicitly close the conneciton, unfortunately)
            URLConnection connection = url.openConnection();
            ins = connection.getInputStream();

            // check whether we may return 304/UNMODIFIED
            long lastModified = connection.getLastModified();
            if ( lastModified > 0 )
            {
                long ifModifiedSince = request.getDateHeader( "If-Modified-Since" );
                if ( ifModifiedSince >= ( lastModified / 1000 * 1000 ) )
                {
                    // Round down to the nearest second for a proper compare
                    // A ifModifiedSince of -1 will always be less
                    response.setStatus( HttpServletResponse.SC_NOT_MODIFIED );

                    return true;
                }

                // have to send, so set the last modified header now
                response.setDateHeader( "Last-Modified", lastModified );
            }

            // describe the contents
            response.setContentType( getServletContext().getMimeType( pi ) );
            response.setIntHeader( "Content-Length", connection.getContentLength() );

            // spool the actual contents
            OutputStream out = response.getOutputStream();
            byte[] buf = new byte[2048];
            int rd;
            while ( ( rd = ins.read( buf ) ) >= 0 )
            {
                out.write( buf, 0, rd );
            }

            // over and out ...
            return true;
        }
        catch ( IllegalAccessException iae )
        {
            // log or throw ???
        }
        catch ( InvocationTargetException ite )
        {
            // log or throw ???
            // Throwable cause = ite.getTargetException();
        }
      finally
        {
            if ( ins != null )
            {
                try
                {
                    ins.close();
                }
                catch ( IOException ignore )
                {
                }
            }
        }

        return false;
    }


}
