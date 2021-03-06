/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.examples.camel.java;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.core.osgi.OsgiClassResolver;
import org.apache.camel.core.osgi.OsgiDataFormatResolver;
import org.apache.camel.core.osgi.OsgiDefaultCamelContext;
import org.apache.camel.core.osgi.OsgiLanguageResolver;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import java.util.ArrayList;

@Component(
        name = "karaf-camel-example-java",
        immediate = true
)
public class CamelComponent {

    private ModelCamelContext camelContext;
    private ServiceRegistration<CamelContext> serviceRegistration;

    @Activate
    public void activate(ComponentContext componentContext) throws Exception {
        BundleContext bundleContext = componentContext.getBundleContext();
        OsgiDefaultCamelContext osgiDefaultCamelContext = new OsgiDefaultCamelContext(bundleContext);
        osgiDefaultCamelContext.setClassResolver(new OsgiClassResolver(camelContext, bundleContext));
        osgiDefaultCamelContext.setDataFormatResolver(new OsgiDataFormatResolver(bundleContext));
        osgiDefaultCamelContext.setLanguageResolver(new OsgiLanguageResolver(bundleContext));
        osgiDefaultCamelContext.setName("context-example");
        camelContext = osgiDefaultCamelContext;
        serviceRegistration = bundleContext.registerService(CamelContext.class, camelContext, null);
        camelContext.start();
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("jetty:http://0.0.0.0:9090/example")
                        .id("example-http-inbound")
                        .convertBodyTo(String.class)
                        .log("[EXAMPLE INBOUND] Received: ${body}")
                        .choice()
                        .when().simple("${headers.CamelHttpMethod} == 'POST'")
                        .setHeader("type").jsonpath("$.notification.type")
                        .choice()
                        .when().simple("${header.type} == 'email'")
                        .log("[EXAMPLE INBOUND] Received email notification")
                        .to("direct:email")
                        .setHeader("Exchange.HTTP_RESPONSE_CODE", constant(200))
                        .when().simple("${header.type} == 'http'")
                        .log("[EXAMPLE INBOUND] Received http notification")
                        .to("direct:http")
                        .setHeader("Exchange.HTTP_RESPONSE_CODE", constant(200))
                        .otherwise()
                        .log("[EXAMPLE INBOUND] Unknown notification")
                        .setBody(constant("{ \"status\": \"reject\", \"type\": \"unknown\" }"))
                        .setHeader("Exchange.HTTP_RESPONSE_CODE", constant(400))
                        .otherwise()
                        .log("[EXAMPLE INBOUND] only POST is accepted (${headers.CamelHttpMethod})")
                        .setBody(constant("{ \"error\": \"only POST is accepted\" }"))
                        .setHeader("Exchange.HTTP_RESPONSE_CODE", constant(500));

                from("direct:email")
                        .id("example-email")
                        .log("[EXAMPLE EMAIL] Sending notification email")
                        .setHeader("to").jsonpath("$.notification.to")
                        .setHeader("subject", constant("Notification"))
                        .setHeader("payload").jsonpath("$.notification.message")
                        //.to("smtp://localhost");
                        .setBody(simple("{ \"status\": \"email sent\", \"to\": \"${header.to}\", \"subject\": \"${header.subject}\" }"));

                from("direct:http")
                        .id("example-http")
                        .log("[EXAMPLE HTTP] Sending http notification")
                        .setHeader("service").jsonpath("$.notification.service")
                        // send to HTTP service
                        .setBody(simple("{ \"status\": \"http requested\", \"service\": \"${header.service}\" }"));
            }
        });
    }

    @Deactivate
    public void deactivate() throws Exception {
        camelContext.stop();
        camelContext.removeRouteDefinitions(new ArrayList<RouteDefinition>(camelContext.getRouteDefinitions()));
        serviceRegistration.unregister();
    }

}
