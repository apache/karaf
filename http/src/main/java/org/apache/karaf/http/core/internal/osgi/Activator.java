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
package org.apache.karaf.http.core.internal.osgi;

import org.apache.karaf.http.core.ProxyService;
import org.apache.karaf.http.core.ServletService;
import org.apache.karaf.http.core.internal.HttpMBeanImpl;
import org.apache.karaf.http.core.internal.ProxyServiceImpl;
import org.apache.karaf.http.core.internal.ServletEventHandler;
import org.apache.karaf.http.core.internal.ServletServiceImpl;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.http.HttpService;

@Services(
        requires = {
                @RequireService(HttpService.class)
        },
        provides = {
                @ProvideService(ServletService.class),
                @ProvideService(ProxyService.class)
        }
)
public class Activator extends BaseActivator {

    private BundleListener listener;

    @Override
    protected void doStart() throws Exception {
        HttpService httpService = getTrackedService(HttpService.class);
        if (httpService == null) {
            return;
        }

        final ServletEventHandler servletEventHandler = new ServletEventHandler();
        register(ServletListener.class, servletEventHandler);

        ServletServiceImpl servletService = new ServletServiceImpl(servletEventHandler);
        register(ServletService.class, servletService);

        listener = event -> {
            if (event.getType() == BundleEvent.UNINSTALLED
                    || event.getType() == BundleEvent.UNRESOLVED
                    || event.getType() == BundleEvent.STOPPED) {
                servletEventHandler.removeEventsForBundle(event.getBundle());
            }
        };
        bundleContext.addBundleListener(listener);

        ProxyServiceImpl proxyService = new ProxyServiceImpl(httpService);
        register(ProxyService.class, proxyService);

        HttpMBeanImpl httpMBean = new HttpMBeanImpl(servletService, proxyService);
        registerMBean(httpMBean, "type=http");
    }

    @Override
    protected void doStop() {
        if (listener != null) {
            bundleContext.removeBundleListener(listener);
            listener = null;
        }
        super.doStop();
    }
}
