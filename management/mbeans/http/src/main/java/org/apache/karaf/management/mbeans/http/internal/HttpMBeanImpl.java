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
package org.apache.karaf.management.mbeans.http.internal;

import org.apache.karaf.management.mbeans.http.HttpMBean;
import org.ops4j.pax.web.service.spi.ServletEvent;
import org.ops4j.pax.web.service.spi.WebEvent;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import javax.servlet.Servlet;
import java.util.Arrays;
import java.util.Collection;

/**
 * Implementation of the HTTP MBean.
 */
public class HttpMBeanImpl extends StandardMBean implements HttpMBean {

    private ServletEventHandler servletEventHandler;

    public HttpMBeanImpl() throws NotCompliantMBeanException {
        super(HttpMBean.class);
    }

    public TabularData getServlets() throws Exception {
        Collection<ServletEvent> events = servletEventHandler.getServletEvents();
        CompositeType servletType = new CompositeType("Servlet", "HTTP Servlet",
                new String[]{"ID", "Servlet", "Servlet Name", "State", "Alias", "URL"},
                new String[]{"ID of the servlet", "Class name of the servlet", "Servlet Name", "Current state of the servlet", "Aliases of the servlet", "URL of the servlet"},
                new OpenType[]{SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING});
        TabularType tableType = new TabularType("Servlets", "Table of all HTTP servlets", servletType, new String[]{"ID"});
        TabularData table = new TabularDataSupport(tableType);

        for (ServletEvent event : events) {
            try {
                Servlet servlet = event.getServlet();
                String servletClassName = " ";
                if (servlet != null) {
                    servletClassName = servlet.getClass().getName();
                    servletClassName = servletClassName.substring(servletClassName.lastIndexOf(".") + 1, servletClassName.length());
                }
                String servletName = event.getServletName() != null ? event.getServletName() : " ";
                if (servletName.contains(".")) {
                    servletName = servletName.substring(servletName.lastIndexOf(".") + 1, servletName.length());
                }

                String alias = event.getAlias() != null ? event.getAlias() : " ";

                String[] urls = (String[]) (event.getUrlParameter() != null ? event.getUrlParameter() : new String[]{""});

                CompositeData data = new CompositeDataSupport(servletType,
                        new String[]{"ID", "Servlet", "Servlet Name", "State", "Alias", "URL"},
                        new Object[]{event.getBundle().getBundleId(), servletClassName, servletName, getStateString(event.getType()), alias, Arrays.toString(urls)});
                table.put(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return table;
    }

    private String getStateString(int type) {
        switch (type) {
            case WebEvent.DEPLOYING:
                return "Deploying  ";
            case WebEvent.DEPLOYED:
                return "Deployed   ";
            case WebEvent.UNDEPLOYING:
                return "Undeploying";
            case WebEvent.UNDEPLOYED:
                return "Undeployed ";
            case WebEvent.FAILED:
                return "Failed     ";
            case WebEvent.WAITING:
                return "Waiting    ";
            default:
                return "Failed     ";
        }
    }

    public ServletEventHandler getServletEventHandler() {
        return this.servletEventHandler;
    }

    public void setServletEventHandler(ServletEventHandler servletEventHandler) {
        this.servletEventHandler = servletEventHandler;
    }

}
