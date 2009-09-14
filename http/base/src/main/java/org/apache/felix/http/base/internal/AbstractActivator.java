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
package org.apache.felix.http.base.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.apache.felix.http.base.internal.util.SystemLogger;

public abstract class AbstractActivator
    implements BundleActivator
{
    private BundleContext context;
    private DispatcherServlet dispatcher;
    private HttpServiceController controller;

    protected final BundleContext getBundleContext()
    {
        return this.context;
    }

    protected final DispatcherServlet getDispatcherServlet()
    {
        return this.dispatcher;
    }

    protected final HttpServiceController getHttpServiceController()
    {
        return this.controller;
    }

    public final void start(BundleContext context)
        throws Exception
    {
        this.context = context;
        this.controller = new HttpServiceController(this.context);
        this.dispatcher = new DispatcherServlet(this.controller);

        SystemLogger.get().open(context);
        doStart();
    }

    public final void stop(BundleContext context)
        throws Exception
    {
        doStop();

        this.controller.unregister();
        this.dispatcher.destroy();
        SystemLogger.get().close();
    }

    protected abstract void doStart()
        throws Exception;

    protected abstract void doStop()
        throws Exception;   
}
