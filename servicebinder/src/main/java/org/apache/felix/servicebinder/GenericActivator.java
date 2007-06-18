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
package org.apache.felix.servicebinder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.io.InputStream;

import org.apache.felix.servicebinder.architecture.DependencyChangeEvent;
import org.apache.felix.servicebinder.architecture.InstanceChangeEvent;
import org.apache.felix.servicebinder.impl.ArchitectureServiceImpl;
import org.apache.felix.servicebinder.parser.KxmlParser;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;


import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * The GenericActivator, it will read information from the metadata.xml file
 * and will create the corresponding instance managers
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
abstract public class GenericActivator implements BundleActivator
{
    private BundleContext m_context = null;
    private List m_instanceManagers = new ArrayList();

    private static boolean m_trace = false;
    private static boolean m_error = true;

    private static String m_version = "1.1.1 (17062004)";

    // Static initializations based on system properties
    static {
        // Get system properties to see if traces or errors need to be displayed
        String result = System.getProperty("servicebinder.showtrace");
        if(result != null && result.equals("true"))
        {
            m_trace = true;
        }
        result = System.getProperty("servicebinder.showerrors");
        if(result != null && result.equals("false"))
        {
            m_error = false;
        }
        result = System.getProperty("servicebinder.showversion");
        if(result != null && result.equals("true"))
        {
            System.out.println("[ ServiceBinder version = "+m_version+" ]\n");
        }        
    }

    public GenericActivator()
    {
    }

    /**
    * Called upon starting of the bundle. This method invokes initialize() which
    * parses the meta data and creates the instance managers
    *
    * @param   context  The bundle context passed by the framework
    * @exception   Exception any exception thrown from initialize
    */
    public void start(BundleContext context) throws Exception
    {
        m_context = context;
        try
        {
            initialize();
        }
        catch (Exception e)
        {
            GenericActivator.error("GenericActivator : in bundle ["
                + context.getBundle().getBundleId() + "] : " + e);
            e.printStackTrace();
            throw e;
        }
    }

    /**
    * Gets the MetaData location, parses the meta data and requests the processing
    * of binder instances
    *
    * @throws java.io.FileNotFoundException if the metadata file is not found
    * @throws javax.xml.parsers.ParserConfigurationException
    * @throws org.xml.sax.SAXException
    * @throws java.io.IOException
    * @throws java.lang.ClassNotFoundException it the instance class is not found
    * @throws java.lang.NoSuchMethodException if binder methods are not found
    * @throws org.osgi.framework.InvalidSyntaxException if the filter has an incorrect syntax
    * @throws Exception if any exception is thrown during the validation of the InstanceManagers
    */
    private void initialize() throws Exception
    {
        // Get the Metadata-Location value from the manifest

        String metadataLocation = "";
        //Dictionary dict = m_context.getBundle().getHeaders();
        
        metadataLocation = (String) m_context.getBundle().getHeaders().get("Metadata-Location");

        if (metadataLocation == null)
        {
            throw new java.io.FileNotFoundException("Metadata-Location entry not found in the manifest");
        }

        if (metadataLocation.startsWith("/") == false)
        {
            metadataLocation="/"+metadataLocation;
        }

        InputStream stream = getClass().getResourceAsStream(metadataLocation);

        if (stream == null)
        {
            throw new java.io.FileNotFoundException("MetaData file not found at:"+metadataLocation);
        }

        // Create the parser

        XmlHandler handler = new XmlHandler();
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        KxmlParser parser = new KxmlParser(in);

        parser.parseXML(handler);

        // Create An instance manager for every entry

        Iterator i = handler.getInstanceMetadatas().iterator();

        while (i.hasNext())
        {
            InstanceMetadata descriptor = (InstanceMetadata) i.next();
                        
            if(descriptor.isFactory()) 
            {
                // NOT YET SUPPORTED IN THIS VERSION
            }
            else // deployment instance
            {
                // create the instance manager
                InstanceManager currentinstance = new InstanceManager(this,descriptor);
                // start managing lifecycle
                currentinstance.validate();
            }
        }
    }

    /**
    * Stop method that destroys all the instance managers
    *
    * @param   context The Bundle Context passed by the framework
    * @exception Exception any exception thrown during destruction of the instance managers
    */
    public void stop(BundleContext context) throws java.lang.Exception
    {
        GenericActivator.trace("GenericActivator : Bundle ["+context.getBundle().getBundleId()+"] will destroy "+m_instanceManagers.size()+" instances");

        while (m_instanceManagers.size() !=0 )
        {
            InstanceManager current = (InstanceManager)m_instanceManagers.get(0);
            try
            {
                current.destroy();
            }
            catch(Exception e)
            {
                GenericActivator.error("GenericActivator : Exception during invalidate : "+e);
                e.printStackTrace();
            }
        }

        m_context = null;

        GenericActivator.trace("GenericActivator : Bundle ["+context.getBundle().getBundleId()+"] STOPPED");
    }

    /**
    * Returns the list of instance references currently associated to this activator
    *
    * @return the list of instance references
    */
    protected List getInstanceReferences()
    {
        return m_instanceManagers;
    }

    /**
    * Returns the BundleContext
    *
    * @return the BundleContext
    */
    protected BundleContext getBundleContext()
    {
        return m_context;
    }

    /**
    * Add an instance manager to the list
    *
    * @param instance an instance manager
    */
    synchronized void addInstanceManager(InstanceManager instance)
    {
        ArchitectureServiceImpl.addInstanceReference(instance);

        m_instanceManagers.add(instance);
    }

    /**
    * Removes a binder instance from the list
    *
    * @param instance an instance manager
    */
    synchronized void removeInstanceManager(InstanceManager instance)
    {
        ArchitectureServiceImpl.removeInstanceReference(instance);

        m_instanceManagers.remove(instance);
    }

    /**
     * Method to display traces
     *
     * @param s a string to be displayed
    **/
    static void trace(String s)
    {
        if(m_trace)
        {
            System.out.println("--- "+s);
        }
    }

    /**
     * Method to display errors
     *
     * @param s a string to be displayed
    **/
    static void error(String s)
    {
        if(m_error)
        {
            System.err.println("### "+s);
        }
    }

    /**
     * Method called before an object implementing services is registered
     * in the OSGi framework. This method is provided so that subclasses of
     * the generic activator may proxy the object. The default implementation
     * returns the passed in object.
     *
     * @param obj the instance object
     * @param descriptor the instance descriptor that provides information relevant to
     *          the instance object
    **/
    protected Object proxyProvidedServiceObject(Object obj, InstanceMetadata descriptor)
    {
        return obj;
    }

    /**
     * Method called before the binding of the service object occurs.
     * This method is provided so that subclasses of the generic activator
     * may proxy the object. The default implementation returns the passed in object.
     *
     * @param obj the instance object
     * @param descriptor the dependency descriptor that provides information relevant to
     *          the service object
    **/
    protected Object proxyRequiredServiceObject(Object obj, DependencyMetadata descriptor)
    {
        return obj;
    }

    /**
     * Fires an event when an instance has changed
     * The generic activator always requests a reference of EvtGeneratorImpl
     * since this class can be instantiated at anytime since it is
     * the service implementation.
     *
     * @evt The InstanceChangeEvent
    **/
    void fireInstanceChangeEvent(InstanceChangeEvent evt)
    {
        ArchitectureServiceImpl evtGenerator = ArchitectureServiceImpl.getReference();

        if(evtGenerator != null)
        {
            evtGenerator.fireInstanceChangeEvent(evt);
        }
    }

    /**
     * Fires an event when a dependency has changed
     *
     * @evt The InstanceChangeEvent
     **/
    void fireDependencyChangeEvent(DependencyChangeEvent evt)
    {
        ArchitectureServiceImpl evtGenerator = ArchitectureServiceImpl.getReference();

        if(evtGenerator != null)
        {
            evtGenerator.fireDependencyChangeEvent(evt);
        }
    }
}
