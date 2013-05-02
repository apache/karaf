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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.json.JSONException;
import org.json.JSONWriter;
import org.ops4j.pax.web.service.spi.ServletEvent;
import org.ops4j.pax.web.service.spi.WebEvent;
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
    private String featuresJs = "/http/res/ui/http-contexts.js";
    private ServletEventHandler servletEventHandler;
    private WebEventHandler webEventHandler;
    private BundleContext bundleContext;

    @Override
    protected boolean isHtmlRequest(HttpServletRequest request) {
        return false;
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
        if (path == null || path.isEmpty()) {
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
        final String statusLine = this.getStatusLine(servlets, web);
        final JSONWriter jw = new JSONWriter(pw);

        try {
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
                jw.key("state");
                jw.value(servlet.getState());
                jw.key("alias");
                jw.value(servlet.getAlias());
                jw.key("urls");
                jw.array();
                for (String url : servlet.getUrls()) {
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
                jw.key("bundlestate");
                jw.value(webDetail.getState());
                jw.key("contextpath");
                jw.value(webDetail.getContextPath());
                jw.key("state");
                jw.value(webDetail.getWebState());
                jw.endObject();
            }
            jw.endArray();

            jw.endObject();
        } catch (JSONException je) {
            throw new IOException(je.toString());
        }

    }

    protected List<ServletDetails> getServletDetails() {

        Collection<ServletEvent> events = servletEventHandler.getServletEvents();
        List<ServletDetails> result = new ArrayList<ServletDetails>(events.size());

        for (ServletEvent event : events) {
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

            ServletDetails details = new ServletDetails();
            details.setId(event.getBundle().getBundleId());
            details.setAlias(alias);
            details.setServlet(servletClassName);
            details.setServletName(servletName);
            details.setState(getStateString(event.getType()));
            details.setUrls(urls);
            result.add(details);
        }
        return result;
    }

    protected List<WebDetail> getWebDetails() {
        Map<Long, WebEvent> bundleEvents = webEventHandler.getBundleEvents();

        List<WebDetail> result = new ArrayList<WebDetail>();

        for (WebEvent event : bundleEvents.values()) {

            WebDetail webDetail = new WebDetail();
            webDetail.setBundleId(event.getBundle().getBundleId());
            webDetail.setContextPath(event.getContextPath().trim().concat("/"));
            int state = event.getBundle().getState();
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

            webDetail.setWebState(getStateString(event.getType()));
            result.add(webDetail);
        }

        return result;
    }

    public String getStatusLine(List<ServletDetails> servlets, List<WebDetail> web) {
        Map<String, Integer> states = new HashMap<String, Integer>();
        for (ServletDetails servlet : servlets) {
            Integer count = states.get(servlet.getState());
            if (count == null) {
                states.put(servlet.getState(), 1);
            } else {
                states.put(servlet.getState(), 1 + count);
            }
        }
        StringBuilder stateSummary = new StringBuilder();
        boolean first = true;
        for (Entry<String, Integer> state : states.entrySet()) {
            if (!first) {
                stateSummary.append(", ");
            }
            first = false;
            stateSummary.append(state.getValue()).append(" ").append(state.getKey());
        }

        return "Http contexts: " + stateSummary.toString();
    }

    public String getStateString(int type) {
        switch (type) {
            case WebEvent.DEPLOYING:
                return "Deploying";
            case WebEvent.DEPLOYED:
                return "Deployed";
            case WebEvent.UNDEPLOYING:
                return "Undeploying";
            case WebEvent.UNDEPLOYED:
                return "Undeployed";
            case WebEvent.FAILED:
                return "Failed";
            case WebEvent.WAITING:
                return "Waiting";
            default:
                return "Failed";
        }
    }

    public void setServletEventHandler(ServletEventHandler eventHandler) {
        this.servletEventHandler = eventHandler;
    }

    public void setWebEventHandler(WebEventHandler eventHandler) {
        this.webEventHandler = eventHandler;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
