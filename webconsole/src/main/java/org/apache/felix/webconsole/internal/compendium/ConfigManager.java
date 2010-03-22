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
package org.apache.felix.webconsole.internal.compendium;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.apache.felix.webconsole.internal.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;


/**
 * The <code>ConfigManager</code> TODO
 */
public class ConfigManager extends ConfigManagerBase
{
    private static final String LABEL = "configMgr"; // was name
    private static final String TITLE = "%config.pluginTitle";
    private static final String CSS[] = { "/res/ui/config.css" };

    private static final String PID_FILTER = "pidFilter";
    private static final String PID = "pid";
    private static final String factoryPID = "factoryPid";

    private static final String PLACEHOLDER_PID = "[Temporary PID replaced by real PID upon save]";

    // templates
    private final String TEMPLATE;

    /** Default constructor */
    public ConfigManager()
    {
        super(LABEL, TITLE, CSS);

        // load templates
        TEMPLATE = readTemplateFile( "/templates/config.html" );
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        // needed multiple times below
        String pid = request.getParameter( ConfigManager.PID );
        if ( pid == null )
        {
            String info = request.getPathInfo();
            pid = info.substring( info.lastIndexOf( '/' ) + 1 );
        }

        // the filter to select part of the configurations
        String pidFilter = request.getParameter( PID_FILTER );

        final ConfigurationAdmin ca = this.getConfigurationAdmin();

        // ignore this request if the pid and/or configuration admin is missing
        if ( pid == null || ca == null )
        {
            // should log this here !!
            return;
        }

        // the configuration to operate on (to be created or "missing")
        Configuration config = null;

        // should actually apply the configuration before redirecting
        if ( request.getParameter( "create" ) != null )
        {
            config = new PlaceholderConfiguration( pid ); // ca.createFactoryConfiguration( pid, null );
            pid = config.getPid();
        }
        else if ( request.getParameter( "apply" ) != null )
        {
            String redirect = applyConfiguration( request, ca, pid );
            if ( redirect != null )
            {
                if (pidFilter != null) {
                    redirect += "?" + PID_FILTER + "=" + pidFilter;
                }

                WebConsoleUtil.sendRedirect(request, response, redirect);
            }
            else
            {
                response.setContentType("text/plain");
                response.getWriter().print("true");
            }

            return;
        }

        if ( config == null )
        {
            config = getConfiguration( ca, pid );
        }

        // check for configuration unbinding
        if ( request.getParameter( "unbind" ) != null )
        {
            config.setBundleLocation( null );
            response.setContentType("text/plain");
            response.getWriter().print("true");
            return;
        }

        // send the result
        response.setContentType( "application/json" );
        response.setCharacterEncoding( "UTF-8" );
        final Locale loc = getLocale( request );
        final String locale = ( loc != null ) ? loc.toString() : null;
        printConfigurationJson( response.getWriter(), pid, config, pidFilter, locale );
    }


    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doGet( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException
    {
        // let's check for a json request
        final String info = request.getPathInfo();
        if ( info.endsWith( ".json" ) )
        {
            response.setContentType( "application/json" );
            response.setCharacterEncoding( "UTF-8" );

            // after last slash and without extension
            String pid = info.substring( info.lastIndexOf( '/' ) + 1, info.length() - 5 );
            // check whether the pid is actually a filter for the selection
            // of configurations to display, if the filter correctly converts
            // into an OSGi filter, we use it to select configurations
            // to display
            String pidFilter = request.getParameter( PID_FILTER );
            if ( pidFilter == null )
            {
                pidFilter = pid;
            }
            try
            {
                getBundleContext().createFilter( pidFilter );

                // if the pidFilter was set from the pid, clear the pid
                if ( pid == pidFilter )
                {
                    pid = null;
                }
            }
            catch ( InvalidSyntaxException ise )
            {
                // its ok, if the pid is just a single PID
                pidFilter = null;
            }

            final ConfigurationAdmin ca = this.getConfigurationAdmin();

            final Locale loc = getLocale( request );
            final String locale = ( loc != null ) ? loc.toString() : null;

            final PrintWriter pw = response.getWriter();

            try {
                pw.write("[");
                final SortedMap services = this.getServices(pid, pidFilter, locale, false);
                final Iterator i = services.keySet().iterator();
                boolean printColon = false;
                while ( i.hasNext() ) {
                    final String servicePid = i.next().toString();

                    final Configuration config = getConfiguration(ca, servicePid);
                    if ( config != null ) {
                        if ( printColon ) {
                            pw.print(',');
                        }
                        this.printConfigurationJson(pw, servicePid, config, pidFilter, locale);
                        printColon = true;
                    }
                }
                pw.write("]");
            } catch (InvalidSyntaxException e) {
                // this should not happened as we checked the filter before
            }
            // nothing more to do
            return;
        }

        super.doGet( request, response );
    }

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {

        // extract the configuration pid from the request path
        String pid = request.getPathInfo().substring(this.getLabel().length() + 1);
        if ( pid.length() == 0 ) {
            pid = null;
        } else {
            pid = pid.substring( pid.lastIndexOf( '/' ) + 1 );
        }
        // check whether the pid is actually a filter for the selection
        // of configurations to display, if the filter correctly converts
        // into an OSGi filter, we use it to select configurations
        // to display
        String pidFilter = request.getParameter( PID_FILTER );
        if ( pidFilter == null )
        {
            pidFilter = pid;
        }
        if ( pidFilter != null )
        {
            try
            {
                getBundleContext().createFilter( pidFilter );

                // if the pidFilter was set from the pid, clear the pid
                if ( pid == pidFilter )
                {
                    pid = null;
                }
            }
            catch ( InvalidSyntaxException ise )
            {
                // its ok, if the pid is just a single PID
                pidFilter = null;
            }
        }

        final ConfigurationAdmin ca = this.getConfigurationAdmin();

        final Locale loc = getLocale( request );
        final String locale = ( loc != null ) ? loc.toString() : null;


        JSONObject json = new JSONObject();
        try
        {
            json.put("status", ca != null ? Boolean.TRUE : Boolean.FALSE);
            if ( ca != null )
            {
                listConfigurations( json, ca, pidFilter, locale, loc );
                listFactoryConfigurations( json, pidFilter, locale );
            }
        }
        catch (JSONException e)
        {
            throw new IOException(e.toString());
        }

        // if a configuration is addressed, display it immediately
        if ( request.getParameter( "create" ) != null && pid != null )
        {
            pid = new PlaceholderConfiguration( pid ).getPid();
        }


        // prepare variables
        DefaultVariableResolver vars = ( ( DefaultVariableResolver ) WebConsoleUtil.getVariableResolver( request ) );
        vars.put( "__data__", json.toString() );
        vars.put( "selectedPid", pid != null ? pid : "");

        response.getWriter().print(TEMPLATE);
    }


    private static final Configuration getConfiguration( ConfigurationAdmin ca, String pid )
    {
        if ( ca != null && pid != null )
        {
            try
            {
                // we use listConfigurations to not create configuration
                // objects persistently without the user providing actual
                // configuration
                String filter = "(" + Constants.SERVICE_PID + "=" + pid + ")";
                Configuration[] configs = ca.listConfigurations( filter );
                if ( configs != null && configs.length > 0 )
                {
                    return configs[0];
                }
            }
            catch ( InvalidSyntaxException ise )
            {
                // should print message
            }
            catch ( IOException ioe )
            {
                // should print message
            }
        }

        // fallback to no configuration at all
        return null;
    }


    private final void listFactoryConfigurations(JSONObject json, String pidFilter,
        String locale)
    {
        try
        {
            Map optionsFactory = getServices(ManagedServiceFactory.class.getName(),
                pidFilter, locale, true);
            addMetaTypeNames(optionsFactory, getFactoryPidObjectClasses(locale),
                pidFilter, ConfigurationAdmin.SERVICE_FACTORYPID);
            for (Iterator i = optionsFactory.keySet().iterator(); i.hasNext();)
            {
                String id = (String) i.next();
                Object name = optionsFactory.get(id);
                json.append("fpids", new JSONObject().put("id", id).put("name", name));
            }
        }
        catch (Exception e)
        {
            log("listFactoryConfigurations: Unexpected problem encountered", e);
        }
    }

    private final void listConfigurations(JSONObject json, ConfigurationAdmin ca,
        String pidFilter, String locale, Locale loc)
    {
        try
        {
            // start with ManagedService instances
            Map optionsPlain = getServices(ManagedService.class.getName(), pidFilter,
                locale, true);

            // next are the MetaType informations without ManagedService
            addMetaTypeNames(optionsPlain, getPidObjectClasses(locale), pidFilter,
                Constants.SERVICE_PID);

            // add in existing configuration (not duplicating ManagedServices)
            Configuration[] cfgs = ca.listConfigurations(pidFilter);
            for (int i = 0; cfgs != null && i < cfgs.length; i++)
            {

                // ignore configuration object if an entry already exists in the map
                String pid = cfgs[i].getPid();
                if (optionsPlain.containsKey(pid))
                {
                    continue;
                }

                // insert and entry for the PID
                ObjectClassDefinition ocd = this.getObjectClassDefinition(cfgs[i], locale);
                String name;
                if (ocd != null)
                {
                    name = ocd.getName();
                }
                else
                {
                    name = pid;
                }

                optionsPlain.put(pid, name);
            }

            for (Iterator i = optionsPlain.keySet().iterator(); i.hasNext();)
            {
                String id = (String) i.next();
                Object name = optionsPlain.get(id);

                final Configuration config = getConfiguration(ca, id);
                JSONObject data = new JSONObject().put("id", id).put("name", name);
                if (null != config)
                {
                    final String fpid = config.getFactoryPid();
                    if (null != fpid)
                    {
                        data.put("fpid", fpid);
                    }

                    final Bundle bundle = getBoundBundle(config);
                    if (null != bundle)
                    {
                        data.put("bundle", bundle.getBundleId());
                        data.put("bundle_name", Util.getName(bundle, loc));
                    }
                }

                json.append("pids", data);
            }
        }
        catch (Exception e)
        {
            log("listConfigurations: Unexpected problem encountered", e);
        }
    }

    private final Bundle getBoundBundle(Configuration config)
    {
        if (null == config)
            return null;
        final String location = config.getBundleLocation();
        if (null == location)
            return null;

        final Bundle bundles[] = getBundleContext().getBundles();
        for (int i = 0; bundles != null && i < bundles.length; i++)
        {
            if (bundles[i].getLocation().equals(location))
                return bundles[i];

        }
        return null;
    }

    private SortedMap getServices( String serviceClass, String serviceFilter, String locale, boolean ocdRequired )
        throws InvalidSyntaxException
    {
        // sorted map of options
        SortedMap optionsFactory = new TreeMap( String.CASE_INSENSITIVE_ORDER );

        // find all ManagedServiceFactories to get the factoryPIDs
        ServiceReference[] refs = this.getBundleContext().getServiceReferences( serviceClass, serviceFilter );
        for ( int i = 0; refs != null && i < refs.length; i++ )
        {
            Object pidObject = refs[i].getProperty( Constants.SERVICE_PID );
            if ( pidObject instanceof String )
            {
                String pid = ( String ) pidObject;
                String name;
                final ObjectClassDefinition ocd = this.getObjectClassDefinition( refs[i].getBundle(), pid, locale );
                if ( ocd != null )
                {
                    name = ocd.getName();
                }
                else
                {
                    name = pid;
                }

                if ( !ocdRequired || ocd != null ) {
                    optionsFactory.put( pid, name );
                }
            }
        }

        return optionsFactory;
    }


    private void addMetaTypeNames( final Map pidMap, final Collection ocdCollection, final String filterSpec, final String type )
    {
        Filter filter = null;
        if ( filterSpec != null )
        {
            try
            {
                filter = getBundleContext().createFilter( filterSpec );
            }
            catch ( InvalidSyntaxException not_expected )
            {
                /* filter is correct */
            }
        }

        for ( Iterator oci = ocdCollection.iterator(); oci.hasNext(); )
        {
            final ObjectClassDefinition ocd = ( ObjectClassDefinition ) oci.next();
            final String pid = ocd.getID();
            final Dictionary props = new Hashtable();
            props.put( type, pid );
            if ( filter == null || filter.match( props ) )
            {
                pidMap.put( pid, ocd.getName() );
            }
        }

    }


    private void printConfigurationJson( PrintWriter pw, String pid, Configuration config, String pidFilter,
        String locale )
    {

        JSONWriter result = new JSONWriter( pw );

        if ( pid != null )
        {
            try
            {
                result.object();
                this.configForm( result, pid, config, pidFilter, locale );
                result.endObject();
            }
            catch ( Exception e )
            {
                log( "Error reading configuration PID " + pid, e );
            }
        }

    }


    private void configForm( JSONWriter json, String pid, Configuration config, String pidFilter, String locale )
        throws JSONException
    {

        json.key( ConfigManager.PID );
        json.value( pid );

        if ( pidFilter != null )
        {
            json.key( PID_FILTER );
            json.value( pidFilter );
        }

        Dictionary props = null;
        ObjectClassDefinition ocd;
        if ( config != null )
        {
            props = config.getProperties();
            ocd = this.getObjectClassDefinition( config, locale );
        }
        else
        {
            ocd = this.getObjectClassDefinition( pid, locale );
        }

        props = this.mergeWithMetaType( props, ocd, json );

        if ( props != null )
        {

            json.key( "title" );
            json.value( pid );
            json.key( "description" );
            json.value( "Please enter configuration properties for this configuration in the field below. This configuration has no associated description" );

            json.key( "propertylist" );
            json.value( "properties" );

            json.key( "properties" );
            json.object();
            for ( Enumeration pe = props.keys(); pe.hasMoreElements(); )
            {
                Object key = pe.nextElement();

                // ignore well known special properties
                if ( !key.equals( Constants.SERVICE_PID ) && !key.equals( Constants.SERVICE_DESCRIPTION )
                    && !key.equals( Constants.SERVICE_ID ) && !key.equals( Constants.SERVICE_RANKING )
                    && !key.equals( Constants.SERVICE_VENDOR )
                    && !key.equals( ConfigurationAdmin.SERVICE_BUNDLELOCATION )
                    && !key.equals( ConfigurationAdmin.SERVICE_FACTORYPID ) )
                {
                    json.key( String.valueOf( key ) );
                    json.value( props.get( key ) );
                }
            }
            json.endObject();

        }

        if ( config != null )
        {
            this.addConfigurationInfo( config, json, locale );
        }
    }


    private Dictionary mergeWithMetaType( Dictionary props, ObjectClassDefinition ocd, JSONWriter json )
        throws JSONException
    {

        if ( props == null )
        {
            props = new Hashtable();
        }

        if ( ocd != null )
        {

            json.key( "title" );
            json.value( ocd.getName() );

            if ( ocd.getDescription() != null )
            {
                json.key( "description" );
                json.value( ocd.getDescription() );
            }

            AttributeDefinition[] ad = ocd.getAttributeDefinitions( ObjectClassDefinition.ALL );
            if ( ad != null )
            {

                JSONArray propertyList = new JSONArray();

                for ( int i = 0; i < ad.length; i++ )
                {
                    json.key( ad[i].getID() );
                    json.object();

                    Object value = props.get( ad[i].getID() );
                    if ( value == null )
                    {
                        value = ad[i].getDefaultValue();
                        if ( value == null )
                        {
                            if ( ad[i].getCardinality() == 0 )
                            {
                                value = "";
                            }
                            else
                            {
                                value = new String[0];
                            }
                        }
                    }

                    json.key( "name" );
                    json.value( ad[i].getName() );

                    json.key( "type" );
                    if ( ad[i].getOptionLabels() != null && ad[i].getOptionLabels().length > 0 )
                    {
                        json.object();
                        json.key( "labels" );
                        json.value( Arrays.asList( ad[i].getOptionLabels() ) );
                        json.key( "values" );
                        json.value( Arrays.asList( ad[i].getOptionValues() ) );
                        json.endObject();
                    }
                    else
                    {
                        json.value( ad[i].getType() );
                    }

                    if ( ad[i].getCardinality() == 0 )
                    {
                        // scalar
                        if ( value instanceof Vector )
                        {
                            value = ( ( Vector ) value ).get( 0 );
                        }
                        else if ( value.getClass().isArray() )
                        {
                            value = Array.get( value, 0 );
                        }
                        json.key( "value" );
                        json.value( value );
                    }
                    else
                    {
                        if ( value instanceof Vector )
                        {
                            value = new JSONArray( ( Vector ) value );
                        }
                        else if ( value.getClass().isArray() )
                        {
                            if ( value.getClass().getComponentType().isPrimitive() )
                            {
                                final int len = Array.getLength(value);
                                final Object[] tmp = new Object[len];
                                for ( int j = 0; j < len; j++ )
                                {
                                    tmp[j] = Array.get(value, j);
                                }
                                value = tmp;
                            }
                            value = new JSONArray( Arrays.asList( ( Object[] ) value ) );
                        }
                        else
                        {
                            JSONArray tmp = new JSONArray();
                            tmp.put( value );
                            value = tmp;
                        }
                        json.key( "values" );
                        json.value( value );
                    }

                    if ( ad[i].getDescription() != null )
                    {
                        json.key( "description" );
                        json.value( ad[i].getDescription() + " (" + ad[i].getID() + ")" );
                    }

                    json.endObject();
                    propertyList.put( ad[i].getID() );
                }

                json.key( "propertylist" );
                json.value( propertyList );
            }

            // nothing more to display
            props = null;
        }

        return props;
    }


    private void addConfigurationInfo( Configuration config, JSONWriter json, String locale ) throws JSONException
    {

        if ( config.getFactoryPid() != null )
        {
            json.key( factoryPID );
            json.value( config.getFactoryPid() );
        }

        String location;
        if ( config.getBundleLocation() == null )
        {
            location = "";
        }
        else
        {
            // if the configuration is bound to a bundle location which
            // is not related to an installed bundle, we just print the
            // raw bundle location binding
            Bundle bundle = this.getBundle( config.getBundleLocation() );
            if ( bundle == null )
            {
                location = config.getBundleLocation();
            }
            else
            {
                Dictionary headers = bundle.getHeaders( locale );
                String name = ( String ) headers.get( Constants.BUNDLE_NAME );
                if ( name == null )
                {
                    location = bundle.getSymbolicName();
                }
                else
                {
                    location = name + " (" + bundle.getSymbolicName() + ")";
                }

                Version v = Version.parseVersion( ( String ) headers.get( Constants.BUNDLE_VERSION ) );
                location += ", Version " + v.toString();
            }
        }
        json.key( "bundleLocation" );
        json.value( location );
        // raw bundle location and service locations
        final String pid = config.getPid();
        String serviceLocation = "";
        try
        {
            final ServiceReference[] refs = getBundleContext().getServiceReferences(
                null,
                "(&(" + Constants.OBJECTCLASS + '=' + ManagedService.class.getName()
                    + ")(" + Constants.SERVICE_PID + '=' + pid + "))");
            if ( refs != null && refs.length > 0 )
            {
                serviceLocation = refs[0].getBundle().getLocation();
            }
        }
        catch (Throwable t)
        {
            log( "Error getting service associated with configuration " + pid, t );
        }
        json.key( "bundle_location" );
        json.value ( config.getBundleLocation() );
        json.key( "service_location" );
        json.value ( serviceLocation );
    }


    private String applyConfiguration( HttpServletRequest request, ConfigurationAdmin ca, String pid )
        throws IOException
    {
        if ( request.getParameter( "delete" ) != null )
        {
            // only delete if the PID is not our place holder
            if ( !PLACEHOLDER_PID.equals( pid ) )
            {
                log( "applyConfiguration: Deleting configuration " + pid );
                Configuration config = ca.getConfiguration( pid, null );
                config.delete();
            }
            return null; // return request.getHeader( "Referer" );
        }

        String factoryPid = request.getParameter( ConfigManager.factoryPID );
        Configuration config = null;

        String propertyList = request.getParameter( "propertylist" );
        if ( propertyList == null )
        {
            String propertiesString = request.getParameter( "properties" );

            if ( propertiesString != null )
            {
                byte[] propBytes = propertiesString.getBytes( "ISO-8859-1" );
                ByteArrayInputStream bin = new ByteArrayInputStream( propBytes );
                Properties props = new Properties();
                props.load( bin );

                config = getConfiguration( ca, pid, factoryPid );
                config.update( props );
            }
        }
        else
        {
            config = getConfiguration( ca, pid, factoryPid );

            Dictionary props = config.getProperties();
            if ( props == null )
            {
                props = new Hashtable();
            }

            Map adMap = this.getAttributeDefinitionMap( config, null );
            if ( adMap != null )
            {
                StringTokenizer propTokens = new StringTokenizer( propertyList, "," );
                while ( propTokens.hasMoreTokens() )
                {
                    String propName = propTokens.nextToken();
                    AttributeDefinition ad = ( AttributeDefinition ) adMap.get( propName );
                    if ( ad == null || ( ad.getCardinality() == 0 && ad.getType() == AttributeDefinition.STRING ) )
                    {
                        String prop = request.getParameter( propName );
                        if ( prop != null )
                        {
                            props.put( propName, prop );
                        }
                    }
                    else if ( ad.getCardinality() == 0 )
                    {
                        // scalar of non-string
                        String prop = request.getParameter( propName );
                        if ( prop != null )
                        {
                            try
                            {
                                props.put( propName, toType( ad.getType(), prop ) );
                            }
                            catch ( NumberFormatException nfe )
                            {
                                // don't care
                            }
                        }
                    }
                    else
                    {
                        // array or vector of any type
                        Vector vec = new Vector();

                        String[] properties = request.getParameterValues( propName );
                        if ( properties != null )
                        {
                            for ( int i = 0; i < properties.length; i++ )
                            {
                                try
                                {
                                    vec.add( toType( ad.getType(), properties[i] ) );
                                }
                                catch ( NumberFormatException nfe )
                                {
                                    // don't care
                                }
                            }
                        }

                        // but ensure size (check for positive value since
                        // abs(Integer.MIN_VALUE) is still INTEGER.MIN_VALUE)
                        int maxSize = Math.abs( ad.getCardinality() );
                        if ( vec.size() > maxSize && maxSize > 0 )
                        {
                            vec.setSize( maxSize );
                        }

                        if ( ad.getCardinality() < 0 )
                        {
                            // keep the vector
                            props.put( propName, vec );
                        }
                        else
                        {
                            // convert to an array
                            props.put( propName, toArray( ad.getType(), vec ) );
                        }
                    }
                }
            }

            config.update( props );
        }

        // redirect to the new configuration (if existing)
        return (config != null) ? config.getPid() : "";
    }


    private static final Configuration getConfiguration( ConfigurationAdmin ca, String pid, String factoryPid ) throws IOException
    {
        if ( factoryPid != null && ( pid == null || pid.equals( PLACEHOLDER_PID ) ) )
        {
            return ca.createFactoryConfiguration( factoryPid, null );
        }

        return ca.getConfiguration( pid, null );
    }


    /**
     * @throws NumberFormatException If the value cannot be converted to
     *      a number and type indicates a numeric type
     */
    private static final Object toType( int type, String value )
    {
        switch ( type )
        {
            case AttributeDefinition.BOOLEAN:
                return Boolean.valueOf( value );
            case AttributeDefinition.BYTE:
                return Byte.valueOf( value );
            case AttributeDefinition.CHARACTER:
                char c = ( value.length() > 0 ) ? value.charAt( 0 ) : 0;
                return new Character( c );
            case AttributeDefinition.DOUBLE:
                return Double.valueOf( value );
            case AttributeDefinition.FLOAT:
                return Float.valueOf( value );
            case AttributeDefinition.LONG:
                return Long.valueOf( value );
            case AttributeDefinition.INTEGER:
                return Integer.valueOf( value );
            case AttributeDefinition.SHORT:
                return Short.valueOf( value );

            default:
                // includes AttributeDefinition.STRING
                return value;
        }
    }


    private static final Object toArray( int type, Vector values )
    {
        int size = values.size();

        // short cut for string array
        if ( type == AttributeDefinition.STRING )
        {
            return values.toArray( new String[size] );
        }

        Object array;
        switch ( type )
        {
            case AttributeDefinition.BOOLEAN:
                array = new boolean[size];
                break;
            case AttributeDefinition.BYTE:
                array = new byte[size];
                break;
            case AttributeDefinition.CHARACTER:
                array = new char[size];
                break;
            case AttributeDefinition.DOUBLE:
                array = new double[size];
                break;
            case AttributeDefinition.FLOAT:
                array = new float[size];
                break;
            case AttributeDefinition.LONG:
                array = new long[size];
                break;
            case AttributeDefinition.INTEGER:
                array = new int[size];
                break;
            case AttributeDefinition.SHORT:
                array = new short[size];
                break;
            default:
                // unexpected, but assume string
                array = new String[size];
        }

        for ( int i = 0; i < size; i++ )
        {
            Array.set( array, i, values.get( i ) );
        }

        return array;
    }

    private static class PlaceholderConfiguration implements Configuration
    {

        private final String factoryPid;
        private String bundleLocation;


        PlaceholderConfiguration( String factoryPid )
        {
            this.factoryPid = factoryPid;
        }


        public String getPid()
        {
            return PLACEHOLDER_PID;
        }


        public String getFactoryPid()
        {
            return factoryPid;
        }


        public void setBundleLocation( String bundleLocation )
        {
            this.bundleLocation = bundleLocation;
        }


        public String getBundleLocation()
        {
            return bundleLocation;
        }


        public Dictionary getProperties()
        {
            // dummy configuration has no properties
            return null;
        }


        public void update()
        {
            // dummy configuration cannot be updated
        }


        public void update( Dictionary properties )
        {
            // dummy configuration cannot be updated
        }


        public void delete()
        {
            // dummy configuration cannot be deleted
        }

    }

}
