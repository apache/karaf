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
import java.io.InputStream;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.internal.obr.DeployerThread;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.log.LogService;


class UpdateHelper extends BaseUpdateInstallHelper
{

    private final Bundle bundle;


    UpdateHelper( final SimpleWebConsolePlugin plugin, final Bundle bundle, boolean refreshPackages )
    {
        this( plugin, bundle, null, refreshPackages );
    }


    UpdateHelper( final SimpleWebConsolePlugin plugin, final Bundle bundle, final File bundleFile,
        boolean refreshPackages )
    {
        super( plugin, "Background Update " + bundle.getSymbolicName() + " (" + bundle.getBundleId() + ")",
            bundleFile, refreshPackages );
        this.bundle = bundle;
    }


    protected Bundle doRun( final InputStream bundleStream ) throws BundleException
    {
        bundle.update( bundleStream );
        return bundle;
    }


    protected Bundle doRun() throws Exception
    {
        // update the bundle from the file if defined
        if ( getBundleFile() != null )
        {
            return super.doRun();
        }

        // try updating from the bundle location
        if ( updateFromBundleLocation() )
        {
            return bundle;
        }

        // ensure we have a symbolic name for the OBR update to follow
        if ( bundle.getSymbolicName() == null )
        {
            throw new BundleException( "Cannot update bundle: Symbolic Name is required for OBR update" );
        }

        // try updating from OBR
        if ( updateFromOBR() )
        {
            return bundle;
        }

        // bundle was not updated, return nothing
        return null;
    }


    private boolean updateFromBundleLocation()
    {
        getLog().log( LogService.LOG_DEBUG, "Trying to update with Bundle.update()" );

        try
        {
            bundle.update();
            getLog().log( LogService.LOG_INFO, "Bundle updated from bundle provided (update) location" );
            return true;
        }
        catch ( Throwable ioe )
        {
            // BundleException, IllegalStateException or SecurityException? lets use OBR then
            getLog().log( LogService.LOG_DEBUG, "Update failure using Bundle.update()", ioe );
        }

        // not installed from the bundle location
        return false;
    }


    private boolean updateFromOBR() throws InvalidSyntaxException
    {
        RepositoryAdmin ra = ( RepositoryAdmin ) getService( RepositoryAdmin.class.getName() );
        if ( ra != null )
        {
            getLog().log( LogService.LOG_DEBUG, "Trying to update from OSGi Bundle Repository" );

            final Resolver resolver = ra.resolver();

            String version = ( String ) bundle.getHeaders().get( Constants.BUNDLE_VERSION );
            if ( version == null )
            {
                version = "0.0.0";
            }
            final String filter = "(&(symbolicname=" + bundle.getSymbolicName() + ")(!(version=" + version
                + "))(version>=" + version + "))";

            final Resource[] resources = ra.discoverResources( filter );
            final Resource resource = selectHighestVersion( resources );
            if ( resource != null )
            {
                resolver.add( resource );

                if ( !resolver.resolve() )
                {
                    DeployerThread.logRequirements( getLog(),
                        "Cannot updated bundle from OBR due to unsatisfied requirements", resolver
                            .getUnsatisfiedRequirements() );
                }
                else
                {
                    DeployerThread.logResource( getLog(), "Installing Requested Resources", resolver
                        .getAddedResources() );
                    DeployerThread.logResource( getLog(), "Installing Required Resources", resolver
                        .getRequiredResources() );
                    DeployerThread.logResource( getLog(), "Installing Optional Resources", resolver
                        .getOptionalResources() );

                    // deploy the resolved bundles and ensure they are started
                    resolver.deploy( Resolver.START );
                    getLog().log( LogService.LOG_INFO, "Bundle updated from OSGi Bundle Repository" );

                    return true;
                }
            }
            else
            {
                getLog().log( LogService.LOG_INFO,
                    "Nothing to update, OSGi Bundle Repository does not provide more recent version" );
            }
        }
        else
        {
            getLog().log( LogService.LOG_DEBUG, "Cannot updated from OSGi Bundle Repository: Service not available" );
        }

        // fallback to false, nothing done
        return false;
    }


    private Resource selectHighestVersion( final Resource[] candidates )
    {
        if ( candidates == null || candidates.length == 0 )
        {
            // nothing to do if there are none
            return null;
        }
        else if ( candidates.length == 1 )
        {
            // simple choice if there is a single one
            return candidates[0];
        }

        // now go on looking for the highest version
        Resource best = candidates[0];
        for ( int i = 1; i < candidates.length; i++ )
        {
            if ( best.getVersion().compareTo( candidates[i].getVersion() ) < 0)
            {
                best = candidates[i];
            }
        }
        return best;
    }
}
