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


import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.Action;
import org.apache.felix.webconsole.internal.BaseManagementPlugin;
import org.apache.felix.webconsole.internal.Logger;
import org.osgi.framework.*;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;


/**
 * The <code>InstallAction</code> TODO
 */
public class InstallAction extends BaseManagementPlugin implements Action
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
    throws IOException
    {

        // get the uploaded data
        final Map params = ( Map ) request.getAttribute( AbstractWebConsolePlugin.ATTR_FILEUPLOAD );
        if ( params == null )
        {
            return true;
        }

        final FileItem startItem = getParameter( params, FIELD_START );
        final FileItem startLevelItem = getParameter( params, FIELD_STARTLEVEL );
        final FileItem[] bundleItems = getFileItems( params, FIELD_BUNDLEFILE );
        final FileItem refreshPackagesItem = getParameter( params, FIELD_REFRESH_PACKAGES );

        // don't care any more if not bundle item
        if ( bundleItems.length == 0 )
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

        for(int i = 0; i < bundleItems.length; i++ )
        {
            final FileItem bundleItem = bundleItems[i];
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
                getLog().log( LogService.LOG_ERROR, "Problem accessing uploaded bundle file: " + bundleItem.getName(), e );

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
        }

        return true;
    }


    private FileItem getParameter( Map params, String name )
    {
        FileItem[] items = ( FileItem[] ) params.get( name );
        if ( items != null )
        {
            for ( int i = 0; i < items.length; i++ )
            {
                if ( items[i].isFormField() )
                {
                    return items[i];
                }
            }
        }

        // nothing found, fail
        return null;
    }

    private FileItem[] getFileItems( Map params, String name )
    {
        final List files = new ArrayList();
        FileItem[] items = ( FileItem[] ) params.get( name );
        if ( items != null )
        {
            for ( int i = 0; i < items.length; i++ )
            {
                if ( !items[i].isFormField() && items[i].getSize() > 0 )
                {
                    files.add(items[i]);
                }
            }
        }

        return (FileItem[])files.toArray(new FileItem[files.size()]);
    }

    private void installBundle( String location, File bundleFile, int startLevel, boolean start, boolean refreshPackages )
    throws IOException
    {
        if ( bundleFile != null )
        {

            // try to get the bundle name, fail if none
            String symbolicName = getSymbolicName( bundleFile );
            if ( symbolicName == null )
            {
                bundleFile.delete();
                throw new IOException(Constants.BUNDLE_SYMBOLICNAME + " header missing, cannot install bundle");
            }

            // check for existing bundle first
            Bundle updateBundle = null;
            if ( Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals( symbolicName ) )
            {
                updateBundle = getBundleContext().getBundle( 0 );
            }
            else
            {
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

        Thread t = new InstallHelper( getBundleContext(), bundleFile, location, startlevel, doStart, refreshPackages )
        {
            protected Logger getLog()
            {
                return InstallAction.this.getLog();
            }


            protected Object getService( String serviceName )
            {
                if ( serviceName.equals( PackageAdmin.class.getName() ) )
                {
                    return InstallAction.this.getPackageAdmin();
                }
                else if ( serviceName.equals( StartLevel.class.getName() ) )
                {
                    return InstallAction.this.getStartLevel();
                }

                return null;
            }
        };

        t.start();
    }


    private void updateBackground( final Bundle bundle, final File bundleFile, final boolean refreshPackages )
    {
        Thread t = new UpdateHelper( bundle, bundleFile, refreshPackages )
        {
            protected Logger getLog()
            {
                return InstallAction.this.getLog();
            }


            protected Object getService( String serviceName )
            {
                if ( serviceName.equals( PackageAdmin.class.getName() ) )
                {
                    return InstallAction.this.getPackageAdmin();
                }

                return null;
            }
        };

        t.start();
    }
}
