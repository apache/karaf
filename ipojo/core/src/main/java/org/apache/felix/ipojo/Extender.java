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
package org.apache.felix.ipojo;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

/**
 * iPOJO Extender.
 * This class listens bundle arrivals and departures in order to detect and manage
 * iPOJO powered bundles. This class creates factories and ask for instance creation.
 * @see SynchronousBundleListener
 * @see BundleActivator
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Extender implements SynchronousBundleListener, BundleActivator {

    /**
     * Enables the iPOJO internal dispatcher.
     * This internal dispatcher helps the OSGi framework to support large
     * scale applications. The internal dispatcher is disabled by default.
     */
    static boolean DISPATCHER_ENABLED = true;
    
    /**
     * Property allowing to set if the internal dispatcher is enabled or disabled.
     * Possible value are either <code>true</code> or <code>false</code>.
     */
    private static final String ENABLING_DISPATCHER = "ipojo.internal.dispatcher";
    
    /**
     * iPOJO Component Type and Instance declaration header.
     */
    private static final String IPOJO_HEADER = "iPOJO-Components";

    /**
     * iPOJO Extension declaration header. 
     */
    private static final String IPOJO_EXTENSION = "IPOJO-Extension";

    /**
     * The Bundle Context of the iPOJO Core bundle.
     */
    private static BundleContext m_context;
    
    /**
     * The iPOJO Extender logger.
     */
    private Logger m_logger;

    /**
     * The instance creator used to create instances.
     * (Singleton)
     */
    private InstanceCreator m_creator;

    /**
     * The iPOJO Bundle.
     */
    private Bundle m_bundle;

    /**
     * The list of factory types.
     */
    private List m_factoryTypes = new ArrayList();

    /**
     * The list of unbound types.
     * A type is unbound if the matching extension is not deployed.
     */
    private final List m_unboundTypes = new ArrayList();
    
    /**
     * The thread analyzing arriving bundles and creating iPOJO contributions.
     */
    private final CreatorThread m_thread = new CreatorThread();

    /**
     * Bundle Listener Notification.
     * @param event the bundle event.
     * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
     */
    public void bundleChanged(final BundleEvent event) {
        if (event.getBundle() == m_bundle) { return; }

        switch (event.getType()) {
            case BundleEvent.STARTED:
                // Put the bundle in the queue
                m_thread.addBundle(event.getBundle());
                break;
            case BundleEvent.STOPPING:
                m_thread.removeBundle(event.getBundle());
                closeManagementFor(event.getBundle()); //TODO Should be done in another thread
                break;
            default:
                break;
        }

    }

    /**
     * Ends the iPOJO Management for the given bundle. 
     * Generally the bundle is leaving. This method
     * stops every factories declared is the bundle and 
     * disposed every declared instances.
     * @param bundle the bundle.
     */
    private void closeManagementFor(Bundle bundle) {
        List toRemove = new ArrayList();
        // Delete instances declared in the leaving bundle.
        m_creator.removeInstancesFromBundle(bundle.getBundleId());
        for (int k = 0; k < m_factoryTypes.size(); k++) {
            ManagedAbstractFactoryType mft = (ManagedAbstractFactoryType) m_factoryTypes.get(k);

            // Look for component type created from this bundle.
            if (mft.m_created != null) {
                List cfs = (List) mft.m_created.remove(bundle);
                for (int i = 0; cfs != null && i < cfs.size(); i++) {
                    IPojoFactory factory = (IPojoFactory) cfs.get(i);
                    m_creator.removeFactory(factory);
                    factory.stop();
                }
            }

            // If the leaving bundle has declared mft : destroy all created factories.
            if (mft.m_bundle == bundle) {
                if (mft.m_created != null) {
                    Iterator iterator = mft.m_created.keySet().iterator();
                    while (iterator.hasNext()) {
                        Bundle key = (Bundle) iterator.next();
                        List list = (List) mft.m_created.get(key);
                        for (int i = 0; i < list.size(); i++) {
                            IPojoFactory factory = (IPojoFactory) list.get(i);
                            factory.stop();
                            m_unboundTypes.add(new UnboundComponentType(mft.m_type, factory.m_componentMetadata, factory.getBundleContext()
                                    .getBundle()));
                        }
                    }
                }
                toRemove.add(mft);
            }
        }

        for (int i = 0; i < toRemove.size(); i++) {
            ManagedAbstractFactoryType mft = (ManagedAbstractFactoryType) toRemove.get(i);
            m_logger.log(Logger.INFO, "The factory type: " + mft.m_type + " is no more available");
            mft.m_bundle = null;
            mft.m_clazz = null;
            mft.m_created = null;
            mft.m_type = null;
            m_factoryTypes.remove(mft);
        }
    }

    /**
     * Checks if the given bundle is an iPOJO bundle, and begin 
     * the iPOJO management is true.
     * @param bundle the bundle to check.
     */
    private void startManagementFor(Bundle bundle) {
        Dictionary dict = bundle.getHeaders();
        // Check for abstract factory type
        String typeHeader = (String) dict.get(IPOJO_EXTENSION);
        if (typeHeader != null) {
            parseAbstractFactoryType(bundle, typeHeader);
        }

        // Check bundle
        String header = (String) dict.get(IPOJO_HEADER);
        if (header != null) {
            try {
                parse(bundle, header);
            } catch (IOException e) {
                m_logger.log(Logger.ERROR, "An exception occurs during the parsing of the bundle " + bundle.getBundleId(), e);
            } catch (ParseException e) {
                m_logger.log(Logger.ERROR, "A parse exception occurs during the parsing of the bundle " + bundle.getBundleId(), e);
            }
        }
    }

    /**
     * Parses an IPOJO-Extension manifest header and then creates
     * iPOJO extensions (factory types).
     * @param bundle the bundle containing the header.
     * @param header the header to parse.
     */
    private void parseAbstractFactoryType(Bundle bundle, String header) {
        String[] arr = ParseUtils.split(header, ",");
        for (int i = 0; arr != null && i < arr.length; i++) {
            String[] arr2 = ParseUtils.split(arr[i], ":");
            String type = arr2[0];
            Class clazz;
            try {
                clazz = bundle.loadClass(arr2[1]);
            } catch (ClassNotFoundException e) {
                m_logger.log(Logger.ERROR, "Cannot load the extension " + type, e);
                return;
            }
            ManagedAbstractFactoryType mft = new ManagedAbstractFactoryType(clazz, type, bundle);
            m_factoryTypes.add(mft);
            m_logger.log(Logger.DEBUG, "New factory type available: " + type);

            for (int j = m_unboundTypes.size() - 1; j >= 0; j--) {
                UnboundComponentType unbound = (UnboundComponentType) m_unboundTypes.get(j);
                if (unbound.m_type.equals(type)) {
                    createAbstractFactory(unbound.m_bundle, unbound.m_description);
                    m_unboundTypes.remove(unbound);
                }
            }
        }
    }

    /**
     * Parses the internal metadata (from the manifest 
     * (in the iPOJO-Components property)). This methods
     * creates factories and add instances to the instance creator.
     * @param bundle the owner bundle.
     * @param components The iPOJO Header String.
     * @throws IOException if the manifest can not be found
     * @throws ParseException if the parsing process failed
     */
    private void parse(Bundle bundle, String components) throws IOException, ParseException {
        ManifestMetadataParser parser = new ManifestMetadataParser();
        parser.parseHeader(components);

        Element[] metadata = parser.getComponentsMetadata(); // Get the component type declaration
        for (int i = 0; i < metadata.length; i++) {
            createAbstractFactory(bundle, metadata[i]);
        }

        Dictionary[] instances = parser.getInstances();
        for (int i = 0; instances != null && i < instances.length; i++) {
            m_creator.addInstance(instances[i], bundle.getBundleId());
        }
    }

    /**
     * iPOJO Start method.
     * @param context the iPOJO bundle context.
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) {
        m_context = context;
        m_bundle = context.getBundle();
        m_creator = new InstanceCreator(context);

        m_logger = new Logger(m_context, "IPOJO-Extender");
        
        enablingDispatcher(context, m_logger);
        
        // Create the dispatcher only if required.
        if (DISPATCHER_ENABLED) {
            EventDispatcher.create(context);
        }
        
        // Begin by initializing core handlers
        startManagementFor(m_bundle);
        
        new Thread(m_thread).start();

        synchronized (this) {
            // listen to any changes in bundles.
            m_context.addBundleListener(this);
            // compute already started bundles.
            for (int i = 0; i < context.getBundles().length; i++) {
                if (context.getBundles()[i].getState() == Bundle.ACTIVE) {
                    m_thread.addBundle(context.getBundles()[i]); // Bundles are processed in another thread.
                }
            }
        }
        
        m_logger.log(Logger.INFO, "iPOJO Runtime started");
    }

    /**
     * Stops the iPOJO Bundle.
     * @param context the bundle context.
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) {
        m_thread.stop(); // Stop the thread processing bundles.
        m_context.removeBundleListener(this);
        
        if (DISPATCHER_ENABLED) {
            EventDispatcher.dispose();
        }

        for (int k = 0; k < m_factoryTypes.size(); k++) {
            ManagedAbstractFactoryType mft = (ManagedAbstractFactoryType) m_factoryTypes.get(k);

            if (mft.m_created != null) {
                Iterator iterator = mft.m_created.keySet().iterator();
                while (iterator.hasNext()) {
                    Bundle key = (Bundle) iterator.next();
                    List list = (List) mft.m_created.get(key);
                    for (int i = 0; i < list.size(); i++) {
                        IPojoFactory factory = (IPojoFactory) list.get(i);
                        m_creator.removeFactory(factory);
                        factory.dispose();
                    }
                }
            }
        }

        m_factoryTypes = null;
        m_creator = null;
        
        m_logger.log(Logger.INFO, "iPOJO Runtime stopped");
        m_context = null;
    }
    
    /**
     * Gets iPOJO bundle context.
     * @return the iPOJO Bundle Context
     */
    public static BundleContext getIPOJOBundleContext() {
        return m_context;
    }
    
    /**
     * Enables or disables the internal dispatcher, so sets the
     * {@link Extender#DISPATCHER_ENABLED} flag.
     * This method checks if the {@link Extender#ENABLING_DISPATCHER}
     * property is set to <code>true</code>. Otherwise, the internal
     * dispatcher is disabled. The property can be set as a system
     * property (<code>ipojo.internal.dispatcher</code>) or inside the
     * iPOJO bundle manifest (<code>ipojo-internal-dispatcher</code>).
     * @param context the bundle context.
     * @param logger the logger to indicates if the internal dispatcher is set.
     */
    private static void enablingDispatcher(BundleContext context, Logger logger) {
        // First check in the framework and in the system properties
        String flag = context.getProperty(ENABLING_DISPATCHER);
        
        // If null, look in bundle manifest
        if (flag == null) {
            String key = ENABLING_DISPATCHER.replace('.', '-');
            flag = (String) context.getBundle().getHeaders().get(key);
        }
        
        if (flag != null) {
            if (flag.equalsIgnoreCase("true")) {
                Extender.DISPATCHER_ENABLED = true;
                logger.log(Logger.INFO, "iPOJO Internal Event Dispatcher enables");
                return;
            }
        }
        
        // Either l is null, or the specified value was false
        Extender.DISPATCHER_ENABLED = false;
        logger.log(Logger.INFO, "iPOJO Internal Event Dispatcher disables");
        
    }

    /**
     * Adds a component factory to the factory list.
     * @param metadata the new component metadata.
     * @param bundle the bundle.
     */
    private void createAbstractFactory(Bundle bundle, Element metadata) {
        ManagedAbstractFactoryType factoryType = null;
        // First, look for factory-type (component, handler, composite ...)
        for (int i = 0; i < m_factoryTypes.size(); i++) {
            ManagedAbstractFactoryType type = (ManagedAbstractFactoryType) m_factoryTypes.get(i);
            if (type.m_type.equals(metadata.getName())) {
                factoryType = type;
                break;
            }
        }

        // If not found, return. It will wait for a new component type factory.
        if (factoryType == null) {
            m_logger.log(Logger.WARNING, "Type of component not available: " + metadata.getName());
            m_unboundTypes.add(new UnboundComponentType(metadata.getName(), metadata, bundle));
            return;
        }

        // Once found, we invoke the AbstractFactory constructor to create the component factory. 
        Class clazz = factoryType.m_clazz;
        try {
            // Look for the constructor, and invoke it.
            Constructor cst = clazz.getConstructor(new Class[] { BundleContext.class, Element.class });
            IPojoFactory factory = (IPojoFactory) cst.newInstance(new Object[] { getBundleContext(bundle), metadata });

            // Add the created factory in the m_createdFactories map.
            if (factoryType.m_created == null) {
                factoryType.m_created = new HashMap();
                List list = new ArrayList();
                list.add(factory);
                factoryType.m_created.put(bundle, list);
            } else {
                List list = (List) factoryType.m_created.get(bundle);
                if (list == null) {
                    list = new ArrayList();
                    list.add(factory);
                    factoryType.m_created.put(bundle, list);
                } else {
                    list.add(factory);
                }
            }

            // Start the created factory.
            factory.start();
            // Then add the factory to the instance creator.
            m_creator.addFactory(factory);

        } catch (SecurityException e) {
            m_logger.log(Logger.ERROR, "Cannot instantiate an abstract factory from " + clazz.getName(), e);
        } catch (NoSuchMethodException e) {
            m_logger.log(Logger.ERROR, "Cannot instantiate an abstract factory from " + clazz.getName() + ": the given class constructor cannot be found");
        } catch (IllegalArgumentException e) {
            m_logger.log(Logger.ERROR, "Cannot instantiate an abstract factory from " + clazz.getName(), e);
        } catch (InstantiationException e) {
            m_logger.log(Logger.ERROR, "Cannot instantiate an abstract factory from " + clazz.getName(), e);
        } catch (IllegalAccessException e) {
            m_logger.log(Logger.ERROR, "Cannot instantiate an abstract factory from " + clazz.getName(), e);
        } catch (InvocationTargetException e) {
            m_logger.log(Logger.ERROR, "Cannot instantiate an abstract factory from " + clazz.getName(), e.getTargetException());
        }
    }

    /**
     * Structure storing an iPOJO extension.
     */
    private static final class ManagedAbstractFactoryType {
        /**
         * The type (i.e.) name of the extension.
         */
        String m_type;

        /**
         * The abstract Factory class.
         */
        Class m_clazz;

        /**
         * The bundle object containing the declaration of the extension.
         */
        Bundle m_bundle;

        /**
         * The factories created by this extension. 
         */
        private Map m_created;

        /**
         * Creates a ManagedAbstractFactoryType.
         * @param factory the abstract factory class.
         * @param type the name of the extension.
         * @param bundle the bundle declaring the extension.
         */
        protected ManagedAbstractFactoryType(Class factory, String type, Bundle bundle) {
            m_bundle = bundle;
            m_clazz = factory;
            m_type = type;
        }
    }

    /**
     * Structure storing unbound component type declarations.
     * Unbound means that there is no extension able to manage the extension.
     */
    private static final class UnboundComponentType {
        /**
         * The component type description.
         */
        private final Element m_description;

        /**
         * The bundle declaring this type.
         */
        private final Bundle m_bundle;

        /**
         * The required extension name.
         */
        private final String m_type;

        /**
         * Creates a UnboundComponentType.
         * @param description the description of the component type.
         * @param bundle the bundle declaring this type.
         * @param type the required extension name.
         */
        protected UnboundComponentType(String type, Element description, Bundle bundle) {
            m_type = type;
            m_description = description;
            m_bundle = bundle;
        }
    }

    /**
     * Computes the bundle context from the bundle class by introspection.
     * @param bundle the bundle.
     * @return the bundle context object or <code>null</code> if not found.
     */
    public BundleContext getBundleContext(Bundle bundle) {
        if (bundle == null) { return null; }

        // getBundleContext (OSGi 4.1)
        Method meth = null;
        try {
            meth = bundle.getClass().getMethod("getBundleContext", new Class[0]); // This method is public and is specified in the Bundle interface.
        } catch (SecurityException e) {
            // Nothing do to, will try the Equinox method
        } catch (NoSuchMethodException e) {
            // Nothing do to, will try the Equinox method
        }

        // try Equinox getContext if not found.
        if (meth == null) {
            try {
                meth = bundle.getClass().getMethod("getContext", new Class[0]);
            } catch (SecurityException e) {
                // Nothing do to, will try field inspection
            } catch (NoSuchMethodException e) {
                // Nothing do to, will try field inspection
            }
        }

        if (meth != null) {
            if (! meth.isAccessible()) { 
                // If not accessible, try to set the accessibility.
                meth.setAccessible(true);
            }
            try {
                return (BundleContext) meth.invoke(bundle, new Object[0]);
            } catch (IllegalArgumentException e) {
                m_logger.log(Logger.ERROR, "Cannot get the BundleContext by invoking " + meth.getName(), e);
                return null;
            } catch (IllegalAccessException e) {
                m_logger.log(Logger.ERROR, "Cannot get the BundleContext by invoking " + meth.getName(), e);
                return null;
            } catch (InvocationTargetException e) {
                m_logger.log(Logger.ERROR, "Cannot get the BundleContext by invoking " + meth.getName(), e.getTargetException());
                return null;
            }
        }

        // Else : Field inspection (KF and Prosyst)        
        Field[] fields = bundle.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            if (BundleContext.class.isAssignableFrom(fields[i].getType())) {
                if (!fields[i].isAccessible()) {
                    fields[i].setAccessible(true);
                }
                try {
                    return (BundleContext) fields[i].get(bundle);
                } catch (IllegalArgumentException e) {
                    m_logger.log(Logger.ERROR, "Cannot get the BundleContext by invoking " + fields[i].getName(), e);
                    return null;
                } catch (IllegalAccessException e) {
                    m_logger.log(Logger.ERROR, "Cannot get the BundleContext by invoking " + fields[i].getName(), e);
                    return null;
                }
            }
        }
        m_logger.log(Logger.ERROR, "Cannot find the BundleContext for " + bundle.getSymbolicName(), null);
        return null;
    }
    

    /**
     * The creator thread analyzes arriving bundles to create iPOJO contribution.
     */
    private class CreatorThread implements Runnable {

        /**
         * Is the creator thread started?
         */
        private boolean m_started = true;
        
        /**
         * The list of bundle that are going to be analyzed.
         */
        private List m_bundles = new ArrayList();
        
        /**
         * A bundle is arriving.
         * This method is synchronized to avoid concurrent modification of the waiting list.
         * @param bundle the new bundle
         */
        public synchronized void addBundle(Bundle bundle) {
            m_bundles.add(bundle);
            notifyAll(); // Notify the thread to force the process.
            m_logger.log(Logger.DEBUG, "Creator thread is going to analyze the bundle " + bundle.getBundleId() + " List : " + m_bundles);
        }
        
        /**
         * A bundle is leaving.
         * If the bundle was not already processed, the bundle is remove from the waiting list.
         * This method is synchronized to avoid concurrent modification of the waiting list.
         * @param bundle the leaving bundle.
         */
        public synchronized void removeBundle(Bundle bundle) {
            m_bundles.remove(bundle);
        }
        
        /**
         * Stops the creator thread.
         */
        public synchronized void stop() {
            m_started = false;
            m_bundles.clear();
            notifyAll();
        }

        /**
         * Creator thread's run method.
         * While the list is not empty, the thread launches the bundle analyzing on the next bundle.
         * When the list is empty, the thread sleeps until the arrival of a new bundle 
         * or until iPOJO stops.
         * @see java.lang.Runnable#run()
         */
        public void run() {
            m_logger.log(Logger.DEBUG, "Creator thread is starting");
            boolean started;
            synchronized (this) {
                started = m_started;
            }
            while (started) {
                Bundle bundle;
                synchronized (this) {
                    while (m_started && m_bundles.isEmpty()) {
                        try {
                            m_logger.log(Logger.DEBUG, "Creator thread is waiting - Nothing to do");
                            wait();
                        } catch (InterruptedException e) {
                            // Interruption, re-check the condition
                        }
                    }
                    if (!m_started) {
                        m_logger.log(Logger.DEBUG, "Creator thread is stopping");
                        return; // The thread must be stopped immediately.
                    } else {
                        // The bundle list is not empty, get the bundle.
                        // The bundle object is collected inside the synchronized block to avoid
                        // concurrent modification. However the real process is made outside the
                        // mutual exclusion area
                        bundle = (Bundle) m_bundles.remove(0);
                    }
                }
                // Process ...
                m_logger.log(Logger.DEBUG, "Creator thread is processing " + bundle.getBundleId());
                try {
                    startManagementFor(bundle);
                } catch (Throwable e) {
                    // To be sure to not kill the thread, we catch all exceptions and errors
                    m_logger.log(Logger.ERROR, "An error occurs when analyzing the content or starting the management of " + bundle.getBundleId(), e);
                }
                synchronized (this) {
                    started = m_started;
                }
            }
        }

    }

}
