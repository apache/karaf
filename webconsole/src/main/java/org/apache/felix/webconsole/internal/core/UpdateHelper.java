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

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogService;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;


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

        // try updating from Apache Felix OBR
        if ( updateFromFelixOBR() )
        {
            return bundle;
        }

        // try updating from OSGi OBR
        if ( updateFromOsgiOBR() )
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


    private boolean updateFromFelixOBR()
    {
        org.apache.felix.bundlerepository.RepositoryAdmin ra = ( org.apache.felix.bundlerepository.RepositoryAdmin ) getService( "org.apache.felix.bundlerepository.RepositoryAdmin" );
        if ( ra != null )
        {
            getLog().log( LogService.LOG_DEBUG, "Trying to update from OSGi Bundle Repository (Apache Felix API)" );

            final org.apache.felix.bundlerepository.Resolver resolver = ra.resolver();

            String version = ( String ) bundle.getHeaders().get( Constants.BUNDLE_VERSION );
            if ( version == null )
            {
                version = "0.0.0";
            }
            final String filter = "(&(symbolicname=" + bundle.getSymbolicName() + ")(!(version=" + version
                + "))(version>=" + version + "))";
            final org.apache.felix.bundlerepository.Requirement req = ra.getHelper().requirement(
                bundle.getSymbolicName(), filter );
            final org.apache.felix.bundlerepository.Resource[] resources = ra
                .discoverResources( new org.apache.felix.bundlerepository.Requirement[]
                    { req } );
            final org.apache.felix.bundlerepository.Resource resource = selectHighestVersion( resources );
            if ( resource != null )
            {
                resolver.add( resource );

                if ( !resolver.resolve() )
                {
                    logRequirements( "Cannot updated bundle from OBR due to unsatisfied requirements", resolver
                        .getUnsatisfiedRequirements() );
                }
                else
                {
                    logResource( "Installing Requested Resources", resolver.getAddedResources() );
                    logResource( "Installing Required Resources", resolver.getRequiredResources() );
                    logResource( "Installing Optional Resources", resolver.getOptionalResources() );

                    // deploy the resolved bundles and ensure they are started
                    resolver.deploy( org.apache.felix.bundlerepository.Resolver.START );
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

    private boolean updateFromOsgiOBR()
    {
        RepositoryAdmin ra = ( RepositoryAdmin ) getService( "org.osgi.service.obr.RepositoryAdmin" );
        if ( ra != null )
        {
            getLog().log( LogService.LOG_DEBUG, "Trying to update from OSGi Bundle Repository (OSGi API)" );

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
                    logRequirements( "Cannot updated bundle from OBR due to unsatisfied requirements", resolver
                        .getUnsatisfiedRequirements() );
                }
                else
                {
                    logResource( "Installing Requested Resources", resolver.getAddedResources() );
                    logResource( "Installing Required Resources", resolver.getRequiredResources() );
                    logResource( "Installing Optional Resources", resolver.getOptionalResources() );

                    // deploy the resolved bundles and ensure they are started
                    resolver.deploy( true );
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


    //---------- Apache Felix OBR API helper

    private org.apache.felix.bundlerepository.Resource selectHighestVersion(
        final org.apache.felix.bundlerepository.Resource[] candidates )
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
        org.apache.felix.bundlerepository.Resource best = candidates[0];
        for ( int i = 1; i < candidates.length; i++ )
        {
            if ( best.getVersion().compareTo( candidates[i].getVersion() ) < 0 )
            {
                best = candidates[i];
            }
        }
        return best;
    }


    private void logResource( String message, org.apache.felix.bundlerepository.Resource[] res )
    {
        if ( res != null && res.length > 0 )
        {
            getLog().log( LogService.LOG_INFO, message );
            for ( int i = 0; i < res.length; i++ )
            {
                getLog().log( LogService.LOG_INFO,
                    "  " + i + ": " + res[i].getSymbolicName() + ", " + res[i].getVersion() );
            }
        }
    }


    private void logRequirements( String message, Reason[] reasons )
    {
        getLog().log( LogService.LOG_ERROR, message );
        for ( int i = 0; reasons != null && i < reasons.length; i++ )
        {
            String moreInfo = reasons[i].getRequirement().getComment();
            if ( moreInfo == null )
            {
                moreInfo = reasons[i].getRequirement().getFilter().toString();
            }
            getLog().log( LogService.LOG_ERROR,
                "  " + i + ": " + reasons[i].getRequirement().getName() + " (" + moreInfo + ")" );
        }
    }


    //---------- OSGi OBR API helper

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


    private void logResource( String message, Resource[] res )
    {
        if ( res != null && res.length > 0 )
        {
            getLog().log( LogService.LOG_INFO, message );
            for ( int i = 0; i < res.length; i++ )
            {
                getLog().log( LogService.LOG_INFO,
                    "  " + i + ": " + res[i].getSymbolicName() + ", " + res[i].getVersion() );
            }
        }
    }


    private void logRequirements( String message, Requirement[] reasons )
    {
        getLog().log( LogService.LOG_ERROR, message );
        for ( int i = 0; reasons != null && i < reasons.length; i++ )
        {
            String moreInfo = reasons[i].getComment();
            if ( moreInfo == null )
            {
                moreInfo = reasons[i].getFilter().toString();
            }
            getLog().log( LogService.LOG_ERROR, "  " + i + ": " + reasons[i].getName() + " (" + moreInfo + ")" );
        }
    }
}
