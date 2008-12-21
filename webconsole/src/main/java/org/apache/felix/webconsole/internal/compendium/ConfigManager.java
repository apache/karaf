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


import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.internal.Util;
import org.apache.felix.webconsole.internal.servlet.OsgiManager;
import org.json.*;
import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;


/**
 * The <code>ConfigManager</code> TODO
 */
public class ConfigManager extends ConfigManagerBase
{

    private static final String PID_FILTER = "pidFilter";

    public static final String NAME = "configMgr";

    public static final String LABEL = "Configuration";

    public static final String PID = "pid";

    public static final String factoryPID = "factoryPid";

    private static final String PLACEHOLDER_PID = "[Temporary PID replaced by real PID upon save]";


    public String getTitle()
    {
        return LABEL;
    }


    public String getLabel()
    {
        return NAME;
    }


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

        ConfigurationAdmin ca = this.getConfigurationAdmin();

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

                response.sendRedirect( redirect );
            }

            return;
        }

        if ( config == null )
        {
            config = getConfiguration( ca, pid );
        }

        // send the result
        response.setContentType( "text/javascript" );
        response.setCharacterEncoding( "UTF-8" );
        printConfigurationJson( response.getWriter(), pid, config, pidFilter, getLocale( request ) );
    }


    public void renderContent( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {

        // true if MetaType service information is not required
        boolean optionalMetaType = false;

        // extract the configuration pid from the request path
        String pid = request.getPathInfo();
        pid = pid.substring( pid.lastIndexOf( '/' ) + 1 );

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

        Locale loc = getLocale( request );
        String locale = ( loc != null ) ? loc.toString() : null;

        PrintWriter pw = response.getWriter();

        String appRoot = (String) request.getAttribute( OsgiManager.ATTR_APP_ROOT );
        pw.println( "<script src='" + appRoot + "/res/ui/configmanager.js' language='JavaScript'></script>" );

        pw.println( "<table class='content' cellpadding='0' cellspacing='0' width='100%'>" );

        ConfigurationAdmin ca = this.getConfigurationAdmin();
        if ( ca == null )
        {
            pw.println( "<tr class='content' id='configField'>" );
            pw.println( "<td class='content'>&nbsp;</th>" );
            pw.println( "<td class='content'>" );
            pw.print( "Configuration Admin Service not available" );
            pw.println( "</td>" );
            pw.println( "</tr>" );
        }
        else
        {
            pw.println( "<tr class='content' id='configField'>" );
            pw.println( "<td class='content'>Configurations</th>" );
            pw.println( "<td class='content'>" );
            this.listConfigurations( pw, ca, pidFilter, optionalMetaType, locale );
            pw.println( "</td>" );
            pw.println( "</tr>" );

            pw.println( "<tr class='content' id='factoryField'>" );
            pw.println( "<td class='content'>Factory Configurations</th>" );
            pw.println( "<td class='content'>" );
            this.listFactoryConfigurations( pw, ca, pidFilter, optionalMetaType, locale );
            pw.println( "</td>" );
            pw.println( "</tr>" );
        }

        pw.println( "</table>" );

        // if a configuration is addressed, display it immediately
        Configuration config;
        if ( request.getParameter( "create" ) != null && pid != null )
        {
            config = new PlaceholderConfiguration( pid );
            pid = config.getPid();
        }
        else
        {
            config = getConfiguration( getConfigurationAdmin(), pid );
        }

        if ( config != null )
        {
            Util.startScript( pw );

            pw.println( "var configuration=" );
            printConfigurationJson( pw, config.getPid(), config, pidFilter, getLocale( request ) );
            pw.println( ";" );

            pw.println( "displayConfigForm(configuration);" );

            Util.endScript( pw );
        }
    }


    private Configuration getConfiguration( ConfigurationAdmin ca, String pid )
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


    private void listConfigurations( PrintWriter pw, ConfigurationAdmin ca, String pidFilter, boolean optionalMetaType, String locale )
    {
        try
        {
            // start with ManagedService instances
            SortedMap optionsPlain = getServices( ManagedService.class.getName(), pidFilter, optionalMetaType, locale );

            // add in existing configuration (not duplicating ManagedServices)
            Configuration[] cfgs = ca.listConfigurations( pidFilter );
            for ( int i = 0; cfgs != null && i < cfgs.length; i++ )
            {

                // ignore configuration object if an entry already exists in the
                // map
                String pid = cfgs[i].getPid();
                if ( optionsPlain.containsKey( pid ) )
                {
                    continue;
                }

                // insert and entry for the pid
                ObjectClassDefinition ocd = this.getObjectClassDefinition( cfgs[i], locale );
                String name;
                if ( ocd != null )
                {
                    name = ocd.getName() + " (";
                    name += pid + ")";
                }
                else
                {
                    name = pid;
                }

                if ( ocd != null || optionalMetaType )
                {
                    optionsPlain.put( pid, name );
                }
            }

            printOptionsForm( pw, optionsPlain, "configSelection_pid", "configure", "Configure" );
        }
        catch ( Exception e )
        {
            // write a message or ignore
        }
    }


    private void listFactoryConfigurations( PrintWriter pw, ConfigurationAdmin ca, String pidFilter, boolean optionalMetaType,
        String locale )
    {
        try
        {
            SortedMap optionsFactory = getServices( ManagedServiceFactory.class.getName(), pidFilter, optionalMetaType, locale );
            printOptionsForm( pw, optionsFactory, "configSelection_factory", "create", "Create" );
        }
        catch ( Exception e )
        {
            // write a message or ignore
        }
    }


    private SortedMap getServices( String serviceClass, String serviceFilter, boolean optionalMetaType, String locale )
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
                ObjectClassDefinition ocd = this.getObjectClassDefinition( refs[i].getBundle(), pid, locale );
                if ( ocd != null )
                {
                    name = ocd.getName() + " (";
                    name += pid + ")";
                }
                else
                {
                    name = pid;
                }

                if ( ocd != null || optionalMetaType )
                {
                    optionsFactory.put( pid, name );
                }
            }
        }

        return optionsFactory;
    }


    private void printOptionsForm( PrintWriter pw, SortedMap options, String formId, String submitMethod,
        String submitLabel )
    {
        SortedSet set = new TreeSet();
        for ( Iterator ei = options.entrySet().iterator(); ei.hasNext(); )
        {
            Entry entry = ( Entry ) ei.next();
            set.add(entry.getValue().toString() + Character.MAX_VALUE + entry.getKey().toString());
        }

        pw.println( "<select class='select' name='pid' id='" + formId + "' onChange='" + submitMethod + "();'>" );
        for ( Iterator ei = set.iterator(); ei.hasNext(); )
        {
            String entry = ( String ) ei.next();
            int sep = entry.indexOf( Character.MAX_VALUE );
            String value = entry.substring( 0, sep );
            String key = entry.substring( sep + 1 );
            pw.print( "<option value='" + key + "'>" );
            pw.print( value );
            pw.println( "</option>" );
        }
        pw.println( "</select>" );
        pw.println( "&nbsp;&nbsp;" );
        pw.println( "<input class='submit' type='button' value='" + submitLabel + "' onClick='" + submitMethod
            + "();' />" );

    }


    private void printConfigurationJson( PrintWriter pw, String pid, Configuration config, String pidFilter,
        Locale locale )
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
                // add message
            }
        }

    }


    private void configForm( JSONWriter json, String pid, Configuration config, String pidFilter, Locale loc )
        throws JSONException
    {
        String locale = ( loc == null ) ? null : loc.toString();

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
            json
                .value( "Please enter configuration properties for this configuration in the field below. This configuration has no associated description" );

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
                        json.value( ad[i].getDescription() );
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
            location = "None";
        }
        else
        {
            Bundle bundle = this.getBundle( config.getBundleLocation() );

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
        json.key( "bundleLocation" );
        json.value( location );
    }


    private String applyConfiguration( HttpServletRequest request, ConfigurationAdmin ca, String pid )
        throws IOException
    {
        if ( request.getParameter( "delete" ) != null )
        {
            // only delete if the PID is not our place holder
            if ( !PLACEHOLDER_PID.equals( pid ) )
            {
                // TODO: should log this here !!
                Configuration config = ca.getConfiguration( pid, null );
                config.delete();
            }
            return request.getHeader( "Referer" );
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
                                props.put( propName, this.toType( ad.getType(), prop ) );
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
                                    vec.add( this.toType( ad.getType(), properties[i] ) );
                                }
                                catch ( NumberFormatException nfe )
                                {
                                    // don't care
                                }
                            }
                        }

                        // but ensure size
                        int maxSize = Math.abs( ad.getCardinality() );
                        if ( vec.size() > maxSize )
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
                            props.put( propName, this.toArray( ad.getType(), vec ) );
                        }
                    }
                }
            }

            config.update( props );
        }

        // redirect to the new configuration (if existing)
        return (config != null) ? config.getPid() : "";
    }


    private Configuration getConfiguration( ConfigurationAdmin ca, String pid, String factoryPid ) throws IOException
    {
        if ( factoryPid != null )
        {
            return ca.createFactoryConfiguration( factoryPid, null );
        }

        return ca.getConfiguration( pid, null );
    }


    /**
     * @throws NumberFormatException If the value cannot be converted to
     *      a number and type indicates a numeric type
     */
    private Object toType( int type, String value )
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


    private Object toArray( int type, Vector values )
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
            case AttributeDefinition.BYTE:
                array = new byte[size];
            case AttributeDefinition.CHARACTER:
                array = new char[size];
            case AttributeDefinition.DOUBLE:
                array = new double[size];
            case AttributeDefinition.FLOAT:
                array = new float[size];
            case AttributeDefinition.LONG:
                array = new long[size];
            case AttributeDefinition.INTEGER:
                array = new int[size];
            case AttributeDefinition.SHORT:
                array = new short[size];
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
