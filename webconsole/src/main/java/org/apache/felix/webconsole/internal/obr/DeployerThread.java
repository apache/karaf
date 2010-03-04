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
package org.apache.felix.webconsole.internal.obr;


import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.webconsole.internal.Logger;
import org.osgi.service.log.LogService;


public class DeployerThread extends Thread
{

    private final Resolver obrResolver;

    private final Logger logger;

    private final boolean startBundles;

    private final boolean optionalDependencies;


    public DeployerThread( Resolver obrResolver, Logger logger, boolean startBundles, boolean optionalDependencies )
    {
        this( obrResolver, logger, startBundles, optionalDependencies, "OBR Bundle Deployer" );
    }


    public DeployerThread( Resolver obrResolver, Logger logger, boolean startBundles, boolean optionalDependencies, String name )
    {
        super( name );
        this.obrResolver = obrResolver;
        this.logger = logger;
        this.startBundles = startBundles;
        this.optionalDependencies = optionalDependencies;
    }


    public void run()
    {
        int flags = 0;
        flags += (startBundles ? Resolver.START : 0);
        flags += (optionalDependencies ? 0 : Resolver.NO_OPTIONAL_RESOURCES);
        try
        {
            if ( obrResolver.resolve( flags ) )
            {

                logResource( logger, "Installing Requested Resources", obrResolver.getAddedResources() );
                logResource( logger, "Installing Required Resources", obrResolver.getRequiredResources() );
                logResource( logger, "Installing Optional Resources", obrResolver.getOptionalResources() );

                obrResolver.deploy( flags );
            }
            else
            {
                logRequirements( logger, "Cannot Install requested bundles due to unsatisfied requirements",
                    obrResolver.getUnsatisfiedRequirements() );
            }
        }
        catch ( Exception ie )
        {
            logger.log( LogService.LOG_ERROR, "Cannot install bundles", ie );
        }
    }


    public static void logResource( Logger logger, String message, Resource[] res )
    {
        if ( res != null && res.length > 0 )
        {
            logger.log( LogService.LOG_INFO, message );
            for ( int i = 0; i < res.length; i++ )
            {
                logger.log( LogService.LOG_INFO, "  " + i + ": " + res[i].getSymbolicName() + ", "
                    + res[i].getVersion() );
            }
        }
    }


    public static void logRequirements( Logger logger, String message, Reason[] reasons )
    {
        logger.log( LogService.LOG_ERROR, message );
        for ( int i = 0; reasons != null && i < reasons.length; i++ )
        {
            String moreInfo = reasons[i].getRequirement().getComment();
            if ( moreInfo == null )
            {
                moreInfo = reasons[i].getRequirement().getFilter().toString();
            }
            logger.log( LogService.LOG_ERROR, "  " + i + ": " + reasons[i].getRequirement().getName() + " (" + moreInfo + ")" );
        }
    }

}
