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
package org.apache.felix.webconsole.internal;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;


public abstract class BaseWebConsolePlugin extends AbstractWebConsolePlugin implements OsgiManagerPlugin
{

    private static String PACKAGE_ADMIN_NAME = PackageAdmin.class.getName();
    private static String START_LEVEL_NAME = StartLevel.class.getName();

    private Logger log;

    private Map services = new HashMap();


    public void deactivate()
    {
        for ( Iterator ti = services.values().iterator(); ti.hasNext(); )
        {
            ServiceTracker tracker = ( ServiceTracker ) ti.next();
            tracker.close();
            ti.remove();
        }

        if ( log != null )
        {
            log.dispose();
            log = null;
        }
    }


    protected Logger getLog()
    {
        if ( log == null )
        {
            log = new Logger( getBundleContext() );
        }

        return log;
    }


    protected StartLevel getStartLevel()
    {
        return ( StartLevel ) getService( START_LEVEL_NAME );
    }


    protected PackageAdmin getPackageAdmin()
    {
        return ( PackageAdmin ) getService( PACKAGE_ADMIN_NAME );
    }


    protected Object getService( String serviceName )
    {
        ServiceTracker serviceTracker = ( ServiceTracker ) services.get( serviceName );
        if ( serviceTracker == null )
        {
            serviceTracker = new ServiceTracker( getBundleContext(), serviceName, null );
            serviceTracker.open();

            services.put( serviceName, serviceTracker );
        }

        return serviceTracker.getService();
    }
}
