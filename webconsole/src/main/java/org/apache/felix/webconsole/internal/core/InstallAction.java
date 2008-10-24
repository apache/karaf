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
package org.apache.felix.webconsole.internal.core;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;


/**
 * The <code>InstallAction</code> TODO
 */
public class InstallAction extends BundleAction
{

    public static final String NAME = "install";

    public static final String LABEL = "Install or Update";

    public static final String FIELD_STARTLEVEL = "bundlestartlevel";

    public static final String FIELD_START = "bundlestart";

    public static final String FIELD_BUNDLEFILE = "bundlefile";

    // set to ask for PackageAdmin.refreshPackages() after install/update
    public static final String FIELD_REFRESH_PACKAGES = "refreshPackages";


    public String getName()
    {
        return NAME;
    }


    public String getLabel()
    {
        return LABEL;
    }


    public boolean performAction( HttpServletRequest request, HttpServletResponse response )
    {

        // get the uploaded data
        Map params = ( Map ) request.getAttribute( AbstractWebConsolePlugin.ATTR_FILEUPLOAD );
        if ( params == null )
        {
            return true;
        }

        FileItem startItem = getFileItem( params, FIELD_START, true );
        FileItem startLevelItem = getFileItem( params, FIELD_STARTLEVEL, true );
        FileItem bundleItem = getFileItem( params, FIELD_BUNDLEFILE, false );
        FileItem refreshPackagesItem = getFileItem( params, FIELD_REFRESH_PACKAGES, true );

        // don't care any more if not bundle item
        if ( bundleItem == null || bundleItem.getSize() <= 0 )
        {
            return true;
        }

        // default values
        // it exists
        int startLevel = -1;
        String bundleLocation = "inputstream:";

        // convert the start level value
        if ( startLevelItem != null )
        {
            try
            {
                startLevel = Integer.parseInt( startLevelItem.getString() );
            }
            catch ( NumberFormatException nfe )
            {
                getLog().log( LogService.LOG_INFO,
                    "Cannot parse start level parameter " + startLevelItem + " to a number, not setting start level" );
            }
        }

        // write the bundle data to a temporary file to ease processing
        File tmpFile = null;
        try
        {
            // copy the data to a file for better processing
            tmpFile = File.createTempFile( "install", ".tmp" );
            bundleItem.write( tmpFile );
        }
        catch ( Exception e )
        {
            getLog().log( LogService.LOG_ERROR, "Problem accessing uploaded bundle file", e );

            // remove the tmporary file
            if ( tmpFile != null )
            {
                tmpFile.delete();
                tmpFile = null;
            }
        }

        // install or update the bundle now
        if ( tmpFile != null )
        {
            // start, refreshPackages just needs to exist, don't care for value
            boolean start = startItem != null;
            boolean refreshPackages = refreshPackagesItem != null;

            bundleLocation = "inputstream:" + bundleItem.getName();
            installBundle( bundleLocation, tmpFile, startLevel, start, refreshPackages );
        }

        return true;
    }


    private FileItem getFileItem( Map params, String name, boolean isFormField )
    {
        FileItem[] items = ( FileItem[] ) params.get( name );
        if ( items != null )
        {
            for ( int i = 0; i < items.length; i++ )
            {
                if ( items[i].isFormField() == isFormField )
                {
                    return items[i];
                }
            }
        }

        // nothing found, fail
        return null;
    }


    private void installBundle( String location, File bundleFile, int startLevel, boolean start, boolean refreshPackages )
    {
        if ( bundleFile != null )
        {

            // try to get the bundle name, fail if none
            String symbolicName = getSymbolicName( bundleFile );
            if ( symbolicName == null )
            {
                bundleFile.delete();
                return;
            }

            // check for existing bundle first
            Bundle updateBundle = null;
            Bundle[] bundles = getBundleContext().getBundles();
            for ( int i = 0; i < bundles.length; i++ )
            {
                if ( ( bundles[i].getLocation() != null && bundles[i].getLocation().equals( location ) )
                    || ( bundles[i].getSymbolicName() != null && bundles[i].getSymbolicName().equals( symbolicName ) ) )
                {
                    updateBundle = bundles[i];
                    break;
                }
            }

            if ( updateBundle != null )
            {

                updateBackground( updateBundle, bundleFile, refreshPackages );

            }
            else
            {

                installBackground( bundleFile, location, startLevel, start, refreshPackages );

            }
        }
    }


    private String getSymbolicName( File bundleFile )
    {
        JarFile jar = null;
        try
        {
            jar = new JarFile( bundleFile );
            Manifest m = jar.getManifest();
            if ( m != null )
            {
                return m.getMainAttributes().getValue( Constants.BUNDLE_SYMBOLICNAME );
            }
        }
        catch ( IOException ioe )
        {
            getLog().log( LogService.LOG_WARNING, "Cannot extract symbolic name of bundle file " + bundleFile, ioe );
        }
        finally
        {
            if ( jar != null )
            {
                try
                {
                    jar.close();
                }
                catch ( IOException ioe )
                {
                    // ignore
                }
            }
        }

        // fall back to "not found"
        return null;
    }


    private void installBackground( final File bundleFile, final String location, final int startlevel,
        final boolean doStart, final boolean refreshPackages )
    {

        Thread t = new InstallHelper( this, "Background Install " + bundleFile, bundleFile, refreshPackages )
        {

            protected Bundle doRun( InputStream bundleStream ) throws BundleException
            {
                Bundle bundle = getBundleContext().installBundle( location, bundleStream );

                if ( startlevel > 0 )
                {
                    StartLevel sl = getStartLevel();
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
        };

        t.start();
    }


    private void updateBackground( final Bundle bundle, final File bundleFile, final boolean refreshPackages )
    {
        Thread t = new InstallHelper( this, "Background Update" + bundle.getSymbolicName() + " ("
            + bundle.getBundleId() + ")", bundleFile, refreshPackages )
        {

            protected Bundle doRun( InputStream bundleStream ) throws BundleException
            {
                bundle.update( bundleStream );
                return bundle;
            }
        };

        t.start();
    }

    private static abstract class InstallHelper extends Thread
    {

        private final InstallAction installAction;

        private final File bundleFile;

        private final boolean refreshPackages;


        InstallHelper( InstallAction installAction, String name, File bundleFile, boolean refreshPackages )
        {
            super( name );
            setDaemon( true );

            this.installAction = installAction;
            this.bundleFile = bundleFile;
            this.refreshPackages = refreshPackages;
        }


        protected abstract Bundle doRun( InputStream bundleStream ) throws BundleException;


        public void run()
        {
            // wait some time for the request to settle
            sleepSilently( 500L );

            // now deploy the resolved bundles
            InputStream bundleStream = null;
            try
            {
                // we need the package admin before we call the bundle
                // installation or update, since we might be updating
                // our selves in which case the bundle context will be
                // invalid by the time we want to call the update
                PackageAdmin pa = ( refreshPackages ) ? installAction.getPackageAdmin() : null;

                bundleStream = new FileInputStream( bundleFile );
                Bundle bundle = doRun( bundleStream );

                if ( pa != null )
                {
                    // wait for asynchronous bundle start tasks to finish
                    sleepSilently( 2000L );

                    pa.refreshPackages( new Bundle[]
                        { bundle } );
                }
            }
            catch ( IOException ioe )
            {
                installAction.getLog().log( LogService.LOG_ERROR, "Cannot install or update bundle from " + bundleFile,
                    ioe );
            }
            catch ( BundleException be )
            {
                installAction.getLog().log( LogService.LOG_ERROR, "Cannot install or update bundle from " + bundleFile,
                    be );
            }
            finally
            {
                if ( bundleStream != null )
                {
                    try
                    {
                        bundleStream.close();
                    }
                    catch ( IOException ignore )
                    {
                    }
                }
                bundleFile.delete();
            }
        }


        protected void sleepSilently( long msecs )
        {
            try
            {
                sleep( msecs );
            }
            catch ( InterruptedException ie )
            {
                // don't care
            }
        }
    }
}
