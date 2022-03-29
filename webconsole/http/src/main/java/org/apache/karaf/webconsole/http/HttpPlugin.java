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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.utils.json.JSONWriter;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.karaf.http.core.Proxy;
import org.apache.karaf.http.core.ProxyService;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.info.ServletInfo;
import org.ops4j.pax.web.service.spi.model.info.WebApplicationInfo;
import org.ops4j.pax.web.service.spi.model.views.ReportWebContainerView;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebConsole plugin to use with HTTP service.
 */
public class HttpPlugin extends AbstractWebConsolePlugin {

    private final Logger log = LoggerFactory.getLogger(HttpPlugin.class);

    public static final String NAME = "http";
    public static final String LABEL = "Http";
    private ClassLoader classLoader;
    private final String featuresJs = "/http/res/ui/http-contexts.js";
    private WebContainer webContainer;
    private BundleContext bundleContext;
    private ProxyService proxyService;

    @Override
    protected boolean isHtmlRequest(HttpServletRequest request) {
        return true;
    }

    public void start() {
        super.activate(bundleContext);
        this.classLoader = this.getClass().getClassLoader();
        this.log.info(LABEL + " plugin activated");
    }

    public void stop() {
        this.log.info(LABEL + " plugin deactivated");
        super.deactivate();
    }

    @Override
    public String getLabel() {
        return NAME;
    }

    @Override
    public String getTitle() {
        return LABEL;
    }

    @Override
    protected void renderContent(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // get request info from request attribute
        final PrintWriter pw = response.getWriter();

        String appRoot = (String) request.getAttribute(WebConsoleConstants.ATTR_APP_ROOT);
        final String featuresScriptTag = "<script src='" + appRoot + this.featuresJs
                + "' language='JavaScript'></script>";
        pw.println(featuresScriptTag);

        pw.println("<script type='text/javascript'>");
        pw.println("// <![CDATA[");
        pw.println("var imgRoot = '" + appRoot + "/res/imgs';");
        pw.println("// ]]>");
        pw.println("</script>");

        pw.println("<div id='plugin_content'/>");

        pw.println("<script type='text/javascript'>");
        pw.println("// <![CDATA[");
        pw.print("renderFeatures( ");
        writeJSON(pw);
        pw.println(" )");
        pw.println("// ]]>");
        pw.println("</script>");
    }

    protected URL getResource(String path) {
        path = path.substring(NAME.length() + 1);
        if (path.isEmpty()) {
            return null;
        }
        URL url = this.classLoader.getResource(path);
        if (url != null) {
            InputStream ins = null;
            try {
                ins = url.openStream();
                if (ins == null) {
                    this.log.error("failed to open " + url);
                    url = null;
                }
            } catch (IOException e) {
                this.log.error(e.getMessage(), e);
                url = null;
            } finally {
                if (ins != null) {
                    try {
                        ins.close();
                    } catch (IOException e) {
                        this.log.error(e.getMessage(), e);
                    }
                }
            }
        }
        return url;
    }

    private void writeJSON(final PrintWriter pw) throws IOException {

        final List<ServletDetails> servlets = this.getServletDetails();
        final List<WebDetail> web = this.getWebDetails();
        final Map<String, Proxy> proxies = proxyService.getProxies();
        final String statusLine = this.getStatusLine(servlets);
        final JSONWriter jw = new JSONWriter(pw);

        jw.object();

        jw.key("status");
        jw.value(statusLine);

        jw.key("contexts");
        jw.array();
        for (ServletDetails servlet : servlets) {
            jw.object();
            jw.key("id");
            jw.value(servlet.getId());
            jw.key("servlet");
            jw.value(servlet.getServlet());
            jw.key("servletName");
            jw.value(servlet.getServletName());
            jw.key("type");
            jw.value(servlet.getType());
            jw.key("urls");
            jw.array();
            for (String url : servlet.getUrls()) {
                jw.value(url);
            }
            jw.endArray();
            jw.key("contexts");
            jw.array();
            for (String url : servlet.getContexts()) {
                jw.value(url);
            }
            jw.endArray();
            jw.endObject();
        }
        jw.endArray();

        jw.key("web");
        jw.array();
        for (WebDetail webDetail : web) {
            jw.object();
            jw.key("id");
            jw.value(webDetail.getBundleId());
            jw.key("bundleState");
            jw.value(webDetail.getState());
            jw.key("contextpath");
            jw.value(webDetail.getContextPath());
            jw.key("state");
            jw.value(webDetail.getWebState());
            jw.endObject();
        }
        jw.endArray();

        jw.key("proxy");
        jw.array();
        for (String proxy : proxies.keySet()) {
            jw.object();
            jw.key("url");
            jw.value(proxy);
            jw.key("proxyTo");
            jw.value(proxies.get(proxy).getProxyTo());
            jw.key("Balancing");
            jw.value(proxies.get(proxy).getBalancingPolicy());
            jw.endObject();
        }
        jw.endArray();

        jw.endObject();
    }

    protected List<ServletDetails> getServletDetails() {
        List<ServletDetails> result = new ArrayList<>();

        ReportWebContainerView view = webContainer.adapt(ReportWebContainerView.class);

        for (ServletInfo info : view.listServlets()) {
            String servletClassName = info.getServletClass();
            String servletName = info.getServletName();
            if (servletName.contains(".")) {
                servletName = servletName.substring(servletName.lastIndexOf(".") + 1);
            }

            String[] urls = info.getMapping() != null ? info.getMapping() : new String[] { "" };
            String[] contexts = info.getContexts() != null ? info.getContexts() : new String[] { "" };

            ServletDetails details = new ServletDetails();
            details.setId(info.getBundle().getBundleId());
            details.setServlet(servletClassName);
            details.setServletName(servletName);
            details.setType(info.getType());
            details.setUrls(urls);
            details.setContexts(contexts);
            result.add(details);
        }
        return result;
    }

    protected List<WebDetail> getWebDetails() {
        List<WebDetail> result = new ArrayList<>();

        ReportWebContainerView view = webContainer.adapt(ReportWebContainerView.class);

        for (WebApplicationInfo info : view.listWebApplications()) {
            if (!info.isWab()) {
                continue;
            }
            WebDetail webDetail = new WebDetail();
            webDetail.setBundleId(info.getBundle().getBundleId());
            webDetail.setContextPath(info.getContextPath().trim());
            int state = bundleContext.getBundle(info.getBundle().getBundleId()).getState();
            String stateStr;
            if (state == Bundle.ACTIVE) {
                stateStr = "Active";
            } else if (state == Bundle.INSTALLED) {
                stateStr = "Installed";
            } else if (state == Bundle.RESOLVED) {
                stateStr = "Resolved";
            } else if (state == Bundle.STARTING) {
                stateStr = "Starting";
            } else if (state == Bundle.STOPPING) {
                stateStr = "Stopping";
            } else {
                stateStr = "Unknown";
            }
            webDetail.setState(stateStr);

            webDetail.setWebState(info.getDeploymentState());
            result.add(webDetail);
        }

        return result;
    }

    public String getStatusLine(List<ServletDetails> servlets) {
        Map<String, Integer> types = new HashMap<>();
        for (ServletDetails servlet : servlets) {
            types.merge(servlet.getType(), 1, Integer::sum);
        }
        StringBuilder stateSummary = new StringBuilder();
        boolean first = true;
        for (Entry<String, Integer> state : types.entrySet()) {
            if (!first) {
                stateSummary.append(", ");
            }
            first = false;
            stateSummary.append(state.getValue()).append(" from ").append(state.getKey());
        }

        return "Servlets: " + stateSummary;
    }

    public void setWebContainer(WebContainer webContainer) {
        this.webContainer = webContainer;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setProxyService(ProxyService proxyService) {
        this.proxyService = proxyService;
    }

}
