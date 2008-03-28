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
package org.apache.felix.ipojo.composite.util;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.ContextListener;
import org.apache.felix.ipojo.ContextSource;
import org.apache.felix.ipojo.composite.CompositeManager;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.apache.felix.ipojo.util.DependencyModel;
import org.apache.felix.ipojo.util.Tracker;
import org.apache.felix.ipojo.util.TrackerCustomizer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * This class manages context-source management.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class SourceManager implements ContextListener {

    /**
     * Source Name service property.
     */
    public static final String SOURCE_NAME = "source.name";

    /**
     * Managed dependency.
     */
    private DependencyModel m_dependency;

    /**
     * List of monitored context sources.
     */
    private List/* <ContextSource> */m_sources = new ArrayList(1);

    /**
     * PRoperties contained in the original filter.
     */
    private String[] m_properties;

    /**
     * Original filter (containing variables). 
     */
    private String m_filter;

    /**
     * Bundle context.
     */
    private BundleContext m_context;

    /**
     * Service Tracker List.
     */
    private List/*<SourceTracker>*/m_trackers = new ArrayList(1);

    /**
     * Constructor.
     * @param sources : context-source attribute from the dependency metadata
     * @param depfilter : original dependency filter
     * @param dependency : dependency object
     * @param manager : composite manager
     * @throws ConfigurationException : the sources are incorrect.
     */
    public SourceManager(String sources, String depfilter, DependencyModel dependency, CompositeManager manager) throws ConfigurationException {
        m_filter = depfilter;
        m_properties = getProperties(depfilter);
        m_dependency = dependency;
        m_context = manager.getGlobalContext();
        if (manager.getParentServiceContext() == null) {
            // The parent is the global context
            parseSources(sources, manager.getGlobalContext(), manager.getGlobalContext(), manager.getServiceContext());
        } else {
            parseSources(sources, manager.getGlobalContext(), manager.getParentServiceContext(), manager.getServiceContext());
        }
    }

    /**
     * Start the context management.
     */
    public void start() {
        for (int i = 0; i < m_trackers.size(); i++) {
            ((SourceTracker) m_trackers.get(i)).open();
        }
        computeFilter();
    }

    /**
     * Stop the context management.
     */
    public void stop() {
        for (int i = 0; i < m_trackers.size(); i++) {
            ((SourceTracker) m_trackers.get(i)).close();
        }
        setFilter(m_filter);
        m_sources.clear();
    }

    /**
     * Get the state of this source manager.
     * @return the state of this source manager.
     */
    public int getState() {
        if (m_sources.isEmpty()) {
            return DependencyModel.UNRESOLVED;
        } else {
            return DependencyModel.RESOLVED;
        }
    }

    /**
     * Set the filter of the managed dependency.
     * @param filter : the new filter to apply
     */
    private void setFilter(String filter) {
        if (!filter.equals(m_dependency.getFilter())) {
            // Reconfigure
            try {
                m_dependency.setFilter(m_context.createFilter(filter));
            } catch (InvalidSyntaxException e) {
                throw new IllegalStateException("A context filter is invalid : " + filter);
            }
        }
    }

    /**
     * Compute the new filter.
     */
    private void computeFilter() {
        String fil = m_filter;
        synchronized (this) {
            for (int i = 0; i < m_sources.size(); i++) {
                Dictionary props = ((ContextSource) m_sources.get(i)).getContext();
                fil = substitute(fil, props); //NOPMD
            }
        }
        if (!fil.equals(m_dependency.getFilter())) {
            setFilter(fil);
        }
    }

    /**
     * This method substitute ${var} substring by values stored in a map.
     * @param str : string with variables
     * @param values : dictionary containing the variable name and the value.
     * @return resulted string
     */
    public static String substitute(String str, Dictionary values) {       
        int len = str.length();
        StringBuffer buffer = new StringBuffer(len);

        int prev = 0;
        int start = str.indexOf("${");
        int end = str.indexOf('}', start);
        while (start != -1 && end != -1) {
            String key = str.substring(start + 2, end);
            Object value = values.get(key);
            if (value == null) {
                buffer.append(str.substring(prev, end + 1));
            } else {
                buffer.append(str.substring(prev, start));
                buffer.append(value);
            }
            prev = end + 1;
            if (prev >= str.length()) {
                break;
            }

            start = str.indexOf("${", prev);
            if (start != -1) {
                end = str.indexOf('}', start);
            }
        }

        buffer.append(str.substring(prev));

        return buffer.toString();
    }

    /**
     * Compute the properties (${name}) from the given filter.
     * @param str : string form of the filter.
     * @return the list of found properties.
     */
    public static String[] getProperties(String str) {
        List list = new ArrayList();
        int prev = 0;
        int start = str.indexOf("${");
        int end = str.indexOf('}', start);
        while (start != -1 && end != -1) {
            String key = str.substring(start + 2, end);
            list.add(key);
            prev = end + 1;
            if (prev >= str.length()) {
                break;
            }

            start = str.indexOf("${", prev);
            if (start != -1) {
                end = str.indexOf('}', start);
            }
        }

        return (String[]) list.toArray(new String[list.size()]);
    }

    /**
     * A context source has modified a monitored property. 
     * @param source : source
     * @param property : modified property
     * @param value : new value.
     * @see org.apache.felix.ipojo.ContextListener#update(org.apache.felix.ipojo.ContextSource, java.lang.String, java.lang.Object)
     */
    public synchronized void update(ContextSource source, String property, Object value) {
        computeFilter();
    }

    /**
     * Parse the context-source attribute in order to create source tracker object.
     * @param sourceAtt : context-source attribute.
     * @param global : global bundle context.
     * @param parent : parent bundle context.
     * @param local : local bundle context.
     * @throws ConfigurationException : the context-source attribute is invalid.
     */
    private void parseSources(String sourceAtt, BundleContext global, BundleContext parent, BundleContext local) throws ConfigurationException {
        String[] sources = ParseUtils.split(sourceAtt, ",");
        for (int i = 0; i < sources.length; i++) {
            String[] srcs = ParseUtils.split(sources[i], ":");
            if (srcs.length == 1) {
                // No prefix use local. //TODO choose default case.
                SourceTracker tracker = new SourceTracker(srcs[0], local);
                m_trackers.add(tracker);
            } else if (srcs.length == 2) {
                // According to prefix add the source in the good list.
                if (srcs[0].equalsIgnoreCase("parent")) {
                    SourceTracker tracker = new SourceTracker(srcs[1], parent);
                    m_trackers.add(tracker);
                } else if (srcs[0].equalsIgnoreCase("local")) {
                    SourceTracker tracker = new SourceTracker(srcs[1], local);
                    m_trackers.add(tracker);
                } else if (srcs[0].equalsIgnoreCase("global")) {
                    SourceTracker tracker = new SourceTracker(srcs[1], global);
                    m_trackers.add(tracker);
                } else {
                    throw new ConfigurationException("Unknowns context scope : " + srcs[0]);
                }
            } else {
                throw new ConfigurationException("Malformed context source : " + sources[i]);
            }
        }
    }

    /**
     * A context source appears.
     * @param source : new context source.
     */
    private void addContextSource(ContextSource source) {
        m_sources.add(source);
        computeFilter();
        source.registerContextListener(this, m_properties);
    }

    /**
     * A context source disappears.
     * @param source : leaving context source.
     */
    private void removeContextSource(ContextSource source) {
        m_sources.remove(source);
        computeFilter();
    }

    private class SourceTracker implements TrackerCustomizer {

        /**
         * Service tracker.
         */
        private Tracker m_tracker;

        /**
         * Constructor.
         * @param name : name of the required context-source.
         * @param countext : bundle context to use.
         * @throws ConfigurationException : the context-source name is invalid.
         */
        public SourceTracker(String name, BundleContext countext) throws ConfigurationException {
            String fil = "(&(" + Constants.OBJECTCLASS + "=" + ContextSource.class.getName() + ")(" + SOURCE_NAME + "=" + name + "))";
            try {
                Filter filter = countext.createFilter(fil);
                m_tracker = new Tracker(countext, filter, this);
            } catch (InvalidSyntaxException e) {
                throw new ConfigurationException("A Context source filter is invalid " + fil + " : " + e.getMessage());
            }
        }

        /**
         * Open the tracker.
         */
        public void open() {
            m_tracker.open();
        }

        /**
         * Close the tracker.
         */
        public void close() {
            m_tracker.close();
        }

        /**
         * A new context-source was added.
         * This method inject the context-source object in the source manager.
         * @param reference : service reference.
         * @see org.apache.felix.ipojo.util.TrackerCustomizer#addedService(org.osgi.framework.ServiceReference)
         */
        public void addedService(ServiceReference reference) {
            addContextSource((ContextSource) m_tracker.getService(reference));
        }

        /**
         * A new context-source is adding in the tracker.. 
         * @param reference : service reference
         * @return true.
         * @see org.apache.felix.ipojo.util.TrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
         */
        public boolean addingService(ServiceReference reference) {
            return true;
        }

        /**
         * A used context-source is modified.
         * @param reference : service reference.
         * @param service : service object.
         * @see org.apache.felix.ipojo.util.TrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
         */
        public void modifiedService(ServiceReference reference, Object service) {
            // Nothing to do.
        }

        /**
         * A used context-source disappears.
         * This method notify the Source Manager in order to manage this departure. 
         * @param reference : service reference.
         * @param service : service object.
         * @see org.apache.felix.ipojo.util.TrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
         */
        public void removedService(ServiceReference reference, Object service) {
            removeContextSource((ContextSource) service);
        }

    }

}
