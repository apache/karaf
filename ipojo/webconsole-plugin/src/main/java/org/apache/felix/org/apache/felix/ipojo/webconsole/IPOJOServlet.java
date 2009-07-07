/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.org.apache.felix.ipojo.webconsole;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.handlers.dependency.DependencyDescription;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandlerDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedService;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandlerDescription;
import org.apache.felix.ipojo.util.DependencyModel;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * iPOJO Plugin for the web console.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Component(immediate = true)
@Provides(specifications = { Servlet.class })
public class IPOJOServlet extends AbstractWebConsolePlugin {

    /**
     * Factory constant.
     */
    public static final String FACTORY = "factory";

    /**
     * Instance constant.
     */
    public static final String INSTANCE = "instance";

    /**
     * Handler constant.
     */
    public static final String HANDLER = "handler";

    /**
     * All constant.
     */
    public static final String ALL = "all";

    /**
     * UUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Label used by the web console.
     */
    @ServiceProperty(name = "felix.webconsole.label")
    private String m_label = "ipojo";

    /**
     * Title used by the web console.
     */
    @ServiceProperty(name = "felix.webconsole.title")
    private String m_title = "iPOJO";

    /**
     * List of available Architecture service.
     */
    @Requires(optional = true, specification = "org.apache.felix.ipojo.architecture.Architecture")
    private List<Architecture> m_archs;

    /**
     * List of available Factories.
     */
    @Requires(optional = true, specification = "org.apache.felix.ipojo.Factory")
    private List<Factory> m_factories;

    /**
     * List of available Handler Factories.
     */
    @Requires(optional = true, specification = "org.apache.felix.ipojo.HandlerFactory")
    private List<HandlerFactory> m_handlers;

    /**
     * Creates a IPOJOServlet.
     * This method activates the plugin.
     * @param bc the bundle context
     */
    public IPOJOServlet(BundleContext bc) {
        activate(bc);
    }

    /**
     * Stop method.
     * This method deactivates the plugin.
     */
    @Invalidate
    public void stop() {
        deactivate();
    }


    /**
     * Gets the plugin label.
     * @return the label
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#getLabel()
     */
    public String getLabel() {
        return m_label;
    }

    /**
     * Gets the plugin title.
     * @return the title
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#getTitle()
     */
    public String getTitle() {
        return m_title;
    }

    /**
     * Gets the number of valid instances.
     * @return the number of valid instances.
     */
    private int getValidCount() {
        int i = 0;
        for (Architecture a : m_archs) {
            if (a.getInstanceDescription().getState() == ComponentInstance.VALID) {
                i ++;
            }
        }
        return i;
    }

    /**
     * Gets the number of invalid instances.
     * @return the number of invalid instances.
     */
    private int getInvalidCount() {
        int i = 0;
        for (Architecture a : m_archs) {
            if (a.getInstanceDescription().getState() == ComponentInstance.INVALID) {
                i ++;
            }
        }
        return i;
    }

    /**
     * Gets the HTML content of the plugin.
     * @param req the request
     * @param res the response
     * @throws ServletException if the content cannot be computed
     * @throws IOException if an IO Exception occurs
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void renderContent(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        PrintWriter writer = res.getWriter();
        // Set the fable plugin_table sortable.
        writer.write("<script type=\"text/javascript\"> "
                 + "function setSortable() { \n"
                 + "$(document).ready(function() { \n"
                 + "$(\"#plugin_table\").tablesorter(); \n"
                 + "})};\n"
                 + "setSortable();"
                 + "</script>");
        // Status Line
        writer.write("<div class=\"fullwidth\"><div class=\"statusline\">"
                + m_archs.size() + " instances in total, "
                + getValidCount() + " valid instances, "
                + getInvalidCount() + " invalid instances."
                +        "</div></div>");
        // Button Line
        writer.write("<form action=\"ipojo\" method=\"get\">"
                + "<div class=\"fullwidth\">"
                + "<div class=\"buttons\">"
                + "<button style=\"margin-left: 30px;\" name=\"" + INSTANCE + "\" value=\"all\" type=\"submit\">Instances</button>"
                + "<button style=\"margin-left: 30px;\" name=\"" + FACTORY + "\" value=\"all\" type=\"submit\">Factories</button>"
                + "<button style=\"margin-left: 30px;\" name=\"" + HANDLER + "\" value=\"all\" type=\"submit\">Handlers</button>"
                + "</div>"
                + "</div>"
                + "</form>");

        // Compute the request and select the content to return
        RequestInfo ri = new RequestInfo(req);
        if (ri.m_type.equals(FACTORY)) {
            if (ri.m_all) {
                printFactoryList(writer);
            } else {
                printFactoryDetail(writer, ri.m_name);
            }
        }

        if (ri.m_type.equals(INSTANCE)) {
            if (ri.m_all) {
                printInstanceList(writer);
            } else {
                printInstanceDetail(writer, ri.m_name);
            }
        }

        if (ri.m_type.equals(HANDLER)) {
            if (ri.m_all) {
                printHandlerList(writer);
            }
        }

        // Button Line
        writer.write("<form action=\"ipojo\" method=\"get\">"
                + "<div class=\"fullwidth\">"
                + "<div class=\"buttons\">"
                + "<button style=\"margin-left: 30px;\" name=\"" + INSTANCE + "\" value=\"all\" type=\"submit\">Instances</button>"
                + "<button style=\"margin-left: 30px;\" name=\"" + FACTORY + "\" value=\"all\" type=\"submit\">Factories</button>"
                + "<button style=\"margin-left: 30px;\" name=\"" + HANDLER + "\" value=\"all\" type=\"submit\">Handlers</button>"
                + "</div>"
                + "</div>"
                + "</form>");

        // Status Line
        writer.write("<div class=\"fullwidth\"><div class=\"statusline\">"
                + m_archs.size() + " instances in total, "
                + getValidCount() + " valid instances, "
                + getInvalidCount() + " invalid instances."
                +        "</div></div>");

    }

    /**
     * Gets a Factory Detail page.
     * This page contains all versions of the factories.
     * @param pw the writer
     * @param name the name of the factory
     */
    private void printFactoryDetail(PrintWriter pw, String name) {
        List<ComponentTypeDescription> fs = new ArrayList<ComponentTypeDescription>();
        for (Factory f : this.m_factories) {
            if (f.getName().equals(name)) {
                fs.add(f.getComponentDescription());
            }
        }

        pw.write("<div class=\"table\">");
        if (fs.isEmpty()) {
            pw.write("<b>Factory not found</b><br>The factory " + name + " does not exist or is private");
        } else {
            for (ComponentTypeDescription factory : fs) {
                pw.write("<table class=\"tablelayout\" border=\"0\"><tbody>");
                addEntry(pw, "Factory Name", factory.getName());
                if (factory.getVersion() != null) {
                    addEntry(pw, "Factory Version", factory.getVersion());
                }
                addEntry(pw, "State", getFactoryState(factory.getFactory().getState()));

                addEntry(pw, " ", " ");
                if (factory.getprovidedServiceSpecification() != null && factory.getprovidedServiceSpecification().length != 0) {
                    addEntry(pw, "Provided Service Specification", Arrays.toString(factory.getprovidedServiceSpecification()));
                }
                if (factory.getProperties() != null && factory.getProperties().length != 0) {
                    addEntry(pw, "Properties", getProperties(factory.getProperties()));
                }

                addEntry(pw, " ", " ");
                addEntry(pw, "Required Handlers", factory.getFactory().getRequiredHandlers().toString());
                addEntry(pw, "Missing Handlers", factory.getFactory().getMissingHandlers().toString());
                addEntry(pw, " ", " ");
                addEntry(pw, "Created Instances", getInstanceList(factory.getName()));

                addEntry(pw, " ", " ");
                addEntry(pw, "Architecture",
                        "<div style=\"overflow: auto; width:800px; border: 1px solid #666; background-color: #ccc; padding: 8px;\"><pre>"
                            + factory.getDescription().toString()
                            + "</pre></div>");
                pw.write("</tbody></table>");
            }
        }
        pw.write("</div>");
    }


    /**
     * Gets component type properties HTML table.
     * @param properties the properties to display
     * @return the HTML code of the table
     */
    private String getProperties(PropertyDescription[] properties) {
        String s = "<table class=\"tablelayout\" style=\"border-top: 1px solid #6181A9;\">"
                + "<thead style=\"border-top: 1px solid #6181A9;\">"
                + "<tr>"
                + "<th>Property Name</th>"
                + "<th>Property Type</th>"
                + "<th>Mandatory</th>"
                + "<th>Immutable</th>"
                + "<th>Value</th>"
                + "</tr>"
                + "</thead>"
                + "<tbody>";
        for (PropertyDescription pd : properties) {
            String name = pd.getName();
            String type = pd.getType();
            boolean mandatory = pd.isMandatory();
            boolean immutable = pd.isImmutable();
            String value = pd.getValue();
            s += "<tr>"
                + "<td>" + name + "</td>"
                + "<td>" + type + "</td>"
                + "<td>" + "" + mandatory + "</td>"
                + "<td>" + "" + immutable + "</td>"
                + "<td>" + (value == null ? "<i>No Value</i>" : value) + "</td>"
                + "</tr>";
        }
        s += "</tbody></table>";
        return s;
    }

    /**
     * Gets the instance list page.
     * @param pw the writer
     */
    private void printInstanceList(PrintWriter pw) {
        pw.write("<div class=\"table\">"
                + "<table id=\"plugin_table\" class=\"tablelayout\">"
                + "<thead>"
                + "<tr>"
                + "<th class=\"col_Name header headerSortDown\">Instance Name</th>"
                + "<th class=\"col_Factory header \">Factory Name</th>"
                + "<th class=\"col_State header \">State</th>"
                + "</tr>"
                + "</thead>"
                + "<tbody>");

        for (Architecture arch : m_archs) {
            InstanceDescription id = arch.getInstanceDescription();
            String name = id.getName();
            String factory = id.getComponentDescription().getName();
            String state = getInstanceState(id.getState());

            pw.write("<tr>"
                    + "<td>" + getInstanceLink(name) + "</td>"
                    + "<td>" + factory + "</td>"
                    + "<td>" + state + "</td>"
                    + "</tr>");

        }

        pw.write("</tbody></table>");
    }

    /**
     * Adds a line into a key / value table.
     * @param pw the writer
     * @param header the key
     * @param value the value (may be HTML code)
     */
    private void addEntry(PrintWriter pw, String header, String value) {
        pw.write(addEntry(header, value));

    }

    /**
     * Gets the HTML code of a key / value table row.
     * @param header the key
     * @param value the value (may be HTML code)
     * @return the HTML code of the line
     */
    private String addEntry(String header, String value) {
        return "<tr>"
                + "<td class=\"aligntop\" nowrap=\"true\" style=\"border: 0px none;\">" + header + "</td>"
                + "<td class=\"aligntop\" style=\"border: 0px none ;\">" + value + "</td>"
                + "</tr>";
    }

    /**
     * Gets the factory link if the factory exists.
     * @param id the factory name
     * @return the HTML link targetting the detail of the factory or just the factory name.
     */
    private String getFactoryLinkIfPossible(InstanceDescription id) {
        String n = id.getComponentDescription().getName();
        System.out.println("Look for " + n);
        for (Factory f : m_factories) {
            if (f.getName().equals(n)) {
                return "<a href=\"ipojo?factory=" + n + "\">" + n + "</a>";
            }
        }

        return n; // No link
    }

    /**
     * Gets the list of invalid handler.
     * @param hl the handler description list.
     * @return the list of invalid handlers.
     */
    private String getInvalidHandlerList(HandlerDescription[] hl) {
        List<String> list = new ArrayList<String>();
        for (HandlerDescription hd : hl) {
            if (! hd.isValid()) {
                list.add(hd.getHandlerName());
            }
        }
        return list.toString();
    }

    /**
     * Gets the page containing instance detail.
     * @param pw the writer
     * @param name the instance name
     */
    private void printInstanceDetail(PrintWriter pw, String name) {
        InstanceDescription desc = getInstanceDescriptionByName(name);
        pw.write("<div class=\"table\">");
        if (desc == null) {
            pw.write("<b>Instance not found</b><br>The instance " + name + " does not exist or does not expose its architecture");
        } else {
            pw.write("<table class=\"tablelayout\" border=\"0\"><tbody>");
            addEntry(pw, "Instance Name", desc.getName());
            addEntry(pw, "Factory", getFactoryLinkIfPossible(desc));
            if (desc.getState() == ComponentInstance.INVALID) {
                addEntry(pw, "Invalid Handlers", getInvalidHandlerList(desc.getHandlers()));
            }
            addEntry(pw, " ", " ");
            addEntry(pw, "Provided Services", getProvidedServiceDetail(desc.getHandlerDescription("org.apache.felix.ipojo:provides")));
            addEntry(pw, "Required Services", getRequiredServiceDetail(desc.getHandlerDescription("org.apache.felix.ipojo:requires")));

            addEntry(pw, " ", " ");
            addEntry(pw, "Architecture",
                    "<div style=\"overflow: auto; width:800px; border: 1px solid #666; background-color: #ccc; padding: 8px;\"><pre>"
                        + desc.getDescription().toString()
                        + "</pre></div>");
            pw.write("</tbody></table>");
        }
        pw.write("</div>");
    }

    /**
     * Gets the HTML link targeting this instance.
     * @param name the instance name
     * @return the HTML link targetting the instance detail
     */
    private String getInstanceLink(String name) {
        return "<a href=\"ipojo?instance=" + name + "\">" + name + "</a>";
    }

    /**
     * Gets the HTML list of service properties.
     * @param properties the properties
     * @return the HTML code containing the service properties.
     */
    private String getServiceProperties(Properties properties) {
        String s = "<ul>";
        Enumeration<Object> e = properties.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            String value = properties.get(key).toString();
            s += "<li>" + key + " = " + value + "</li>";
        }
        s += "</ul>";
        return s;
    }

    /**
     * Gets the instance list created by the given factory.
     * @param factory the factory name
     * @return the HTML list containing the created instances
     */
    private String getInstanceList(String factory) {
        String s = "<ul>";
        for (Architecture arch : m_archs) {
            String n = arch.getInstanceDescription().getComponentDescription().getName();
            if (factory.equals(n)) {
                s += "<li>" + getInstanceLink(arch.getInstanceDescription().getName()) + "</li>";
            }
        }
        s += "</ul>";
        return s;
    }

    /**
     * Gets the HTML list containing service references.
     * If the service is provided by an iPOJO instance, the link is inserted.
     * @param refs the references.
     * @return the HTML list
     */
    private String getServiceReferenceList(List<ServiceReference> refs) {
        String s = "<ul>";
        for (ServiceReference ref : refs) {
            s += "<li>";
            if (ref.getProperty("instance.name") == null) {
                s += ref.getProperty(Constants.SERVICE_ID);
            } else {
                s += "<a href=\"ipojo?instance="
                        + ref.getProperty("instance.name") + "\">"
                        + ref.getProperty("instance.name") + " (" +  ref.getProperty(Constants.SERVICE_ID) + ")</a>";
            }
            s += "</li>";
        }
        s += "</ul>";
        return s;
    }

    /**
     * Gets provided service details.
     * @param hd the provided service handler description or <code>null</code> if not found.
     * @return the details about provided services
     */
    private String getProvidedServiceDetail(HandlerDescription hd) {
        if (hd == null) {
            return "No provided services";
        }

        String r = "";
        ProvidedServiceHandlerDescription desc = (ProvidedServiceHandlerDescription) hd;

        for (ProvidedServiceDescription ps : desc.getProvidedServices()) {
            r += "<table border=\"0\"><tbody>";
            r += addEntry("Specification", Arrays.toString(ps.getServiceSpecifications()));
            r += addEntry("State", getProvidedServiceState(ps.getState()));
            if (ps.getServiceReference() != null) {
                r += addEntry("Service Id", ((Long) ps.getServiceReference().getProperty(Constants.SERVICE_ID)).toString());
            }
            r += addEntry("Service Properties", getServiceProperties(ps.getProperties()));

            r += "</tbody></table>";
            r += "<hr style=\"color: #CCCCCC;'\"/>";
        }

        return r;

    }

    /**
     * Gets required service details.
     * @param hd the required service handler description or <code>null</code> if not found.
     * @return the details about required services
     */
    private String getRequiredServiceDetail(
            HandlerDescription hd) {
        if (hd == null) {
            return "No required services";
        }
        String r = "";
        DependencyHandlerDescription desc = (DependencyHandlerDescription) hd;
        for (DependencyDescription dep : desc.getDependencies()) {
            r += "<table border=\"0\" style=\"margin-bottom:5px\"><tbody>";
            r += addEntry("Specification", dep.getSpecification());
            r += addEntry("Id", "" + dep.getId());
            r += addEntry("State", getDependencyState(dep.getState()));
            r += addEntry("Binding Policy" , getDependencyBindingPolicy(dep.getPolicy()));
            r += addEntry("Optional", "" + dep.isOptional());
            r += addEntry("Aggregate", "" + dep.isMultiple());
            if (dep.getFilter() != null) {
                r += addEntry("Filter", "" + dep.getFilter());
            }
            if (dep.getComparator() != null) {
                r += addEntry("Comparator", "" + dep.getComparator());
            }
            r += addEntry("Matching Services", getServiceReferenceList(dep.getServiceReferences()));
            r += addEntry("Used Services", getServiceReferenceList(dep.getUsedServices()));

            r += "</tbody></table>";
            r += "<hr style=\"color: #CCCCCC;'\"/>";

        }

        return r;
    }

    /**
     * Gets the instance description by name.
     * @param name the instance name
     * @return the instance description or <code>null</code> if not found
     */
    private InstanceDescription getInstanceDescriptionByName(String name) {
        for (Architecture arch : m_archs) {
            if (name.equals(arch.getInstanceDescription().getName())) {
                return arch.getInstanceDescription();
            }
        }
        return null;
    }

    /**
     * Prints the factory list.
     * @param pw the writer.
     */
    private void printFactoryList(PrintWriter pw) {
        pw.write("<div class=\"table\">"
                + "<table id=\"plugin_table\" class=\"tablelayout\">"
                + "<thead>"
                + "<tr>"
                + "<th class=\"col_Name header headerSortDown\">Factory Name</th>"
                + "<th class=\"col_Bundle header \">Bundle</th>"
                + "<th class=\"col_State header \">State</th>"
                + "</tr>"
                + "</thead>"
                + "<tbody>");



        for (Factory factory : m_factories) {
            String name = factory.getName();
            String version = factory.getVersion();
            String state = getFactoryState(factory.getState());
            String bundle = factory.getBundleContext().getBundle().getSymbolicName()
                + " (" + factory.getBundleContext().getBundle().getBundleId() + ")";
            pw.write("<tr>"
                    + "<td><a href=\"ipojo?factory=" + name + "\">"
                        + (version == null ? name : name + " (" + version + ")")
                        + "</a></td>" //TODO Link
                    + "<td>" + bundle + "</td>"
                    + "<td>" + state + "</td>"
                    + "</tr>");

        }

        pw.write("</tbody></table>");
    }

    /**
     * Print the handler list.
     * @param pw the writer.
     */
    private void printHandlerList(PrintWriter pw) {
        pw.write("<div class=\"table\">"
                + "<table id=\"plugin_table\" class=\"tablelayout\">"
                + "<thead>"
                + "<tr>"
                + "<th class=\"col_Name header headerSortDown\">Handler Name</th>"
                + "<th class=\"col_Type header \">Handler Type</th>"
                + "<th class=\"col_Bundle header \">Bundle</th>"
                + "<th class=\"col_State header \">State</th>"
                + "<th class=\"col_MissingHandler header \">Missing Handler</th>"
                + "</tr>"
                + "</thead>"
                + "<tbody>");



        for (HandlerFactory hf : m_handlers) {
            String name = hf.getHandlerName();
            String type = hf.getType();
            String state = getFactoryState(hf.getState());
            String bundle = hf.getBundleContext().getBundle().getSymbolicName()
                + " (" + hf.getBundleContext().getBundle().getBundleId() + ")";
            String missing = hf.getMissingHandlers().isEmpty() ? "<i>no missing handlers</i>" : hf.getMissingHandlers().toString();
            pw.write("<tr>"
                    + "<td>" + name + "</td>"
                    + "<td>" + type + "</td>"
                    + "<td>" + bundle + "</td>"
                    + "<td>" + state + "</td>"
                    + "<td>" + missing + "</td>"
                    + "</tr>");

        }

        pw.write("</tbody></table>");
    }

    /**
     * Gets the instance state as a String.
     * @param state the state.
     * @return the String form of the state.
     */
    private String getInstanceState(int state) {
        switch(state) {
            case ComponentInstance.VALID :
                return "valid";
            case ComponentInstance.INVALID :
                return "invalid";
            case ComponentInstance.DISPOSED :
                return "disposed";
            case ComponentInstance.STOPPED :
                return "stopped";
            default :
                return "unknown";
        }
    }

    /**
     * Gets the factory state as a String.
     * @param state the state.
     * @return the String form of the state.
     */
    private String getFactoryState(int state) {
        switch(state) {
            case Factory.VALID :
                return "valid";
            case Factory.INVALID :
                return "invalid";
            default :
                return "unknown";
        }
    }

    /**
     * Gets the dependency state as a String.
     * @param state the state.
     * @return the String form of the state.
     */
    private String getDependencyState(int state) {
        switch(state) {
            case DependencyModel.RESOLVED :
                return "resolved";
            case DependencyModel.UNRESOLVED :
                return "unresolved";
            case DependencyModel.BROKEN :
                return "broken";
            default :
                return "unknown (" + state + ")";
        }
    }

    /**
     * Gets the dependency binding policy as a String.
     * @param policy the policy.
     * @return the String form of the policy.
     */
    private String getDependencyBindingPolicy(int policy) {
        switch(policy) {
            case DependencyModel.DYNAMIC_BINDING_POLICY :
                return "dynamic";
            case DependencyModel.DYNAMIC_PRIORITY_BINDING_POLICY :
                return "dynamic-priority";
            case DependencyModel.STATIC_BINDING_POLICY :
                return "static";
            default :
                return "unknown (" + policy + ")";
        }
    }

    /**
     * Gets the provided service state as a String.
     * @param state the state.
     * @return the String form of the state.
     */
    private String getProvidedServiceState(int state) {
        switch(state) {
            case ProvidedService.REGISTERED :
                return "registered";
            case ProvidedService.UNREGISTERED :
                return "unregistered";
            default :
                return "unknown (" + state + ")";
        }
    }

    private final class RequestInfo {
        /**
         * Name of the required element.
         */
        public final String m_name;
        /**
         * Type of the required element.
         */
        public final String m_type;
        /**
         * Is 'all' elements of the type required.
         */
        public final boolean m_all;

        /**
         * Creates a RequestInfo.
         * This constructor parses the parameter of the request.
         * @param request the request
         */
        protected RequestInfo(final HttpServletRequest request) {
            String factory = request.getParameter(FACTORY);
            String instance = request.getParameter(INSTANCE);
            String handler = request.getParameter(HANDLER);

            if (factory != null) {
                m_type = FACTORY;
                m_name = factory;
            } else if (instance != null) {
                m_type = INSTANCE;
                m_name = instance;
            } else if (handler != null) {
                m_type = HANDLER;
                m_name = handler;
            } else {
                m_type = INSTANCE;
                m_name = ALL;
            }

            if (ALL.equals(m_name)) {
                m_all = true;
            } else {
                m_all = false;
            }

            request.setAttribute(IPOJOServlet.class.getName(), this);
        }

        /**
         * toString method.
         * @return the String form of the request.
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return "Request: " + m_type + "=" + m_name;
        }

    }

}
