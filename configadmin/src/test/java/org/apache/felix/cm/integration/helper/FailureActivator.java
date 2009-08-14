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
package org.apache.felix.cm.integration.helper;


import java.util.Dictionary;
import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.util.tracker.ServiceTracker;


public class FailureActivator implements BundleActivator
{
    private static final String PID = "myPID";

    private ServiceTracker tracker;

    private ServiceRegistration factory;

    private static String servicePid;

    private ManagedServiceFactory m_factory = new ManagedServiceFactory()
    {
        private final Object m_lock = new Object();
        private Exception e;


        public void deleted( String pid )
        {
            System.out.println( "Deleted " + pid );
        }


        public String getName()
        {
            return "A testing factory";
        }


        public void updated( String pid, Dictionary dict ) throws ConfigurationException
        {
            synchronized ( m_lock )
            {
                System.out.println( this + " Updated " + pid + " with " + dict );
                if ( e == null )
                {
                    e = new Exception(pid);
                }
                else
                {
                    System.out.println( "******************************" );
                    System.out.println( "******************************" );
                    System.out.println( "Error: updated more than once." );
                    System.out.println( "first:" );
                    e.printStackTrace( System.out );
                    System.out.println( "******************************" );
                    System.out.println( "second:" );
                    new Exception( pid ).printStackTrace( System.out );
                    System.out.println( "******************************" );
                    System.out.println( "******************************" );
                }
            }
        }
    };


    public void start( final BundleContext context )
    {
        // Register our service factory
        Properties props = new Properties();
        props.put( Constants.SERVICE_PID, PID );
        factory = context.registerService( ManagedServiceFactory.class.getName(), m_factory, props );

        // Create a new thread to update the config admin (this way the
        // activator can return)
        new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    FailureActivator.this.tracker = new ServiceTracker( context, context.createFilter( "(" + Constants.OBJECTCLASS
                        + "=" + ConfigurationAdmin.class.getName() + ")" ), null );
                    tracker.open();
                    ConfigurationAdmin configAdmin = ( ConfigurationAdmin ) tracker.waitForService( 5000 );
                    Configuration config = configAdmin.createFactoryConfiguration( PID, null );
                    servicePid = config.getPid();
                    Properties serviceProps = new Properties();
                    serviceProps.put( "key", "value" );
                    config.update( serviceProps );
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    public void stop( BundleContext arg0 ) throws Exception
    {
        factory.unregister();

        // remove the configuration again to clean up
        ConfigurationAdmin configAdmin = ( ConfigurationAdmin ) tracker.waitForService( 5000 );
        Configuration config = configAdmin.getConfiguration( servicePid, null );
        config.delete();

        tracker.close();
    }
}
