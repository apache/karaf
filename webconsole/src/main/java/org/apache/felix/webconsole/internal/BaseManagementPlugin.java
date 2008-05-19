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


import org.apache.felix.webconsole.internal.servlet.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;


public class BaseManagementPlugin
{

    private BundleContext bundleContext;
    private Logger log;

    private ServiceTracker startLevelService;

    private ServiceTracker packageAdmin;


    protected BaseManagementPlugin()
    {
    }


    public void setBundleContext( BundleContext bundleContext )
    {
        this.bundleContext = bundleContext;
    }


    public void setLogger( Logger log )
    {
        this.log = log;
    }


    protected BundleContext getBundleContext()
    {
        return bundleContext;
    }


    protected Logger getLog()
    {
        return log;
    }


    protected StartLevel getStartLevel()
    {
        if ( startLevelService == null )
        {
            startLevelService = new ServiceTracker( getBundleContext(), StartLevel.class.getName(), null );
            startLevelService.open();
        }
        return ( StartLevel ) startLevelService.getService();
    }


    protected PackageAdmin getPackageAdmin()
    {
        if ( packageAdmin == null )
        {
            packageAdmin = new ServiceTracker( getBundleContext(), PackageAdmin.class.getName(), null );
            packageAdmin.open();
        }
        return ( PackageAdmin ) packageAdmin.getService();
    }

}
