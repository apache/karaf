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
package org.apache.karaf.tooling.exam.regression.supports;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServletActivator implements BundleActivator {

    private static final transient Logger LOG = LoggerFactory.getLogger(ServletActivator.class);

    private HttpServiceTracker tracker;

    @Override
    public void start(BundleContext bc) throws Exception {
        tracker = new HttpServiceTracker(bc);
        tracker.open();
    }

    @Override
    public void stop(BundleContext bc) throws Exception {
        tracker.close();
    }

    class HttpServiceTracker extends ServiceTracker<HttpService, HttpService> {

        private static final String ALIAS = "/test/services";

        public HttpServiceTracker(BundleContext context) {
            super(context, HttpService.class, null);
        }

        @Override
        public HttpService addingService(ServiceReference<HttpService> reference) {
            HttpService httpService = context.getService(reference);
            final HttpContext httpContext = httpService.createDefaultHttpContext();
            final Dictionary<String, String> initParams = new Hashtable<String, String>();
            initParams.put("servlet-name", "TestServlet");
            try {
                httpService.registerServlet(ALIAS, new EchoServlet(), initParams, httpContext);
                LOG.info("Servlet registered successfully");
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
            return super.addingService(reference);
        }

        @Override
        public void removedService(ServiceReference<HttpService> reference, HttpService service) {
            HttpService httpService = context.getService(reference);
            httpService.unregister(ALIAS);
            super.removedService(reference, service);
        }

    }
}
