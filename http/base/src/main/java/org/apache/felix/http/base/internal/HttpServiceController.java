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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.apache.felix.http.base.internal.handler.HandlerRegistry;
import org.apache.felix.http.base.internal.dispatch.Dispatcher;
import org.apache.felix.http.base.internal.service.HttpServiceFactory;
import org.apache.felix.http.api.ExtHttpService;
import javax.servlet.ServletContext;
import java.util.Hashtable;

public final class HttpServiceController
{
    private final BundleContext bundleContext;
    private final HandlerRegistry registry;
    private final Dispatcher dispatcher;
    private final Hashtable<String, Object> serviceProps;
    private ServiceRegistration serviceReg;

    public HttpServiceController(BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;
        this.registry = new HandlerRegistry();
        this.dispatcher = new Dispatcher(this.registry);
        this.serviceProps = new Hashtable<String, Object>();
    }

    public Dispatcher getDispatcher()
    {
        return this.dispatcher;
    }

    public void setProperties(Hashtable<String, Object> props)
    {
        this.serviceProps.clear();
        this.serviceProps.putAll(props);

        if (this.serviceReg != null) {
            this.serviceReg.setProperties(this.serviceProps);
        }
    }

    public void register(ServletContext servletContext)
    {
        HttpServiceFactory factory = new HttpServiceFactory(servletContext, this.registry);
        String[] ifaces = new String[] { HttpService.class.getName(), ExtHttpService.class.getName() };
        this.serviceReg = this.bundleContext.registerService(ifaces, factory, this.serviceProps);
    }

    public void unregister()
    {
        if (this.serviceReg == null) {
            return;
        }

        try {
            this.serviceReg.unregister();
            this.registry.removeAll();
        } finally {
            this.serviceReg = null;
        }
    }
}
