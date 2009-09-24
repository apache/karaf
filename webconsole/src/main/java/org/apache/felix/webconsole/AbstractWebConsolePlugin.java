/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.webconsole;


import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.felix.webconsole.internal.WebConsolePluginAdapter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;


public abstract class AbstractWebConsolePlugin extends HttpServlet
{

    /** Pseudo class version ID to keep the IDE quite. */
    private static final long serialVersionUID = 1L;

    /** The name of the request attribute containig the map of FileItems from the POST request */
    public static final String ATTR_FILEUPLOAD = "org.apache.felix.webconsole.fileupload";

    public static final String GET_RESOURCE_METHOD_NAME = "getResource";

    /**
     * The reference to the getResource method provided by the
     * {@link #getResourceProvider()}. This is <code>null</code> if there is
     * none or before the first check if there is one.
     *
     * @see #getGetResourceMethod()
     */
    private Method getResourceMethod;

    /**
     * flag indicating whether the getResource method has already been looked
     * up or not. This prevens the {@link #getGetResourceMethod()} method from
     * repeatedly looking up the resource method on plugins which do not have
     * one.
     */
    private boolean getResourceMethodChecked;

    private BundleContext bundleContext;

    private String adminTitle;

    private static BrandingPlugin brandingPlugin = DefaultBrandingPlugin.getInstance();

    //---------- HttpServlet Overwrites ----------------------------------------

    /**
     * Returns the title for this plugin as returned by {@link #getTitle()}
     */
    public String getServletName()
    {
        return getTitle();
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
            // detect if this is an html request
            if ( isHtmlRequest(request) )
            {
                // start the html response, write the header, open body and main div
                PrintWriter pw = startResponse( request, response );

                // render top navigation
                renderTopNavigation( request, pw );

                // wrap content in a separate div
                pw.println( "<div id='content'>" );
                renderContent( request, response );
                pw.println( "</div>" );

                // close the main div, body, and html
                endResponse( pw );
            }
            else
            {
                renderContent( request, response );
            }
        }
    }


    /**
     * Detects whether this request is intended to have the headers and
     * footers of this plugin be rendered or not. This method always returns
     * <code>true</true> but has been overwritten in the
     * {@link WebConsolePluginAdapter} for the plugins.
     */
    protected boolean isHtmlRequest( final HttpServletRequest request )
    {
        return true;
    }


    //---------- AbstractWebConsolePlugin API ----------------------------------

    public void activate( BundleContext bundleContext )
    {
        this.bundleContext = bundleContext;

        Dictionary headers = bundleContext.getBundle().getHeaders();

        adminTitle = ( String ) headers.get( Constants.BUNDLE_NAME );
    }


    public void deactivate()
    {
        this.bundleContext = null;
    }


    public abstract String getTitle();


    public abstract String getLabel();


    protected abstract void renderContent( HttpServletRequest req, HttpServletResponse res ) throws ServletException,
        IOException;


    /**
     * Returns a list of CSS reference paths or <code>null</code> if no
     * additional CSS files are provided by the plugin.
     * <p>
     * The result is an array of strings which are used as the value of
     * the <code>href</code> attribute of the <code>&lt;link&gt;</code> elements
     * placed in the head section of the HTML generated. If the reference is
     * a relative path, it is turned into an absolute path by prepending the
     * value of the {@link WebConsoleConstants#ATTR_APP_ROOT} request attribute.
     *
     * @return The list of additional CSS files to reference in the head
     *      section or <code>null</code> if no such CSS files are required.
     */
    protected String[] getCssReferences()
    {
        return null;
    }


    protected BundleContext getBundleContext()
    {
        return bundleContext;
    }


    /**
     * Returns the object which might provide resources. The class of this
     * object is used to find the <code>getResource</code> method.
     * <p>
     * This method may be overwritten by extensions. This base class
     * implementation returns this instance.
     *
     * @return The resource provider object or <code>null</code> if no
     *      resources will be provided by this plugin.
     */
    protected Object getResourceProvider()
    {
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
        // return what we know of the getResourceMethod, if we already checked
        if (getResourceMethodChecked) {
            return getResourceMethod;
        }

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

        // set what we have found and prevent future lookups
        getResourceMethod = tmpGetResourceMethod;
        getResourceMethodChecked = true;

        // now also return the method
        return getResourceMethod;
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
        Method getResourceMethod = getGetResourceMethod();
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


    protected PrintWriter startResponse( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        response.setCharacterEncoding( "utf-8" );
        response.setContentType( "text/html" );

        final PrintWriter pw = response.getWriter();

        final String appRoot = ( String ) request.getAttribute( WebConsoleConstants.ATTR_APP_ROOT );

        String header = MessageFormat.format( getHeader(), new Object[]
            { adminTitle, getTitle(), appRoot, getLabel(), brandingPlugin.getFavIcon(),
                brandingPlugin.getMainStyleSheet(), brandingPlugin.getProductURL(), brandingPlugin.getProductName(),
                brandingPlugin.getProductImage(), getCssLinks( appRoot ) } );
        pw.println( header );

        return pw;
    }


    protected void renderTopNavigation( HttpServletRequest request, PrintWriter pw )
    {
        // assume pathInfo to not be null, else this would not be called
        boolean linkToCurrent = true;
        String current = request.getPathInfo();
        int slash = current.indexOf( "/", 1 );
        if ( slash < 0 )
        {
            slash = current.length();
            linkToCurrent = false;
        }
        current = current.substring( 1, slash );

        boolean disabled = false;
        String appRoot = ( String ) request.getAttribute( WebConsoleConstants.ATTR_APP_ROOT );
        Map labelMap = ( Map ) request.getAttribute( WebConsoleConstants.ATTR_LABEL_MAP );
        if ( labelMap != null )
        {

            // prepare the navigation
            SortedMap map = new TreeMap( String.CASE_INSENSITIVE_ORDER );
            for ( Iterator ri = labelMap.entrySet().iterator(); ri.hasNext(); )
            {
                Map.Entry labelMapEntry = ( Map.Entry ) ri.next();
                if ( labelMapEntry.getKey() == null )
                {
                    // ignore renders without a label
                }
                else if ( disabled || current.equals( labelMapEntry.getKey() ) )
                {
                    if ( linkToCurrent )
                    {
                        map.put( labelMapEntry.getValue(), "<a class='technavat' href='" + appRoot + "/"
                            + labelMapEntry.getKey() + "'>" + labelMapEntry.getValue() + "</a>" );
                    }
                    else
                    {
                        map.put( labelMapEntry.getValue(), "<span class='technavat'>" + labelMapEntry.getValue()
                            + "</span>" );
                    }
                }
                else
                {
                    map.put( labelMapEntry.getValue(), "<a href='" + appRoot + "/" + labelMapEntry.getKey() + "'>"
                        + labelMapEntry.getValue() + "</a>" );
                }
            }

            // render the navigation
            pw.println( "<div id='technav'>" );
            for ( Iterator li = map.values().iterator(); li.hasNext(); )
            {
                pw.print( "<div class='technavitem'>" );
                pw.print( li.next() );
                pw.println( "</div>" );
            }
            pw.println( "</div>" );
        }
    }


    protected void endResponse( PrintWriter pw )
    {
        pw.println(getFooter());
    }


    public static String getParameter( HttpServletRequest request, String name )
    {
        // just get the parameter if not a multipart/form-data POST
        if ( !ServletFileUpload.isMultipartContent( new ServletRequestContext( request ) ) )
        {
            return request.getParameter( name );
        }

        // check, whether we alread have the parameters
        Map params = ( Map ) request.getAttribute( ATTR_FILEUPLOAD );
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
            request.setAttribute( ATTR_FILEUPLOAD, params );
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
     * Some app servers like web sphere handle relative redirects differently
     * therefore we should make an absolute url before invoking send redirect.
     */
    protected void sendRedirect(final HttpServletRequest request,
                                final HttpServletResponse response,
                                String redirectUrl) throws IOException {
        // check for relative url
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
     * @return the brandingPlugin
     */
    public static BrandingPlugin getBrandingPlugin() {
        return AbstractWebConsolePlugin.brandingPlugin;
    }

    /**
     * @param brandingPlugin the brandingPlugin to set
     */
    public static void setBrandingPlugin(BrandingPlugin brandingPlugin) {
        if(brandingPlugin == null){
            AbstractWebConsolePlugin.brandingPlugin = DefaultBrandingPlugin.getInstance();
        } else {
            AbstractWebConsolePlugin.brandingPlugin = brandingPlugin;
        }
    }


    private String getHeader()
    {
        // MessageFormat pattern place holder
        //  0 main title (plugin providing bundle name)
        //  1 console plugin title
        //  2 application root path (ATTR_APP_ROOT)
        //  3 console plugin label (from the URI)
        //  4 branding favourite icon (BrandingPlugin.getFavIcon())
        //  5 branding main style sheet (BrandingPlugin.getMainStyleSheet())
        //  6 branding product URL (BrandingPlugin.getProductURL())
        //  7 branding product name (BrandingPlugin.getProductName())
        //  8 branding product image (BrandingPlugin.getProductImage())
        //  9 additional HTML code to be inserted into the <head> section
        //    (for example plugin provided CSS links)

        final String header = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"

        + "<html xmlns=\"http://www.w3.org/1999/xhtml\">"
        + "  <head>"
        + "    <meta http-equiv=\"Content-Type\" content=\"text/html; utf-8\">"
        + "    <link rel=\"icon\" href=\"{2}{4}\">"
        + "    <title>{0} - {1}</title>"

        + "    <link href=\"{2}/res/ui/admin.css\" rel=\"stylesheet\" type=\"text/css\">"
        + "    <link href=\"{2}{5}\" rel=\"stylesheet\" type=\"text/css\">"
        + "    {9}"

        + "    <script language=\"JavaScript\">"
        + "      appRoot = \"{2}\";"
        + "      pluginRoot = \"{2}/{3}\";"
        + "    </script>"

        + "    <script src=\"{2}/res/ui/jquery-1.3.2.min.js\" language=\"JavaScript\"></script>"
        + "    <script src=\"{2}/res/ui/jquery.tablesorter-2.0.3.min.js\" language=\"JavaScript\"></script>"

        + "    <script src=\"{2}/res/ui/admin.js\" language=\"JavaScript\"></script>"
        + "    <script src=\"{2}/res/ui/ui.js\" language=\"JavaScript\"></script>"

        + "  </head>"
        + "  <body>"
        + "    <div id=\"main\">"
        + "      <div id=\"lead\">"
        + "        <h1>"
        + "          {0}<br>{1}"
        + "        </h1>"
        + "        <p>"
        + "          <a target=\"_blank\" href=\"{6}\" title=\"{7}\"><img src=\"{2}{8}\" border=\"0\"></a>"
        + "        </p>"
        + "      </div>";
        return header;
    }


    private String getFooter()
    {
        // close <div id="main">, body and html
        final String footer = "    </div>"
            + "  </body>"
            + "</html>";
        return footer;
    }


    private String getCssLinks( final String appRoot )
    {
        // get the CSS references and return nothing if there are none
        final String[] cssRefs = getCssReferences();
        if ( cssRefs == null )
        {
            return "";
        }

        // build the CSS links from the references
        final StringBuffer buf = new StringBuffer();
        for ( int i = 0; i < cssRefs.length; i++ )
        {
            buf.append( "<link href='" );

            final String cssRef = cssRefs[i];
            if ( cssRef.startsWith( "/" ) )
            {
                buf.append( appRoot );
            }

            buf.append( cssRef ).append( "' rel='stylesheet' type='text/css'>" );
        }

        return buf.toString();
    }
}
