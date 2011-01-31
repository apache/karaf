/*
 *  Copyright 2009 Marcin.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.apache.karaf.webconsole.admin;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.karaf.admin.AdminService;
import org.apache.karaf.admin.Instance;
import org.apache.karaf.admin.InstanceSettings;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.json.JSONException;
import org.json.JSONWriter;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

/**
 * Felix Web Console plugin for interacting with the {@link AdminService}
 */
public class AdminPlugin extends AbstractWebConsolePlugin {

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(AdminPlugin.class);

    public static final String NAME = "admin";
    public static final String LABEL = "Admin";
    private String adminJs = "/admin/res/ui/admin.js";
    private BundleContext bundleContext;
    private AdminService adminService;
    private ClassLoader classLoader;

    /**
     * Blueprint lifecycle callback methods
     */
    public void start() {
        super.activate(bundleContext);
        this.classLoader = this.getClass().getClassLoader();
        this.logger.info(LABEL + " plugin activated");
    }

    public void stop() {
        this.logger.info(LABEL + " plugin deactivated");
        super.deactivate();
    }

    @Override
    public String getTitle() {
        return LABEL;
    }

    @Override
    public String getLabel() {
        return NAME;
    }

    @Override
    protected void renderContent(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        final PrintWriter pw = res.getWriter();

        String appRoot = (String) req.getAttribute("org.apache.felix.webconsole.internal.servlet.OsgiManager.appRoot");
        final String adminScriptTag = "<script src='" + appRoot + this.adminJs + "' language='JavaScript'></script>";
        pw.println(adminScriptTag);

        pw.println("<script type='text/javascript'>");
        pw.println("// <![CDATA[");
        pw.println("var imgRoot = '" + appRoot + "/res/imgs';");
        pw.println("// ]]>");
        pw.println("</script>");

        pw.println("<div id='plugin_content'/>");

        pw.println("<script type='text/javascript'>");
        pw.println("// <![CDATA[");
        pw.print("renderAdmin( ");
        writeJSON(pw);
        pw.println(" )");
        pw.println("// ]]>");
        pw.println("</script>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        boolean success = false;

        String action = req.getParameter("action");
        String name = req.getParameter("name");

        if (action == null) {
            success = true;
        } else if ("create".equals(action)) {
            int sshPort = parsePortNumber(req.getParameter("sshPort"));
            int rmiPort = parsePortNumber(req.getParameter("rmiPort"));
            String location = parseString(req.getParameter("location"));
            String javaOpts = parseString(req.getParameter("javaOpts"));
            List<String> featureURLs = parseStringList(req.getParameter("featureURLs"));
            List<String> features = parseStringList(req.getParameter("features"));
            InstanceSettings settings = new InstanceSettings(sshPort, rmiPort, location, javaOpts, featureURLs, features);
            success = createInstance(name, settings);
        } else if ("destroy".equals(action)) {
            success = destroyInstance(name);
        } else if ("start".equals(action)) {
            String javaOpts = req.getParameter("javaOpts");
            success = startInstance(name, javaOpts);
        } else if ("stop".equals(action)) {
            success = stopInstance(name);
        }

        if (success) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            this.renderJSON(res, null);
        } else {
            super.doPost(req, res);
        }
    }

    /*
     * Parse the String value, returning <code>null</code> if the String is empty 
     */
    private String parseString(String value) {
        if (value != null && value.trim().length() == 0) {
            value = null;
        }
        return value;
    }
    
    private List<String> parseStringList(String value) {
        List<String> list = new ArrayList<String>();
        if (value != null) {
            for (String el : value.split(",")) {
                String trimmed = el.trim();
                if (trimmed.length() == 0) {
                    continue;
                }
                list.add(trimmed);
            }            
        }
        return list;
    }

    /*
     * Parse the port number for the String given, returning 0 if the String does not represent an integer 
     */
    private int parsePortNumber(String port) {
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    protected URL getResource(String path) {
        path = path.substring(NAME.length() + 1);
        URL url = this.classLoader.getResource(path);
        if (url != null) {
            InputStream ins = null;
            try {
                ins = url.openStream();
                if (ins == null) {
                    this.logger.error("failed to open " + url);
                    url = null;
                }
            } catch (IOException e) {
                this.logger.error(e.getMessage(), e);
                url = null;
            } finally {
                if (ins != null) {
                    try {
                        ins.close();
                    } catch (IOException e) {
                        this.logger.error(e.getMessage(), e);
                    }
                }
            }
        } 
        return url;
    }

    private void renderJSON(final HttpServletResponse response, final String feature) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        final PrintWriter pw = response.getWriter();
        writeJSON(pw);
    }

    private void writeJSON(final PrintWriter pw) {
        final JSONWriter jw = new JSONWriter(pw);
        final Instance[] instances = adminService.getInstances();
        try {
            jw.object();
            jw.key("status");
            jw.value(getStatusLine());
            jw.key("instances");
            jw.array();
            for (Instance i : instances) {
//                if (!i.isRoot()) {
                    instanceInfo(jw, i);
//                }
            }
            jw.endArray();
            jw.endObject();
        } catch (JSONException ex) {
            Logger.getLogger(AdminPlugin.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(AdminPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void instanceInfo(JSONWriter jw, Instance instance) throws JSONException, Exception {
        jw.object();
        jw.key("pid");
        jw.value(instance.getPid());
        jw.key("name");
        jw.value(instance.getName());
        jw.key("sshPort");
        jw.value(instance.getSshPort());
        jw.key("rmiPort");
        jw.value(instance.getRmiRegistryPort());
        jw.key("state");
        jw.value(instance.getState());
        jw.key("location");
        jw.value(instance.getJavaOpts() != null ? instance.getJavaOpts() : "");
        jw.key("javaopts");
        jw.value(instance.getLocation());
        jw.key("actions");
        jw.array();
        action(jw, "destroy", "Destroy", "delete");
        if (instance.getState().equals(Instance.STARTED)) {
            action(jw, "stop", "Stop", "stop");
        } else if (instance.getState().equals(Instance.STARTING)) {
            action(jw, "stop", "Stop", "stop");
        } else if (instance.getState().equals(Instance.STOPPED)) {
            action(jw, "start", "Start", "start");
        }
        jw.endArray();
        jw.endObject();
    }

    private void action(JSONWriter jw, String op, String title, String image) throws JSONException {
        jw.object();
        jw.key("op").value(op);
        jw.key("title").value(title);
        jw.key("image").value(image);
        jw.endObject();
    }

    private String getStatusLine() {
        final Instance[] instances = adminService.getInstances();
        int started = 0, starting = 0, stopped = 0;
        for (Instance instance : instances) {
            try {
                if (instance.getState().equals(Instance.STARTED)) {
                    started++;
                } else if (instance.getState().equals(Instance.STARTING)) {
                    starting++;
                } else if (instance.getState().equals(Instance.STOPPED)) {
                    stopped++;
                }
            } catch (Exception ex) {
                Logger.getLogger(AdminPlugin.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        final StringBuffer buffer = new StringBuffer();
        buffer.append("Instance information: ");
        buffer.append(instances.length);
        buffer.append(" instance");
        if (instances.length != 1) {
            buffer.append('s');
        }
        buffer.append(" in total");
        if (started == instances.length) {
            buffer.append(" - all started");
        } else {
            if (started != 0) {
                buffer.append(", ");
                buffer.append(started);
                buffer.append(" started");
            }
            if (starting != 0) {
                buffer.append(", ");
                buffer.append(starting);
                buffer.append(" starting");
            }
            buffer.append('.');
        }
        return buffer.toString();
    }

    private boolean createInstance(String name, InstanceSettings settings) {
        try {
            adminService.createInstance(name, settings);
            return true;
        } catch (Exception ex) {
            Logger.getLogger(AdminPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    private boolean destroyInstance(String name) {
        try {
            Instance instance = adminService.getInstance(name);
            if (instance != null) {
                instance.destroy();
                return true;
            }
        } catch (Exception ex) {
            Logger.getLogger(AdminPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    private boolean startInstance(String name, String javaOpts) {
        try {
            Instance instance = adminService.getInstance(name);
            if (instance != null) {
                instance.start(javaOpts);
                return true;
            }
        } catch (Exception ex) {
            Logger.getLogger(AdminPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    private boolean stopInstance(String name) {
        try {
            Instance instance = adminService.getInstance(name);
            if (instance != null) {
                instance.stop();
                return true;
            }
        } catch (Exception ex) {
            Logger.getLogger(AdminPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * @param adminService the adminService to set
     */
    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * @param bundleContext the bundleContext to set
     */
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
