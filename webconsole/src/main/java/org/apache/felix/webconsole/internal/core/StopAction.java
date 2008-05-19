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
package org.apache.felix.webconsole.internal.core;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.log.LogService;


/**
 * The <code>StopAction</code> TODO
 */
public class StopAction extends BundleAction
{

    public static final String NAME = "stop";
    public static final String LABEL = "Stop";


    public String getName()
    {
        return NAME;
    }


    public String getLabel()
    {
        return LABEL;
    }


    public boolean performAction( HttpServletRequest request, HttpServletResponse response )
    {

        long bundleId = this.getBundleId( request );
        if ( bundleId > 0 )
        { // cannot stop system bundle !!
            Bundle bundle = this.getBundleContext().getBundle( bundleId );
            if ( bundle != null )
            {
                try
                {
                    bundle.stop();
                }
                catch ( BundleException be )
                {
                    getLog().log( LogService.LOG_ERROR, "Cannot stop", be );
                }

            }
        }

        return true;
    }
}
