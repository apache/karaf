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
package org.apache.felix.metatype;


import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.felix.metatype.internal.LocalizedObjectClassDefinition;
import org.apache.felix.metatype.internal.l10n.BundleResources;
import org.apache.felix.metatype.internal.l10n.Resources;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;


/**
 * The <code>DefaultMetaTypeProvider</code> class is an implementation of the
 * <code>MetaTypeProvider</code> interface whichis configured for a given
 * bundle using a {@link MetaData} object.
 * <p>
 * This class may be used by clients, e.g. <code>ManagedService</code> or
 * <code>ManagedServiceFactory</code> implementations to easily also implement
 * the <code>MetaTypeProvider</code> interface. 
 *
 * @author fmeschbe
 */
public class DefaultMetaTypeProvider implements MetaTypeProvider
{

    private final Bundle bundle;
    private final String localePrefix;
    
    private Map objectClassDefinitions;
    private Map designates;
    private Map locales;


    public DefaultMetaTypeProvider( Bundle bundle, MetaData metadata )
    {
        this.bundle = bundle;

        // copy from holder
        if ( metadata.getObjectClassDefinitions() == null )
        {
            objectClassDefinitions = Collections.EMPTY_MAP;
        }
        else
        {
            Map copy = new HashMap( metadata.getObjectClassDefinitions() );
            objectClassDefinitions = Collections.unmodifiableMap( copy );
        }
        if ( metadata.getDesignates() == null )
        {
            designates = Collections.EMPTY_MAP;
        }
        else
        {
            Map copy = new HashMap( metadata.getDesignates() );
            designates = Collections.unmodifiableMap( copy );
        }

        String metaDataLocalePrefix = metadata.getLocalePrefix();
        if ( metaDataLocalePrefix == null )
        {
            metaDataLocalePrefix = ( String ) bundle.getHeaders().get( Constants.BUNDLE_LOCALIZATION );
            if ( metaDataLocalePrefix == null )
            {
                metaDataLocalePrefix = Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
            }
        }
        this.localePrefix = metaDataLocalePrefix;
    }


    /**
     * Returns the <code>Bundle</code> to which this instance belongs.
     */
    public Bundle getBundle()
    {
        return bundle;
    }


    /* (non-Javadoc)
     * @see org.osgi.service.metatype.MetaTypeProvider#getLocales()
     */
    public String[] getLocales()
    {
        if ( locales == null )
        {
            String path;
            String pattern;
            int lastSlash = localePrefix.lastIndexOf( '/' );
            if ( lastSlash < 0 )
            {
                path = "/";
                pattern = localePrefix;
            }
            else
            {
                path = localePrefix.substring( 0, lastSlash );
                pattern = localePrefix.substring( lastSlash + 1 );
            }

            Enumeration entries = getBundle().findEntries( path, pattern + "*.properties", false );
            locales = new TreeMap();
            while ( entries.hasMoreElements() )
            {
                URL url = ( URL ) entries.nextElement();
                String name = url.getPath();
                name = name.substring( name.lastIndexOf( '/' ) + 1 + pattern.length(), name.length()
                    - ".properties".length() );
                if ( name.startsWith( "_" ) )
                {
                    name = name.substring( 1 );
                }
                locales.put( name, url );
            }
        }

        // no locales found
        if ( locales.isEmpty() )
        {
            return null;
        }

        return ( String[] ) locales.keySet().toArray( new String[locales.size()] );
    }


    /* (non-Javadoc)
     * @see org.osgi.service.metatype.MetaTypeProvider#getObjectClassDefinition(java.lang.String, java.lang.String)
     */
    public ObjectClassDefinition getObjectClassDefinition( String id, String locale )
    {
        Designate designate = getDesignate( id );
        if ( designate == null || designate.getObject() == null )
        {
            return null;
        }

        String ocdRef = designate.getObject().getOcdRef();
        OCD ocd = ( OCD ) objectClassDefinitions.get( ocdRef );
        if ( ocd == null )
        {
            return null;
        }

        Resources resources = BundleResources.getResources( bundle, localePrefix, locale );
        return new LocalizedObjectClassDefinition( bundle, ocd, resources );
    }


    public Designate getDesignate( String pid )
    {
        return ( Designate ) designates.get( pid );
    }
    
    protected Map getObjectClassDefinitions()
    {
        return objectClassDefinitions;
    }


    protected Map getDesignates()
    {
        return designates;
    }

}
