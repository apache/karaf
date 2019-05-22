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
package org.apache.karaf.http.core.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.servlet.Servlet;

import org.apache.karaf.http.core.ServletInfo;
import org.apache.karaf.http.core.ServletService;
import org.ops4j.pax.web.service.spi.ServletEvent;

public class ServletServiceImpl implements ServletService {
    private ServletEventHandler servletEventHandler;

    public ServletServiceImpl(ServletEventHandler servletEventHandler) {
        this.servletEventHandler = servletEventHandler;
    }

    @Override
    public List<ServletInfo> getServlets() {
        List<ServletInfo> servletInfos = new ArrayList<>();
        List<ServletEvent> events = servletEventHandler.getServletEvents();
        events.sort(Comparator.<ServletEvent>comparingLong(s -> s.getBundle().getBundleId())
                .thenComparing(ServletEvent::getServletName));
        for (ServletEvent event : events) {
            Servlet servlet = event.getServlet();
            String servletClassName = " ";
            if (servlet != null) {
                    servletClassName = servlet.getClass().getName();
                    servletClassName = servletClassName.substring(servletClassName.lastIndexOf(".") + 1);
            }
            String servletName = event.getServletName() != null ? event.getServletName() : " ";
            if (servletName.contains(".")) {
                servletName = servletName.substring(servletName.lastIndexOf(".") + 1);
            }

            String alias = event.getAlias();
            String[] urls = event.getUrlParameter();

            String contextPath = event.getBundle().getHeaders().get("Web-ContextPath");
            if (contextPath == null) {
                contextPath = event.getBundle().getHeaders().get("Webapp-Context"); // this one used by pax-web but is deprecated
            }
            if (contextPath != null) {
                contextPath = contextPath.trim();
                if (!contextPath.startsWith("/")) {
                    contextPath = "/" + contextPath;
                }
                if (alias != null) {
                    alias = contextPath + alias;
                }
                if (urls != null) {
                    urls = urls.clone();
                    for (int i = 0; i < urls.length; i++) {
                        if (urls[i].startsWith("/")) {
                            urls[i] = contextPath + urls[i];
                        } else {
                            urls[i] = contextPath + "/" + urls[i];
                        }
                    }
                }
            }

            ServletInfo info = new ServletInfo();
            info.setBundleId(event.getBundle().getBundleId());
            info.setName(servletName);
            info.setClassName(servletClassName);
            info.setState(event.getType());
            info.setAlias(alias != null ? alias : " ");
            info.setUrls(urls != null ? urls : new String[] {""});
            servletInfos.add(info);
        }
        return servletInfos;
    }

}
