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


import java.io.IOException;
import java.util.Hashtable;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;


/**
 * The <code>ConfigureThread</code> class is extends the {@link TestThread} for
 * use as the configuration creator and updater in the
 * {@link org.apache.felix.cm.integration.ConfigUpdateStressTest}.
 */
public class ConfigureThread extends TestThread
{
    private final Configuration config;

    private final Hashtable<String, Object> props;


    public ConfigureThread( final ConfigurationAdmin configAdmin, final String pid, final boolean isFactory )
        throws IOException
    {
        // ensure configuration and disown it
        final Configuration config;
        if ( isFactory )
        {
            config = configAdmin.createFactoryConfiguration( pid );
        }
        else
        {
            config = configAdmin.getConfiguration( pid );
        }
        config.setBundleLocation( null );

        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put( "prop1", "aValue" );
        props.put( "prop2", 4711 );

        this.config = config;
        this.props = props;
    }


    @Override
    public void doRun()
    {
        try
        {
            config.update( props );
        }
        catch ( IOException ioe )
        {
            // TODO: log !!
        }
    }


    @Override
    public void cleanup()
    {
        try
        {
            config.delete();
        }
        catch ( IOException ioe )
        {
            // TODO: log !!
        }
    }
}