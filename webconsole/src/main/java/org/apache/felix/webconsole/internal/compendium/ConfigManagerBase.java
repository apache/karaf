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


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.felix.webconsole.internal.BaseWebConsolePlugin;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;


/**
 * The <code>ConfigManagerBase</code> is the base class for the
 * ConfigurationAdmin support in the web console. It provides various helper
 * methods mostly with respect to using the MetaTypeService to access
 * configuration descriptiorns.
 */
abstract class ConfigManagerBase extends BaseWebConsolePlugin
{

    private static final long serialVersionUID = -6691093960031418130L;

    private static final String CONFIGURATION_ADMIN_NAME = ConfigurationAdmin.class.getName();

    private static final String META_TYPE_NAME = MetaTypeService.class.getName();


    protected ConfigurationAdmin getConfigurationAdmin()
    {
        return ( ConfigurationAdmin ) getService( CONFIGURATION_ADMIN_NAME );
    }


    protected MetaTypeService getMetaTypeService()
    {
        return ( MetaTypeService ) getService( META_TYPE_NAME );
    }


    /**
     * Returns a map of PIDs and providing bundles of MetaType information. The
     * map is indexed by PID and the value of each entry is the bundle providing
     * the MetaType information for that PID.
     */
    protected Collection getPidObjectClasses( final String locale )
    {
        return getObjectClasses( PID_GETTER, locale );
    }


    /**
     * Returns a map of factory PIDs and providing bundles of MetaType
     * information. The map is indexed by factory PID and the value of each
     * entry is the bundle providing the MetaType information for that factory
     * PID.
     */
    protected Collection getFactoryPidObjectClasses( final String locale )
    {
        return getObjectClasses( FACTORY_PID_GETTER, locale );
    }


    /**
     * Returns the <code>ObjectClassDefinition</code> objects for the ids
     * returned by the <code>idGetter</code>. Depending on the
     * <code>idGetter</code> implementation this will be for factory PIDs or
     * plain PIDs.
     * 
     * @param idGetter The {@link IdGetter} used to get the list of factory PIDs
     *          or PIDs from <code>MetaTypeInformation</code> objetcs.
     * @param locale The name of the locale to get the object class definitions
     *          for.
     */
    private Collection getObjectClasses( final IdGetter idGetter, final String locale )
    {
        final Collection objectClasses = new ArrayList();
        final MetaTypeService mts = this.getMetaTypeService();
        if ( mts != null )
        {
            final Bundle[] bundles = this.getBundleContext().getBundles();
            for ( int i = 0; i < bundles.length; i++ )
            {
                final MetaTypeInformation mti = mts.getMetaTypeInformation( bundles[i] );
                if ( mti != null )
                {
                    final String[] idList = idGetter.getIds( mti );
                    for ( int j = 0; idList != null && j < idList.length; j++ )
                    {
                        ObjectClassDefinition ocd = mti.getObjectClassDefinition( idList[j], locale );
                        if ( ocd != null )
                        {
                            objectClasses.add( ocd );
                        }
                    }
                }
            }
        }
        return objectClasses;
    }


    protected ObjectClassDefinition getObjectClassDefinition( Configuration config, String locale )
    {

        // if the configuration is not bound, search in the bundles
        if ( config.getBundleLocation() == null )
        {
            // if the configuration is a factory one, use the factory PID
            if ( config.getFactoryPid() != null )
            {
                return this.getObjectClassDefinition( config.getFactoryPid(), locale );
            }

            // otherwise use the configuration PID
            return this.getObjectClassDefinition( config.getPid(), locale );
        }

        MetaTypeService mts = this.getMetaTypeService();
        if ( mts != null )
        {
            Bundle bundle = this.getBundle( config.getBundleLocation() );
            if ( bundle != null )
            {
                MetaTypeInformation mti = mts.getMetaTypeInformation( bundle );
                if ( mti != null )
                {
                    // check by factory PID
                    if ( config.getFactoryPid() != null )
                    {
                        return mti.getObjectClassDefinition( config.getFactoryPid(), locale );
                    }

                    // otherwise check by configuration PID
                    return mti.getObjectClassDefinition( config.getPid(), locale );
                }
            }
        }

        // fallback to nothing found
        return null;
    }


    protected ObjectClassDefinition getObjectClassDefinition( Bundle bundle, String pid, String locale )
    {
        if ( bundle != null )
        {
            MetaTypeService mts = this.getMetaTypeService();
            if ( mts != null )
            {
                MetaTypeInformation mti = mts.getMetaTypeInformation( bundle );
                if ( mti != null )
                {
                    return mti.getObjectClassDefinition( pid, locale );
                }
            }
        }

        // fallback to nothing found
        return null;
    }


    protected ObjectClassDefinition getObjectClassDefinition( String pid, String locale )
    {
        Bundle[] bundles = this.getBundleContext().getBundles();
        for ( int i = 0; i < bundles.length; i++ )
        {
            try
            {
                ObjectClassDefinition ocd = this.getObjectClassDefinition( bundles[i], pid, locale );
                if ( ocd != null )
                {
                    return ocd;
                }
            }
            catch ( IllegalArgumentException iae )
            {
                // don't care
            }
        }
        return null;
    }


    protected Map getAttributeDefinitionMap( Configuration config, String locale )
    {
        ObjectClassDefinition ocd = this.getObjectClassDefinition( config, locale );
        if ( ocd != null )
        {
            AttributeDefinition[] ad = ocd.getAttributeDefinitions( ObjectClassDefinition.ALL );
            if ( ad != null )
            {
                Map adMap = new HashMap();
                for ( int i = 0; i < ad.length; i++ )
                {
                    adMap.put( ad[i].getID(), ad[i] );
                }
                return adMap;
            }
        }

        // fallback to nothing found
        return null;
    }


    protected Bundle getBundle( String bundleLocation )
    {
        if ( bundleLocation == null )
        {
            return null;
        }

        Bundle[] bundles = this.getBundleContext().getBundles();
        for ( int i = 0; i < bundles.length; i++ )
        {
            if ( bundleLocation.equals( bundles[i].getLocation() ) )
            {
                return bundles[i];
            }
        }

        return null;
    }


    protected Locale getLocale( HttpServletRequest request )
    {
        try
        {
            return request.getLocale();
        }
        catch ( Throwable t )
        {
            // expected in standard OSGi Servlet 2.1 environments
            // fallback to using the default locale
            return Locale.getDefault();
        }
    }

    /**
     * The <code>IdGetter</code> interface is an internal helper to abstract
     * retrieving object class definitions from all bundles for either
     * pids or factory pids.
     * 
     * @see #PID_GETTER
     * @see #FACTORY_PID_GETTER
     */
    private static interface IdGetter
    {
        String[] getIds( MetaTypeInformation metaTypeInformation );
    }

    /** 
     * The implementation of the {@link IdGetter} interface returning the PIDs
     * listed in the meta type information.
     * 
     * @see #getPidObjectClasses(String)
     */
    private static final IdGetter PID_GETTER = new IdGetter()
    {
        public String[] getIds( MetaTypeInformation metaTypeInformation )
        {
            return metaTypeInformation.getPids();
        }
    };

    /** 
     * The implementation of the {@link IdGetter} interface returning the
     * factory PIDs listed in the meta type information.
     * 
     * @see #getFactoryPidObjectClasses(String)
     */
    private static final IdGetter FACTORY_PID_GETTER = new IdGetter()
    {
        public String[] getIds( MetaTypeInformation metaTypeInformation )
        {
            return metaTypeInformation.getFactoryPids();
        }
    };

}
