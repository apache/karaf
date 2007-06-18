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

import java.util.Properties;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.felix.servicebinder.architecture.Dependency;
import org.apache.felix.servicebinder.architecture.DependencyChangeEvent;
import org.apache.felix.servicebinder.architecture.Instance;
import org.apache.felix.servicebinder.architecture.InstanceChangeEvent;
import org.apache.felix.servicebinder.impl.ArchitectureServiceImpl;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Bundle;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * A InstanceManager is created for every component instance. *  * When the InstanceManager is instantiated, a collection of DependencyManagers is * created. Each dependency manager corresponds to a required service *  * A InstanceManager follows a sequence of clearly defined steps. *  * 1.- Creation : the binder instance is created, its state becomes CREATED. This step is further divided *                in the following substeps: *                  - The binder instance checks if all of the dependencies are valid, if this *                    is false, it returns. *                  - If the dependendencies are valid, its state becomes executing. The object from *                    the instance class is created (if this object receives a ServiceBinderContext as *                    a parameter in its constructor, the context is passed to it. *                  - The validate() method is called on the dependency managers, this will cause *                    calls on the binding methods to occur *                  - The binder instance adds itself to the list of binder instances in the activator *                  - The binder instance registers the services implemented by the instance object. *  * 2.- Disposal : *  * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */

public class InstanceManager implements InstanceReference, Instance
{
    // The values ranging from 0 to 3 are public and are defined in InstanceReference
    static final int INSTANCE_CREATING = 4;
    static final int INSTANCE_VALIDATING = 5;
    static final int INSTANCE_INVALIDATING = 6;
    static final int INSTANCE_DESTROYING = 7;

    static final String m_states[]={"CREATED","VALID","INVALID",
                                    "DESTROYED","CREATING","VALIDATING",
                                    "INVALIDATING","DESTROYING"};

    // The state of this instance manager
    private int m_state = INSTANCE_CREATING;

    /**
     * 
     * @uml.property name="m_instanceMetadata"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    // The metadata
    private InstanceMetadata m_instanceMetadata;


    // The object that implements the service and that is bound to other services
    private Object m_implementorObject;

    // The dependency managers that manage every dependency
    private List m_dependencyManagers;

    // The ServiceRegistration
    private ServiceRegistration m_serviceRegistration;

    /**
     * 
     * @uml.property name="m_activator"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    // A reference to the GenericActivator
    private GenericActivator m_activator;

    /**
     * 
     * @uml.property name="m_sbcontext"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    // The context that will be passed to the objects
    private ServiceBinderContextImpl m_sbcontext;

    /**
     * 
     * @uml.property name="m_instanceListener"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    // Listeners to validation events
    private InstanceReferenceListener m_instanceListener = null;

    // Properties that can be attached to te InstanceManager
    private Properties m_localProperties = new Properties();
    
    // Flag that indicates that activate was called
    private boolean m_activateCalled = false;

    /**
    * Constructor that creates a collection of dependency managers that will be in
    * charge of the different dependencies for a particular instance.
    *
    * @param   activator A reference to the generic activator
    * @param   descriptor an InstanceMetadata that contains information found in the descriptor file
    * @throws  java.lang.ClassNotFoundException if the instance class (declared in the descriptor file) is not found
    * @throws  java.lang.NoSuchMethodException if the bind or unbind methods are not found on the instance class
    * @throws  org.osgi.framework.InvalidSyntaxException if the filter declared in the requires entry has an invalid syntax
    **/
    InstanceManager(GenericActivator activator,InstanceMetadata descriptor)
        throws ClassNotFoundException, NoSuchMethodException, InvalidSyntaxException
    {
        m_activator = activator;

        m_instanceMetadata = descriptor;

        m_dependencyManagers = new ArrayList();

        if (m_instanceMetadata.getDependencies().size() != 0)
        {
            Iterator dependencyit = m_instanceMetadata.getDependencies().iterator();

            while(dependencyit.hasNext())
            {
                DependencyMetadata currentdependency = (DependencyMetadata)dependencyit.next();

                DependencyManager depmanager = new DependencyManager(currentdependency);

                m_dependencyManagers.add(depmanager);

                // Register the dependency managers as listeners to service events so that they begin
                // to manage the dependency autonomously

                m_activator.getBundleContext().addServiceListener(depmanager,depmanager.getDependencyMetadata().getFilter());
            }
        }

        m_sbcontext = new ServiceBinderContextImpl(this);

        // Add this instance manager to the Generic activator list
        m_activator.addInstanceManager(this);

        setState(INSTANCE_CREATED);
    }

    /**
    * Validate this Instance manager.
    *
    * CONCURRENCY NOTE: This method can be called either after an instance manager is created
    * or after the instance is validated again after by the instance manager itself
    */
    synchronized void validate()
    {
        if (m_state == INSTANCE_VALID)
        {
            return;
        }
        else if (m_state != INSTANCE_INVALID && m_state !=INSTANCE_CREATED)
        {
            GenericActivator.error("InstanceManager : create() called for a non INVALID or CREATED InstanceManager ("+m_states[m_state]+")");
            return;
        }

        setState(INSTANCE_VALIDATING);

        // Test if all dependency managers are valid

        Iterator it = m_dependencyManagers.iterator();

        while (it.hasNext())
        {
            // It is not possible to call the isValid method yet in the DependencyManager
            // since they have not been initialized yet, but we can't call initialize
            // since the object where bindings will be done has not been created.
            // This test is necessary, because we don't want to instantiate
            // the object if the dependency managers won't be valid.
            DependencyManager dm = (DependencyManager)it.next();
            if (dm.getRequiredServiceRefs() == null && dm.getDependencyMetadata().isOptional() == false)
            {
                setState(INSTANCE_INVALID);
                return;
            }
        }

        // everything ok to go...

        try
        {
            Class c = m_activator.getClass().getClassLoader().loadClass(m_instanceMetadata.getImplementorName());
            try
            {
                Constructor cons = c.getConstructor(new Class[] {ServiceBinderContext.class});
                m_implementorObject = cons.newInstance(new Object[] {m_sbcontext});
            }
            catch(NoSuchMethodException ex)
            {
                // Aparently he doesn't want a ServiceBinderContext...
            }

            // Create from no-param constructor
            if (m_implementorObject == null)
            {
                m_implementorObject = c.newInstance();
            }

            /* is it a factory?
            if (m_implementorObject instanceof GenericFactory)
            {
                ((GenericFactory) m_implementorObject).setActivator(m_activator, this);
            }
            */

            // Allow somebody to proxy the object through the proxyProvidedServiceObject method
            // in the activator
            Object proxy = m_activator.proxyProvidedServiceObject(m_implementorObject, this.getInstanceMetadata());
            if (proxy != null)
            {
                m_implementorObject = proxy;
            }
            else
            {
                GenericActivator.error("InstanceManager : Proxy method returned a null value");
            }
        }
        catch (Throwable t)
        {
            // failure at creation
            GenericActivator.error("InstanceManager : Error during instantiation : "+t);
            t.printStackTrace();
            _invalidate();
            return;
        }
        
        // initial bindings

        it = m_dependencyManagers.iterator();

        while (it.hasNext())
        {
            DependencyManager dm = (DependencyManager)it.next();
            if (dm.initialize() == false)
            {
                _invalidate();
                return;
            }
        }
        
        // We need to check if we are still validating because it is possible that when we
        // registered the service above our thread causes an instance to become valid which
        // then registered a service that then generated an event that we needed that
        // caused validate() to be called again, thus if we are not still VALIDATING, it
        // means we are already VALID.
        if (m_state == INSTANCE_VALIDATING)
        {
            // activate

            if (m_implementorObject instanceof Lifecycle)
            {
                try
                {
                    ((Lifecycle)m_implementorObject).activate();
                    this.m_activateCalled=true;
                }
                catch(Exception e)
                {
                    GenericActivator.error("InstanceManager : exception during activate:"+e);
                    e.printStackTrace();
                    _invalidate();
                    return;
                }
            }

            // validated!

            fireInstanceReferenceValidated();
            setState(INSTANCE_VALID);
        }
        

        // register services

        boolean reg = requestRegistration();

        if (!reg)
        {
            GenericActivator.error("InstanceManager : registration of the services failed...");
            _invalidate();
            return;
        }
        
        // Configuration ended successfuly.

    }

    /**
     * This method invalidates the InstanceManager
     *
     * CONCURRENCY NOTE: This method may be called either from application code or event thread.
    **/
    synchronized void invalidate()
    {
        if (m_state == INSTANCE_INVALID)
        {
            return;
        }
        else if (m_state != INSTANCE_VALID && m_state != INSTANCE_DESTROYING)
        {
            GenericActivator.error("InstanceManager : invalidate() called for a non VALID InstanceManager ("+m_states[m_state]+")");
            return;
        }

        if (m_state != INSTANCE_DESTROYING)
        {
            setState(INSTANCE_INVALIDATING);
        }

        // Fire invalidating events
        fireInstanceReferenceInvalidating();

        _invalidate();


    }

    /**
     * this method invalidates the InstanceManager without performing any of the callbacks
     * associated with the Lifecycle interface or the InstanceReference event listeners.
    **/
    private void _invalidate()
    {
        // Unregister services

        requestUnregistration();
        
        if(m_activateCalled==true)
        {        
            // Call deactivate on the Lifecycle

            if (m_implementorObject instanceof Lifecycle)
            {
                try
                {
                    ((Lifecycle)m_implementorObject).deactivate();
                }
                catch(Exception e)
                {
                    GenericActivator.error("InstanceManager : exception during call to deactivate():"+e);
                }
            }

        }

        // Unbind all services

        Iterator it = m_dependencyManagers.iterator();

        while (it.hasNext())
        {
            DependencyManager dm = (DependencyManager)it.next();
            dm.unbindAll();
        }

        //m_activator.removeInstanceManager(this);

        // remove all instances from a factory

        /*
        if (m_implementorObject instanceof GenericFactory)
        {
            ((GenericFactory)m_implementorObject).invalidateInstances();
        }
        */

        // Release the object reference

        m_implementorObject = null;

        GenericActivator.trace("InstanceManager from bundle ["
           + m_activator.getBundleContext().getBundle().getBundleId() + "] was invalidated.");

        if (m_state != INSTANCE_DESTROYING)
        {
            setState(INSTANCE_INVALID);
        }
    }

    /**
     * This method should be called to completely remove the InstanceManager from the system.
     * This means that the dependency managers will stop listening to events.
     *
     * CONCURRENCY NOTE: This method is only called from the GenericActivator, which is
     *                   essentially application code and not via events.
    **/
    synchronized void destroy()
    {
        if (m_state == INSTANCE_DESTROYED)
        {
            return;
        }

        // Theoretically this should never be in any state other than VALID or INVALID,
        // because validate is called right after creation.
        boolean invalidatefirst = (m_state == INSTANCE_VALID);

        setState(INSTANCE_DESTROYING);

        // Stop the dependency managers to listen to events...
        Iterator it = m_dependencyManagers.iterator();

        while (it.hasNext())
        {
            DependencyManager dm = (DependencyManager)it.next();
            m_activator.getBundleContext().removeServiceListener(dm);
        }

        if (invalidatefirst)
        {
            invalidate();
        }

        m_dependencyManagers.clear();

        m_instanceListener = null;

        GenericActivator.trace("InstanceManager from bundle ["
           + m_activator.getBundleContext().getBundle().getBundleId() + "] was destroyed.");

        m_activator.removeInstanceManager(this);
        setState(INSTANCE_DESTROYED);

        m_activator = null;
    }

    /**
    * Returns the InstanceMetadata
    */
    public InstanceMetadata getInstanceMetadata()
    {
        return m_instanceMetadata;
    }

    /**
    * Get the object that is implementing this descriptor
    *
    * @return the object that implements
    */
    public Object getObject()
    {
        return m_implementorObject;
    }

    /**
    * Request the registration of the service provided by this binder instance
    *
    * @return returns false if registration was not successful,
    *                returns true if registration was successful
    **/
    boolean requestRegistration()
    {
        if (!m_instanceMetadata.instanceRegistersServices())
        {
            return true;
        }
        else if (m_implementorObject == null)
        {
            GenericActivator.error("GenericActivator : Cannot register, implementor object not created!");
            return false;
        }
        else if (m_serviceRegistration != null)
        {
            GenericActivator.error("GenericActivator : Cannot register, binder instance already registered :"
                + m_instanceMetadata.getImplementorName());
            return true;
        }

        // Check validity of dependencies before registering !
        Iterator it = m_dependencyManagers.iterator();
        while (it.hasNext())
        {
            DependencyManager dm = (DependencyManager)it.next();
            if (dm.isValid() == false)
                return false;
        }

        // When registering a factory, add an instanceClass property which is an array
        // of service interfaces implemented by the objects created by the factory.

        if (m_instanceMetadata.isFactory())
        {
            if(m_instanceMetadata.getProperties().get("instanceClass") == null)
            {
                m_instanceMetadata.getProperties().put("instanceClass",m_instanceMetadata.getInstantiates().getInterfaces());
            }
        }

        m_serviceRegistration = m_activator.getBundleContext().registerService(
            m_instanceMetadata.getInterfaces(), m_implementorObject, m_instanceMetadata.getProperties());

        GenericActivator.trace("Generic Activator : InstanceManager inside bundle ["
            + m_activator.getBundleContext().getBundle().getBundleId()
            + "] successfully registered its services !");

        return true;
    }


    /**
    *
    * Request the unfegistration of the service provided by this binder instance
    *
    **/
    void requestUnregistration()
    {
        if (m_serviceRegistration != null)
        {
            m_serviceRegistration.unregister();
            m_serviceRegistration = null;

            GenericActivator.trace("Generic Activator : InstanceManager inside bundle ["
                + m_activator.getBundleContext().getBundle().getBundleId()
                + "] unregistered its services !");

         }
    }

    /**
    * Get the state
    */
    public int getState()
    {
        return m_state;
    }

    /**
    * Get the state
    */
    public long getBundleId()
    {
        return m_activator.getBundleContext().getBundle().getBundleId();
    }

    /**
     * Get a property associated with this instance. For classes
     * implementing this method, special care must be taken for
     * values implementing <tt>InstanceReference.ValueHolder</tt>.
     * In such cases, the value itself should not be returned, but
     * the value of <tt>InstanceReference.ValueHolder.get()</tt>
     * should be returned instead. This may be used to defer
     * creating value objects in cases where creating the value
     * object is expensive.
     * @param name the name of the property to retrieve.
     * @return the value of the associated property or <tt>null</tt>.
    **/
    public Object get(String name)
    {
        GenericActivator.trace("InstanceManager.get("+name+")");

        if(name.equals(InstanceReference.INSTANCE_STATE))
        {
            return new Integer(m_state);
        }
        else if(name.equals(InstanceReference.INSTANCE_METADATA))
        {
            return getInstanceMetadata();
        }
        else if(name.equals(InstanceReference.INSTANCE_BUNDLE))
        {
            return new Integer((int) getBundleId());
        }
        else if(name.equals(InstanceReference.INSTANCE_DEPENDENCIES))
        {
            return getDependencies();
        }
        else
        {
            Object ret = m_localProperties.get(name);

            if (ret != null)
            {
                if (ret instanceof ValueHolder)
                {
                    return ((ValueHolder)ret).get(this);
                }
                return ret;
            }

            return m_instanceMetadata.getProperties().get(name);
        }

    }

    /**
     * Associate a property with this instance. For classes
     * implementing this method, special care must be taken for
     * values implementing <tt>InstanceReference.ValueHolder</tt>.
     * In such cases, the value itself should not be returned, but
     * the value of <tt>InstanceReference.ValueHolder.get()</tt>
     * should be returned instead. This may be used to defer
     * creating value objects in cases where creating the value
     * object is expensive.
     * @param name the name of the property to add.
     * @param obj the value of the property.
    **/
    public void put(String name, Object obj)
    {
        m_localProperties.put(name,obj);
    }

    /**
     * Adds an instance reference listener to listen for changes to
     * the availability of the underlying object associated with this
     * instance reference.
     * @param l the listener to add.
    **/
    public void addInstanceReferenceListener(InstanceReferenceListener l)
    {
        m_instanceListener = StateChangeMulticaster.add(m_instanceListener, l);
    }

    /**
     * Removes an instance reference listener.
     * @param l the listener to remove.
    **/
    public void removeInstanceReferenceListener(InstanceReferenceListener l)
    {
        m_instanceListener = StateChangeMulticaster.remove(m_instanceListener, l);
    }

    /**
     * Fires an event when the instance reference has been validated
    **/
    protected void fireInstanceReferenceValidated()
    {
        try
        {
            if (m_instanceListener != null)
            {
                m_instanceListener.validated(new InstanceReferenceEvent(this));
            }
        }
        catch(Exception ex)
        {
            // Ignore any exception
        }
    }

    /**
     * Fires an event when the instance reference is invalidating
    **/
    protected void fireInstanceReferenceInvalidating()
    {
        try
        {
            if (m_instanceListener != null)
            {
                m_instanceListener.invalidating(new InstanceReferenceEvent(this));
            }
        }
        catch(Exception ex)
        {
            // Ignore any exception
        }
    }

    /**
     * sets the state of the instanceManager
    **/
    synchronized void setState(int newState)
    {
        m_state = newState;

        if(m_state == INSTANCE_CREATED || m_state == INSTANCE_VALID || m_state == INSTANCE_INVALID || m_state == INSTANCE_DESTROYED)
        {
            m_activator.fireInstanceChangeEvent(new InstanceChangeEvent(this,m_instanceMetadata,m_state));
        }
    }

    /**
     * Get an array of dependencies for this instance. This method is declared
     * in the Instance interface
     *
     * @return an array of Dependencies
    **/
    public Dependency [] getDependencies()
    {
        Dependency deps [] = new Dependency[m_dependencyManagers.size()];
        return (Dependency[]) m_dependencyManagers.toArray(deps);
    }

    /**
     * Get a list of child instances in case this is a factory
     *
     * @return an array of Instances
    **/
     public Instance[] getChildInstances()
    {
        /*
        if(m_implementorObject != null && m_implementorObject instanceof GenericFactory)
        {
            List instanceRefs = ((GenericFactory)m_implementorObject).getInstanceReferences();
            Instance [] instances = new Instance[instanceRefs.size()];
            instances = (Instance [])instanceRefs.toArray(instances);
            return instances;
        }
        */
        return null;
    }

/**
 * This class implements the ServiceBinderContext, which cannot be directly * implemented by the activator because of the getInstanceReference() method
 */

    class ServiceBinderContextImpl implements ServiceBinderContext
    {

        /**
         * 
         * @uml.property name="m_parent"
         * @uml.associationEnd multiplicity="(0 1)"
         */
        private InstanceReference m_parent;

        ServiceBinderContextImpl(InstanceReference parent)
        {
            m_parent = parent;
        }

        /**
         * Get the bundle context
        **/
        public BundleContext getBundleContext()
        {
            return m_activator.getBundleContext();
        }

        /**
         * Return all of the InstanceReferences created in the same bundle
        **/
        public List getInstanceReferences()
        {
            return m_activator.getInstanceReferences();
        }

        /**
         * Get the parent InstanceReference
        **/
        public InstanceReference getInstanceReference()
        {
            return m_parent;
        }
    }

/**
 * The DependencyManager task is to listen to service events and to call the * bind/unbind methods on a given object. It is also responsible for requesting * the unregistration of a service in case a dependency is broken.
 */

    class DependencyManager implements ServiceListener, Dependency
    {

        /**
         * 
         * @uml.property name="m_dependencyMetadata"
         * @uml.associationEnd multiplicity="(0 1)"
         */
        private DependencyMetadata m_dependencyMetadata;

        private Map m_boundServices = new HashMap();
        private Method m_bindMethod;
        private Method m_unbindMethod;
        private boolean m_isValid;
        private int m_depState;
        private boolean m_receivesRef = false;

        /**
         * Constructor that receives several parameters.
         *
         * @param   dependency  An object that contains data about the dependency
        **/
        DependencyManager(DependencyMetadata dependency) throws ClassNotFoundException, NoSuchMethodException
        {
            m_dependencyMetadata = dependency;
            m_isValid = false;

            m_bindMethod = getTargetMethod(m_dependencyMetadata.getBindMethodName(),InstanceManager.this.getInstanceMetadata().getImplementorName(),m_dependencyMetadata.getServiceName());
            m_unbindMethod = getTargetMethod(m_dependencyMetadata.getUnbindMethodName(),InstanceManager.this.getInstanceMetadata().getImplementorName(),m_dependencyMetadata.getServiceName());

            setStateDependency(DependencyChangeEvent.DEPENDENCY_CREATED);
        }

        /**
         * initializes a dependency. This method binds all of the service occurrences to the instance object
         *
         * @return true if the operation was successful, false otherwise
        **/
        boolean initialize()
        {
            if(getObject() == null)
            {
                return false;
            }

            ServiceReference refs[] = getRequiredServiceRefs();

            if (refs == null && m_dependencyMetadata.isOptional() == false)
            {
                m_isValid = false;
                setStateDependency(DependencyChangeEvent.DEPENDENCY_INVALID);
                return m_isValid;
            }

            m_isValid = true;
            setStateDependency(DependencyChangeEvent.DEPENDENCY_VALID);

            if (refs != null)
            {
                int max = 1;
                boolean retval = true;

                if (m_dependencyMetadata.isMultiple() == true)
                {
                    max = refs.length;
                }

                for (int index = 0; index < max; index++)
                {
                    retval = callBindMethod(refs[index]);
                    if(retval == false && (max == 1))
                    {
                        // There was an exception when calling the bind method
                        GenericActivator.error("Dependency Manager: Possible exception in the bind method during initialize()");
                        m_isValid = false;
                        setStateDependency(DependencyChangeEvent.DEPENDENCY_INVALID);
                        return m_isValid;
                    }
                }
            }

            return m_isValid;
        }

        /**
         * Called upon a service event. This method is responsible for calling the
         * binding and unbinding methods and also to request the eventual unregistering
         * of a service when a dependency breaks
         *
         * @param evt  The ServiceEvent
        **/
        public void serviceChanged(ServiceEvent evt)
        {
            synchronized (InstanceManager.this)
            {
                // If the object is being created or destroyed, we can safely ignore events.
                if (m_state == INSTANCE_DESTROYING || m_state == INSTANCE_DESTROYED || m_state == INSTANCE_CREATING || m_state == INSTANCE_CREATED)
                {
                    return;
                }

                // If we are in the process of invalidating, it is not necessary to pass
                // unregistration events, since we are unbinding everything anyway.
                else if (m_state == INSTANCE_INVALIDATING && evt.getType() == ServiceEvent.UNREGISTERING)
                {
                    return;
                }

                // We do not have an entry for VALIDATING because it is reentrant.

                // A service is unregistering
                if (evt.getType() == ServiceEvent.UNREGISTERING)
                {
                    if (m_boundServices.keySet().contains(evt.getServiceReference()) == true)
                    {
                        // A static dependency is broken the instance manager will be invalidated
                        if (m_dependencyMetadata.isStatic())
                        {
                            m_isValid = false;
                            setStateDependency(DependencyChangeEvent.DEPENDENCY_INVALID);
                            try
                            {
                                GenericActivator.trace("Dependency Manager: Static dependency is broken");
                                invalidate();
                                GenericActivator.trace("Dependency Manager: RECREATING");
                                validate();
                            }
                            catch(Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                        // dynamic dependency
                        else
                        {
                            // Release references to the service, call unbinder method
                            // and eventually request service unregistration

                            callUnbindMethod(evt.getServiceReference());

                            // The only thing we need to do here is check if we can reinitialize
                            // once the bound services becomes zero. This tries to repair dynamic
                            // 1..1 or rebind 0..1, since replacement services may be available.
                            // In the case of aggregates, this will only invalidate them since they
                            // can't be repaired.
                            if (m_boundServices.size() == 0)
                            {
                                // try to reinitialize
                                if (!initialize())
                                {
                                    if (!m_dependencyMetadata.isOptional())
                                    {
                                        GenericActivator.trace("Dependency Manager: Mandatory dependency not fullfilled and no replacements available... unregistering service...");
                                        invalidate();
                                        GenericActivator.trace("Dependency Manager: RECREATING");
                                        validate();
                                    }
                                }
                            }
                        }
                    }
                }
                // A service is registering.
                else if (evt.getType() == ServiceEvent.REGISTERED)
                {
                    if (m_boundServices.keySet().contains(evt.getServiceReference()) == true)
                    {
                        // This is a duplicate
                        GenericActivator.trace("DependencyManager : ignoring REGISTERED ServiceEvent (already bound)");
                    }
                    else
                    {
                        m_isValid = true;
                        setStateDependency(DependencyChangeEvent.DEPENDENCY_VALID);

                        // If the InstanceManager is invalid, a call to validate is made
                        // which will fix everything.
                        if (InstanceManager.this.getState() != INSTANCE_VALID)
                        {
                            validate();
                        }
                        // Otherwise, this checks for dynamic 0..1, 0..N, and 1..N it never
                        // checks for 1..1 dynamic which is done above by the validate()
                        else if (!m_dependencyMetadata.isStatic())
                        {
                            // For dependency that are aggregates, always bind the service
                            // Otherwise only bind if bind services is zero, which captures the 0..1 case
                            if (m_dependencyMetadata.isMultiple() || m_boundServices.size() == 0)
                            {
                                callBindMethod(evt.getServiceReference());
                            }
                        }
                    }
                }
            }
        }

        /**
         * Revoke all bindings. This method cannot throw an exception since it must try
         * to complete all that it can
         *
        **/
        void unbindAll()
        {
            Object []allrefs = m_boundServices.keySet().toArray();

            if (allrefs == null)
                return;

            for (int i = 0; i < allrefs.length; i++)
            {
                callUnbindMethod((ServiceReference)allrefs[i]);
            }
        }

        /**
         * Test if this dependency managed by this object is valid
        **/
        boolean isValid()
        {
            return m_isValid;
        }

        /**
         *
         * Returns an array containing the service references that are pertinent to the
         * dependency managed by this object. This method filters out services that
         * belong to bundles that are being (or are actually) shutdown. This is an issue
         * since is not clearly specified in the OSGi specification if a getServiceReference
         * call should return the services that belong to bundles that are stopping.
         *
         * @return an array of ServiceReferences valid in the context of this dependency
        **/
        ServiceReference [] getRequiredServiceRefs()
        {
            try
            {
                ArrayList list=new ArrayList();
                ServiceReference temprefs[] =
                    m_activator.getBundleContext().getServiceReferences(m_dependencyMetadata.getServiceName(), m_dependencyMetadata.getFilter());

                if (temprefs == null)
                {
                    return null;
                }

                for (int i = 0; i < temprefs.length; i++)
                {
                     if (temprefs[i].getBundle().getState() == Bundle.ACTIVE
                            || temprefs[i].getBundle().getState() == Bundle.STARTING)
                     {
                         list.add(temprefs[i]);
                     }
                }

                return (ServiceReference []) list.toArray(new ServiceReference [temprefs.length]);

            }
            catch (Exception e)
            {
                GenericActivator.error("DependencyManager: exception while getting references :"+e);
                return null;
            }
        }

        /**
         * Gets a target method based on a set of parameters
         *
         * @param methodname The name of the method
         * @param targetClass the class to which the method belongs to
         * @param paramClass the class of the parameter that is passed to the method
         * @throws java.lang.ClassNotFoundException if the class was not found
         * @throws java.lang.NoSuchMethodException if the method was not found
        **/
        private Method getTargetMethod(String methodname, String targetClass, String paramClass)
            throws ClassNotFoundException, NoSuchMethodException
        {
            Class targetclass = m_activator.getClass().getClassLoader().loadClass(targetClass);

            Method method = null;
            
            try
            {
                method = targetclass.getMethod(methodname, 
                    new Class[]{m_activator.getClass().getClassLoader().loadClass(paramClass)});
               
            }
            catch(NoSuchMethodException ex)
            {
                // Test if the bind method receives a ServiceReference as the first parameter
                
                method = targetclass.getMethod(methodname, 
                    new Class[]{ServiceReference.class, m_activator.getClass().getClassLoader().loadClass(paramClass)});
               
                m_receivesRef = true;
            }
            
            return method;
        }

        /**
         * Call the bind method. In case there is an exception while calling the bind method, the service
         * is not considered to be bound to the instance object
         *
         * @param ref A ServiceReference with the service that will be bound to the instance object
         * @return true if the call was successful, false otherwise
        **/
        boolean callBindMethod(ServiceReference ref)
        {
            try
            {
                Object requiredService = m_activator.getBundleContext().getService(ref);
                Object proxy = m_activator.proxyRequiredServiceObject(requiredService,m_dependencyMetadata);
                
                if(proxy == null)
                {
                    // ignore a null return value from the proxy method
                    proxy = requiredService;
                }
                if(m_receivesRef == false)
                {
                    m_bindMethod.invoke(getObject(),new Object[] {proxy});
                }
                else
                {
                    m_bindMethod.invoke(getObject(),new Object[] {ref,proxy});
                }
                m_boundServices.put(ref,proxy);
                
                return true;
            }
            catch(Exception e)
            {
                if(e instanceof InvocationTargetException)
                {
                    InvocationTargetException ite = (InvocationTargetException) e;
                    GenericActivator.error("DependencyManager : exception while invoking "+m_dependencyMetadata.getBindMethodName()+" :"+ite.getTargetException());
                }
                else
                {
                    GenericActivator.error("DependencyManager : exception while invoking "+m_dependencyMetadata.getBindMethodName()+" :"+e);
                }
                e.printStackTrace();
                return false;
            }
        }

        /**
         * Call the unbind method
         *
         * @param ref A service reference corresponding to the service that will be unbound
        **/
        void callUnbindMethod(ServiceReference ref)
        {
            Object requiredService = m_boundServices.get(ref);
            if (requiredService == null)
            {
                GenericActivator.error("DependencyManager : callUnbindMethod UNBINDING UNKNOWN SERVICE !!!!");
                return;
            }

            try
            {
                if(m_receivesRef == false)
                {
                    m_unbindMethod.invoke(getObject(),new Object [] {requiredService});
                }
                else
                {
                    m_unbindMethod.invoke(getObject(),new Object [] {ref, requiredService});
                }
                
                m_boundServices.remove(ref);
                m_activator.getBundleContext().ungetService(ref);
            }
            catch(Exception e)
            {
                if(e instanceof InvocationTargetException)
                {
                    InvocationTargetException ite = (InvocationTargetException) e;
                    GenericActivator.error("DependencyManager : exception while invoking "+m_dependencyMetadata.getUnbindMethodName()+" :"+ite.getTargetException());
                }
                else
                {
                    GenericActivator.error("DependencyManager : exception while invoking "+m_dependencyMetadata.getUnbindMethodName()+" :"+e);
                }
                e.printStackTrace();
            }
        }

        /**
         * Return the dependency descriptor
         *
         * @return the corresponding dependency descriptor
        **/
        public DependencyMetadata getDependencyMetadata()
        {
            return m_dependencyMetadata;
        }

        /**
         * Fire a state change event.
         *
         * @param state the state of the dependency manager
        **/
        void setStateDependency(int state)
        {
            m_depState = state;
            m_activator.fireDependencyChangeEvent(new DependencyChangeEvent(this,m_dependencyMetadata,state));
        }

        /**
         * Get the state of the dependency.
         *
         * @return the state of the dependency manager
        **/
        public int getDependencyState()
        {
            return m_depState;
        }

        /**
         * Get the bound service objects. This method is declared in the Dependency interface
         * and is used for to get a model.
         *
         * @return an array containing the bound service objects
        **/
        public Instance[] getBoundInstances()
        {
            Object bound[] = m_boundServices.values().toArray();

            ArrayList tempArray = new ArrayList();

            for(int i=0; i<bound.length; i++)
            {
                InstanceReference ref = ArchitectureServiceImpl.findInstanceReference(bound[i]);
                if(ref != null)
                {
                    tempArray.add(ref);
                }
            }

            Instance instances[]= new Instance[tempArray.size()];
            instances = (Instance [])tempArray.toArray(instances);

            return instances;


        }
    }

/**
 * @version X.XX Feb 3, 2004  * @author Humberto Cervantes
 */

    static public class StateChangeMulticaster implements InstanceReferenceListener
    {

        /**
         * 
         * @uml.property name="a"
         * @uml.associationEnd multiplicity="(0 1)"
         */
        protected InstanceReferenceListener a;

        /**
         * 
         * @uml.property name="b"
         * @uml.associationEnd multiplicity="(0 1)"
         */
        protected InstanceReferenceListener b;

        protected StateChangeMulticaster(InstanceReferenceListener a, InstanceReferenceListener b)    
        {        
            this.a = a;        
            this.b = b;    
        }    
    
    
        public void validated(InstanceReferenceEvent e)    
        {
            a.validated(e);        
            b.validated(e);
        }
    
        public void invalidating(InstanceReferenceEvent e)    
        {
            a.invalidating(e);        
            b.invalidating(e);
        }
    
        public static InstanceReferenceListener add(InstanceReferenceListener a, InstanceReferenceListener b)
        {
            if (a == null)
                return b;
            else if (b == null)
                return a;
            else
                return new StateChangeMulticaster(a, b);
        }
    
        public static InstanceReferenceListener remove(InstanceReferenceListener a, InstanceReferenceListener b)
        {
            if ((a == null) || (a == b))            
                return null;        
            else if (a instanceof StateChangeMulticaster)            
                return add (remove (((StateChangeMulticaster) a).a, b),remove (((StateChangeMulticaster) a).b, b));        
            else            
                return a;    
        }
    }
}
