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
package org.apache.felix.scr.impl;


import org.apache.felix.scr.Component;
import org.apache.felix.scr.impl.manager.AbstractComponentManager;
import org.osgi.service.log.LogService;


/**
 * The <code>ComponentActivatorTask</code> extends the <code>Runnable</code>
 * interface with the functionality to have a meaningful {@link #toString()}
 * implementation. This is mainly used when logging something around the task
 * being run or scheduled.
 */
public abstract class ComponentActivatorTask implements Runnable
{

    private final String taskName;
    private final AbstractComponentManager component;


    protected ComponentActivatorTask( String taskName, AbstractComponentManager component )
    {
        this.taskName = taskName;
        this.component = component;
    }


    protected abstract void doRun();


    public void run()
    {
        // fail, if the bundle is not active
        if ( component.getState() == Component.STATE_DISPOSED )
        {
            // cannot use bundle to log because it is not accessible from the
            // component if the component is destroyed
            Activator.log( LogService.LOG_WARNING, null, "Cannot run task '" + this
                + "': Component has already been disposed", null );
        }
        else if ( !ComponentRegistry.isBundleActive( component.getBundle() ) )
        {
            Activator.log( LogService.LOG_WARNING, component.getBundle(), "Cannot run task '" + this
                + "': Declaring bundle is not active", null );
        }
        else
        {
            doRun();
        }
    }


    public String toString()
    {
        return taskName + " " + component;
    }
}
