/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.webconsole.internal.system;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.Action;
import org.apache.felix.webconsole.internal.BaseManagementPlugin;
import org.osgi.framework.BundleException;
import org.osgi.service.log.LogService;


public class ShutdownAction extends BaseManagementPlugin implements Action
{

    public static final String NAME = "shutdown";


    public String getName()
    {
        return NAME;
    }


    public String getLabel()
    {
        return NAME;
    }


    public boolean performAction( HttpServletRequest request, HttpServletResponse response )
    {
        // simply terminate VM in case of shutdown :-)
        Thread t = new Thread( "Stopper" )
        {
            public void run()
            {
                try
                {
                    Thread.sleep( 2000L );
                }
                catch ( InterruptedException ie )
                {
                    // ignore
                }

                getLog().log( LogService.LOG_INFO, "Shutting down server now!" );

                // stopping bundle 0 (system bundle) stops the framework
                try
                {
                    getBundleContext().getBundle( 0 ).stop();
                }
                catch ( BundleException be )
                {
                    getLog().log( LogService.LOG_ERROR, "Problem stopping Framework", be );
                }
            }
        };
        t.start();

        return true;
    }

}
