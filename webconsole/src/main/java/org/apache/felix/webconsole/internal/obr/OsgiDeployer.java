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


import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.osgi.service.log.LogService;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;


public class OsgiDeployer implements Runnable
{

    private final Resolver obrResolver;

    private final AbstractWebConsolePlugin logger;

    private final boolean startBundles;


    static void deploy( Resolver obrResolver, AbstractWebConsolePlugin logger, boolean startBundles )
    {
        final OsgiDeployer d = new OsgiDeployer( obrResolver, logger, startBundles );
        final Thread t = new Thread( d, "OBR Bundle Deployer (OSGi API)" );
        t.start();
    }


    public OsgiDeployer( Resolver obrResolver, AbstractWebConsolePlugin logger, boolean startBundles )
    {
        this.obrResolver = obrResolver;
        this.logger = logger;
        this.startBundles = startBundles;
    }


    public void run()
    {
        try
        {
            if ( obrResolver.resolve() )
            {

                logResource( logger, "Installing Requested Resources", obrResolver.getAddedResources() );
                logResource( logger, "Installing Required Resources", obrResolver.getRequiredResources() );
                logResource( logger, "Installing Optional Resources", obrResolver.getOptionalResources() );

                obrResolver.deploy( startBundles );
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


    public static void logResource( AbstractWebConsolePlugin logger, String message, Resource[] res )
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


    public static void logRequirements( AbstractWebConsolePlugin logger, String message, Requirement[] reasons )
    {
        logger.log( LogService.LOG_ERROR, message );
        for ( int i = 0; reasons != null && i < reasons.length; i++ )
        {
            String moreInfo = reasons[i].getComment();
            if ( moreInfo == null )
            {
                moreInfo = reasons[i].getFilter().toString();
            }
            logger.log( LogService.LOG_ERROR, "  " + i + ": " + reasons[i].getName() + " (" + moreInfo + ")" );
        }
    }

}
