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

import java.util.Arrays;
import java.util.List;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.apache.karaf.http.core.HttpMBean;
import org.apache.karaf.http.core.ServletInfo;
import org.apache.karaf.http.core.ServletService;

/**
 * Implementation of the HTTP MBean.
 */
public class Http extends StandardMBean implements HttpMBean {
    private ServletService servletService;

    public Http(ServletService servletService) throws NotCompliantMBeanException {
        super(HttpMBean.class);
        this.servletService = servletService;
    }

    public TabularData getServlets() {
        try {
            CompositeType servletType = new CompositeType("Servlet", "HTTP Servlet",
                new String[]{"Bundle-ID", "Servlet", "Servlet Name", "State", "Alias", "URL"},
                new String[]{"ID of the bundle that registered the servlet", "Class name of the servlet", "Servlet Name", "Current state of the servlet", "Aliases of the servlet", "URL of the servlet"},
                new OpenType[]{SimpleType.LONG, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING});
            TabularType tableType = new TabularType("Servlets", "Table of all HTTP servlets", servletType, new String[]{"Bundle-ID", "Servlet Name", "State"});
            TabularData table = new TabularDataSupport(tableType);
            List<ServletInfo> servletInfos = servletService.getServlets();
            for (ServletInfo info : servletInfos) {
            
                CompositeData data = new CompositeDataSupport(servletType,
                        new String[]{"Bundle-ID", "Servlet", "Servlet Name", "State", "Alias", "URL"},
                        new Object[]{info.getBundle().getBundleId(), info.getClassName(), info.getName(), info.getStateString(), info.getAlias(), Arrays.toString(info.getUrls())});
                table.put(data);
            
            }
            return table;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
