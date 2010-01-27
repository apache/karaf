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
package org.apache.felix.webconsole.internal.i18n;


import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;


public class ResourceBundleManager implements BundleListener
{

    private final BundleContext bundleContext;

    private final ResourceBundleCache consoleResourceBundleCache;

    private final Map resourceBundleCaches;


    public ResourceBundleManager( final BundleContext bundleContext )
    {
        this.bundleContext = bundleContext;
        this.consoleResourceBundleCache = new ResourceBundleCache( bundleContext.getBundle() );
        this.resourceBundleCaches = new HashMap();

        bundleContext.addBundleListener( this );
    }


    public void dispose()
    {
        bundleContext.removeBundleListener( this );
    }


    public ResourceBundle getResourceBundle( final Bundle provider, final Locale locale )
    {
        // check whether we have to return the resource bundle for the
        // Web Console itself in which case we directly return it
        final ResourceBundle defaultResourceBundle = consoleResourceBundleCache.getResourceBundle( locale );
        if ( provider == null || provider.equals( bundleContext.getBundle() ) )
        {
            return defaultResourceBundle;
        }

        ResourceBundleCache cache;
        synchronized ( resourceBundleCaches )
        {
            Long key = new Long( provider.getBundleId() );
            cache = ( ResourceBundleCache ) resourceBundleCaches.get( key );
            if ( cache == null )
            {
                cache = new ResourceBundleCache( provider );
                resourceBundleCaches.put( key, cache );
            }
        }

        final ResourceBundle bundleResourceBundle = cache.getResourceBundle( locale );
        return new CombinedResourceBundle( bundleResourceBundle, defaultResourceBundle, locale );
    }


    // ---------- BundleListener

    public void bundleChanged( BundleEvent event )
    {
        if ( event.getType() == BundleEvent.STOPPED )
        {
            Long key = new Long( event.getBundle().getBundleId() );
            synchronized ( resourceBundleCaches )
            {
                resourceBundleCaches.remove( key );
            }
        }
    }
}
