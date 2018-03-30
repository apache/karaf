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
package org.apache.karaf.webconsole.http;

import javax.servlet.Servlet;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.karaf.http.core.ProxyService;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.ops4j.pax.web.service.spi.WebListener;

@Services(requires = @RequireService(ProxyService.class))
public class Activator extends BaseActivator {

    private HttpPlugin httpPlugin;
    private ServletEventHandler eaHandler;
    private WebEventHandler webEaHandler;

    @Override
    protected void doStart() throws Exception {
        ProxyService proxyService = getTrackedService(ProxyService.class);
        if (proxyService == null) {
            return;
        }

        eaHandler = new ServletEventHandler();
        eaHandler.setBundleContext(bundleContext);
        eaHandler.init();
        register(ServletListener.class, eaHandler);

        webEaHandler = new WebEventHandler();
        webEaHandler.setBundleContext(bundleContext);
        webEaHandler.init();
        register(WebListener.class, webEaHandler);

        httpPlugin = new HttpPlugin();
        httpPlugin.setBundleContext(bundleContext);
        httpPlugin.setServletEventHandler(eaHandler);
        httpPlugin.setWebEventHandler(webEaHandler);
        httpPlugin.setProxyService(proxyService);
        httpPlugin.start();

        Dictionary<String, String> props = new Hashtable<>();
        props.put("felix.webconsole.label", "http");
        register(Servlet.class, httpPlugin, props);
    }

    @Override
    protected void doStop() {
        super.doStop();
        if (httpPlugin != null) {
            httpPlugin.stop();
            httpPlugin = null;
        }
        if (eaHandler != null) {
            eaHandler.destroy();
            eaHandler = null;
        }
        if (webEaHandler != null) {
            webEaHandler.destroy();
            webEaHandler = null;
        }
    }

}
