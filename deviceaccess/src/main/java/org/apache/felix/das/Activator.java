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
package org.apache.felix.das;


import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import org.osgi.service.device.Device;
import org.osgi.service.device.Driver;
import org.osgi.service.device.DriverLocator;
import org.osgi.service.device.DriverSelector;
import org.osgi.service.log.LogService;

import org.apache.felix.dependencymanager.DependencyActivatorBase;
import org.apache.felix.dependencymanager.DependencyManager;
import org.apache.felix.dependencymanager.Service;

import org.apache.felix.das.util.Util;


/**
 * TODO: add javadoc
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator extends DependencyActivatorBase
{

    /** the bundle context */
    private BundleContext m_context;

    /** the dependency manager */
    private DependencyManager m_manager;

    /** the device manager */
    private DeviceManager m_deviceManager;


    /**
     * Here, we create and start the device manager, but we do not register it
     * within the framework, since that is not specified by the specification.
     * 
     * @see org.apache.felix.dependencymanager.DependencyActivatorBase#init(org.osgi.framework.BundleContext,
     *      org.apache.felix.dependencymanager.DependencyManager)
     */
    public void init( BundleContext context, DependencyManager manager ) throws Exception
    {

        m_context = context;
        m_manager = manager;

        m_deviceManager = new DeviceManager( m_context );

        // the real device manager
        startDeviceManager();

        // the analyzers to inform the user (and me) if something is wrong
        // startAnalyzers();

    }


    private void startDeviceManager()
    {

        final String driverFilter = Util.createFilterString( "(&(%s=%s)(%s=%s))", new String[]
            { Constants.OBJECTCLASS, Driver.class.getName(), org.osgi.service.device.Constants.DRIVER_ID, "*" } );

        final String deviceFilter = Util.createFilterString( "(|(%s=%s)(%s=%s))", new String[]
            { Constants.OBJECTCLASS, Device.class.getName(), org.osgi.service.device.Constants.DEVICE_CATEGORY, "*" } );

        Service svc = createService();

        svc.setImplementation( m_deviceManager );

        svc.add( createServiceDependency().setService( LogService.class ).setRequired( false ) );

        svc.add( createServiceDependency().setService( DriverSelector.class ).setRequired( false )
            .setAutoConfig( false ) );

        svc.add( createServiceDependency().setService( DriverLocator.class ).setRequired( false ).setAutoConfig( false )
            .setCallbacks( "locatorAdded", "locatorRemoved" ) );
        svc.add( createServiceDependency().setService( Driver.class, driverFilter ).setRequired( false ).setCallbacks(
            "driverAdded", "driverRemoved" ) );
        svc.add( createServiceDependency().setService( Device.class, deviceFilter ).setRequired( false ).setCallbacks(
            "deviceAdded", "deviceModified", "deviceRemoved" ) );

        m_manager.add( svc );

    }


    public void destroy( BundleContext context, DependencyManager manager ) throws Exception
    {
        // TODO Auto-generated method stub

    }

}
