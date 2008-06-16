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
package org.apache.felix.metatype.internal.l10n;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.osgi.framework.Bundle;
import org.osgi.service.metatype.MetaTypeService;


/**
 * The <code>BundleResources</code> TODO
 *
 * @author fmeschbe
 * @version $Rev$, $Date$
 */
public class BundleResources
{

    private Bundle bundle;
    private long bundleLastModified;

    private Map resourcesByLocale;

    private static Map resourcesByBundle = null;


    public static Resources getResources( Bundle bundle, String basename, String locale )
    {
        BundleResources bundleResources = null;

        if ( resourcesByBundle != null )
        {
            // the bundle has been uninstalled, ensure removed from the cache
            // and return null (e.g. no resources now)
            if ( bundle.getState() == Bundle.UNINSTALLED )
            {
                resourcesByBundle.remove( new Long( bundle.getBundleId() ) );
                return null;
            }

            // else check whether we know the bundle already
            bundleResources = ( BundleResources ) resourcesByBundle.get( new Long( bundle.getBundleId() ) );
        }
        else
        {
            // create the cache to be used for a newly created BundleResources
            resourcesByBundle = new HashMap();
        }

        if ( bundleResources == null )
        {
            bundleResources = new BundleResources( bundle );
            resourcesByBundle.put( new Long( bundle.getBundleId() ), bundleResources );
        }

        return bundleResources.getResources( basename, locale );
    }


    public static void clearResourcesCache()
    {
        resourcesByBundle = null;
    }


    private BundleResources( Bundle bundle )
    {
        this.bundle = bundle;
        this.bundleLastModified = bundle.getLastModified();
        this.resourcesByLocale = new HashMap();
    }


    private boolean isUpToDate()
    {
        return bundle.getState() != Bundle.UNINSTALLED && bundleLastModified >= bundle.getLastModified();
    }


    private Resources getResources( String basename, String locale )
    {
        // ensure locale - use VM default locale if null
        if ( locale == null )
        {
            locale = Locale.getDefault().toString();
        }

        // check the cache, if the bundle has not changed
        if ( isUpToDate() )
        {
            Resources res = ( Resources ) resourcesByLocale.get( locale );
            if ( res != null )
            {
                return res;
            }
        }
        else
        {
            // otherwise clear the cache
            resourcesByLocale.clear();
        }

        // get the list of potential resource names files
        Properties parentProperties = null;
        List resList = createResourceList( locale );
        for ( Iterator ri = resList.iterator(); ri.hasNext(); )
        {
            String tmpLocale = ( String ) ri.next();
            Resources res = ( Resources ) resourcesByLocale.get( tmpLocale );
            if ( res != null )
            {
                parentProperties = res.getResources();
            }
            else
            {
                Properties props = loadProperties( basename, tmpLocale, parentProperties );
                res = new Resources( tmpLocale, props );
                resourcesByLocale.put( tmpLocale, res );
                parentProperties = props;
            }
        }

        // just return from the cache again
        return ( Resources ) resourcesByLocale.get( locale );
    }


    private Properties loadProperties( String basename, String locale, Properties parentProperties )
    {
        String resourceName = basename;
        if ( locale != null && locale.length() > 0 )
        {
            resourceName += "_" + locale;
        }
        resourceName += ".properties";

        Properties props = new Properties( parentProperties );
        URL resURL = bundle.getEntry( resourceName );
        
        // FELIX-607 backwards compatibility, support
        if ( resURL == null )
        {
            resURL = bundle.getEntry( MetaTypeService.METATYPE_DOCUMENTS_LOCATION + "/" + resourceName );
        }
        
        if ( resURL != null )
        {
            InputStream ins = null;
            try
            {
                ins = resURL.openStream();
                props.load( ins );
            }
            catch ( IOException ex )
            {
                // File doesn't exist, just continue loop
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
        }

        return props;
    }


    private List createResourceList( String locale )
    {
        List result = new ArrayList( 4 );

        StringTokenizer tokens;
        StringBuffer tempLocale = new StringBuffer();

        result.add( tempLocale.toString() );

        if ( locale != null && locale.length() > 0 )
        {
            tokens = new StringTokenizer( locale, "_" );
            while ( tokens.hasMoreTokens() )
            {
                if ( tempLocale.length() > 0 )
                {
                    tempLocale.append( "_" );
                }
                tempLocale.append( tokens.nextToken() );
                result.add( tempLocale.toString() );
            }
        }
        return result;
    }
}
