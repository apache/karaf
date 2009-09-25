/*
 * Copyright 1997-2009 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.felix.webconsole.internal.core;


import java.io.File;
import java.io.InputStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.startlevel.StartLevel;


abstract class InstallHelper extends BaseUpdateInstallHelper
{
    private final BundleContext bundleContext;
    private final String location;
    private final int startlevel;
    private final boolean doStart;

    InstallHelper( final BundleContext bundleContext, final File bundleFile, final String location, final int startlevel,
        final boolean doStart, final boolean refreshPackages )
    {
        super( "Background Install " + bundleFile, bundleFile, refreshPackages );

        this.bundleContext = bundleContext;
        this.location = location;
        this.startlevel = startlevel;
        this.doStart = doStart;
    }


    protected Bundle doRun( InputStream bundleStream ) throws BundleException
    {
        Bundle bundle = bundleContext.installBundle( location, bundleStream );

        if ( startlevel > 0 )
        {
            StartLevel sl = ( StartLevel ) getService( StartLevel.class.getName() );
            if ( sl != null )
            {
                sl.setBundleStartLevel( bundle, startlevel );
            }
        }

        if ( doStart )
        {
            bundle.start();
        }

        return bundle;
    }
}