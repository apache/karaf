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
package org.apache.felix.webconsole.internal.core;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.felix.webconsole.internal.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;


abstract class BaseUpdateInstallHelper extends Thread
{

    private final File bundleFile;

    private final boolean refreshPackages;


    BaseUpdateInstallHelper( String name, File bundleFile, boolean refreshPackages )
    {
        super( name );
        setDaemon( true );

        this.bundleFile = bundleFile;
        this.refreshPackages = refreshPackages;
    }


    protected File getBundleFile()
    {
        return bundleFile;
    }


    protected abstract Bundle doRun( InputStream bundleStream ) throws BundleException;


    protected abstract Logger getLog();


    protected abstract Object getService( String serviceName );


    /**
     * @return the installed bundle or <code>null</code> if no bundle was touched
     * @throws BundleException
     * @throws IOException
     */
    protected Bundle doRun() throws BundleException, IOException
    {
        // now deploy the resolved bundles
        InputStream bundleStream = null;
        try
        {
            bundleStream = new FileInputStream( bundleFile );
            return doRun( bundleStream );
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
        }

    }


    public void run()
    {
        // wait some time for the request to settle
        sleepSilently( 500L );

        // now deploy the resolved bundles
        try
        {
            // we need the package admin before we call the bundle
            // installation or update, since we might be updating
            // our selves in which case the bundle context will be
            // invalid by the time we want to call the update
            PackageAdmin pa = ( refreshPackages ) ? ( PackageAdmin ) getService( PackageAdmin.class.getName() ) : null;

            Bundle bundle = doRun();

            if ( pa != null && bundle != null )
            {
                // wait for asynchronous bundle start tasks to finish
                sleepSilently( 2000L );

                pa.refreshPackages( new Bundle[]
                    { bundle } );
            }
        }
        catch ( IOException ioe )
        {
            getLog().log( LogService.LOG_ERROR, "Cannot install or update bundle from " + bundleFile, ioe );
        }
        catch ( BundleException be )
        {
            getLog().log( LogService.LOG_ERROR, "Cannot install or update bundle from " + bundleFile, be );
        }
        finally
        {
            if ( bundleFile != null )
            {
                bundleFile.delete();
            }
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