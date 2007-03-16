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
package org.apache.felix.scr;

import java.lang.reflect.*;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;


/**
 * The default ComponentManager. Objects of this class are responsible for managing
 * implementation object's lifecycle.  
 *
 */
class ComponentManagerImpl implements ComponentManager, ComponentInstance
{
	// States of the instance manager
	static final int INSTANCE_CREATING = 0;
	static final int INSTANCE_CREATED = 1;
	static final int INSTANCE_VALIDATING = 2;
	static final int INSTANCE_VALID = 3;
	static final int INSTANCE_INVALIDATING = 4;
	static final int INSTANCE_INVALID = 5;
	static final int INSTANCE_DESTROYING = 6;
	static final int INSTANCE_DESTROYED = 7;
		

    static final String m_states[]={"CREATING","CREATED",
    								"VALIDATING","VALID",
    								"INVALIDATING","INVALID",
    								"DESTROYING","DESTROYED"
                                    };

    // The state of this instance manager
    private int m_state = INSTANCE_CREATING;

    // The metadata
    private ComponentMetadata m_componentMetadata;

    // The object that implements the service and that is bound to other services
    private Object m_implementationObject = null;

    // The dependency managers that manage every dependency
    private List m_dependencyManagers = new ArrayList();

    // The ServiceRegistration
    private ServiceRegistration m_serviceRegistration = null;

    // A reference to the BundleComponentActivator
    private BundleComponentActivator m_activator = null;

    // The context that will be passed to the implementationObject
    private ComponentContext m_componentContext = null;
    
    // In case of a delayed component, this holds a reference to the factory
    private ServiceFactory m_delayedComponentServiceFactory;
    
    /**
     * The constructor receives both the activator and the metadata
     * 
     * @param activator
     * @param metadata
     */
    ComponentManagerImpl(BundleComponentActivator activator, ComponentMetadata metadata)
    {
    	// Store the activator reference
        m_activator = activator;

        // Store the metadata reference
        m_componentMetadata = metadata;
    }
    
    /**
     * Enable this component
     * 
     * @return true if enabling was successful
     */
    public boolean enable() {
    	
        Activator.trace("Enabling component", m_componentMetadata);
    	
    	try
    	{
	        // If this component has got dependencies, create dependency managers for each one of them.
	        if (m_componentMetadata.getDependencies().size() != 0)
	        {
	            Iterator dependencyit = m_componentMetadata.getDependencies().iterator();
	
	            while(dependencyit.hasNext())
	            {
	                ReferenceMetadata currentdependency = (ReferenceMetadata)dependencyit.next();
	
	                DependencyManager depmanager = new DependencyManager(currentdependency);
	
	                m_dependencyManagers.add(depmanager);
	
	                // Register the dependency managers as listeners to service events so that they begin
	                // to manage the dependency autonomously
	                m_activator.getBundleContext().addServiceListener(depmanager,depmanager.m_dependencyMetadata.getTarget());
	            }
	        }
	
	        // TODO: create the context
	        //m_sbcontext = new ServiceBinderContextImpl(this);
	
	        // Add this instance manager to the Generic activator list
	        //m_activator.addInstanceManager(this);
	        
	
	        setState(INSTANCE_CREATED);
	        
	        activate();
	        
	        return true;
    	}
    	catch(Exception ex)
    	{
    		// TODO: Log this error
    		return false;
    	}
    }

    /**
    * Activate this Instance manager.
    * 
    * 112.5.6 Activating a component configuration consists of the following steps
    *   1. Load the component implementation class
    *   2. Create the component instance and component context
    *   3. Bind the target services
    *   4. Call the activate method, if present
    *   [5. Register provided services]
    */
    synchronized private void activate()
    {
        // CONCURRENCY NOTE: This method is called either by the enable() method or by the dependency
    	// managers.    	
    	if (m_state != INSTANCE_INVALID && m_state !=INSTANCE_CREATED) {
    		// This state can only be entered from the CREATED or INVALID states
    		return;
    	}

        setState(INSTANCE_VALIDATING);

        // Before creating the implementation object, we are going to
        // test if all the mandatory dependencies are satisfied
        Iterator it = m_dependencyManagers.iterator();

        while (it.hasNext())
        {
            // It is not possible to call the isValid method yet in the DependencyManagers
            // since they have not been initialized yet, but they can't be initialized yet
            // because the implementationObject has not been created yet.
            // This test is necessary, because we don't want to instantiate
            // the implementationObject if the dependency managers aren't valid.
            DependencyManager dm = (DependencyManager)it.next();
            if (dm.getRequiredServiceRefs() == null && dm.m_dependencyMetadata.isOptional() == false)
            {
                setState(INSTANCE_INVALID);
                return;
            }
        }
        
        // 1. Load the component implementation class
        // 2. Create the component instance and component context
        // If the component is not immediate, this is not done at this moment
        if( m_componentMetadata.isImmediate() == true )
        {
            //Activator.trace("Loading implementation class and creating instance for component '"+m_componentMetadata.getName()+"'");
        	try
	        {
	        	// 112.4.4 The class is retrieved with the loadClass method of the component's bundle
	            Class c = m_activator.getBundleContext().getBundle().loadClass(m_componentMetadata.getImplementationClassName());
	            
	            // 112.4.4 The class must be public and have a public constructor without arguments so component instances
	            // may be created by the SCR with the newInstance method on Class
	            m_componentContext = new ComponentContextImpl(null);
	            m_implementationObject = c.newInstance();
	        }
	        catch (Exception ex)
	        {
	            // TODO: manage this exception when implementation object cannot be created
	            Activator.exception("Error during instantiation", m_componentMetadata, ex);
	            deactivate();
	            //invalidate();
	            return;
	        }
        }
        else if ( m_componentMetadata.getServiceMetadata() != null
            && m_componentMetadata.getServiceMetadata().isServiceFactory() )
        {
            // delayed component is a ServiceFactory service
            m_delayedComponentServiceFactory = new DelayedServiceFactoryServiceFactory();
        }
        else
        {
            // delayed component is a standard service
            m_delayedComponentServiceFactory = new DelayedComponentServiceFactory();
        }
        
        // 3. Bind the target services
        it = m_dependencyManagers.iterator();

        //Activator.trace("Binding target services for component '"+m_componentMetadata.getName()+"'");
        
        while (it.hasNext())
        {
            DependencyManager dm = (DependencyManager)it.next();
            
            // if any of the dependency managers is unable to bind (it is invalid), the component is deactivated
            if (dm.bind() == false)
            {
            	deactivate();
                return;
            }
        }
        
        //Activator.trace("Calling activate for component '"+m_componentMetadata.getName()+"'");
        
        // 4. Call the activate method, if present
	        // We need to check if we are still validating because it is possible that when we
	        // registered the service above our thread causes an instance to become valid which
	        // then registered a service that then generated an event that we needed that
	        // caused validate() to be called again, thus if we are not still VALIDATING, it
	        // means we are already VALID.
        if ( m_componentMetadata.isImmediate() == true && m_state == INSTANCE_VALIDATING)
        {
            // Search for the activate method
        	try {
                Method activateMethod = getMethod(m_implementationObject.getClass(), "activate", new Class[]{ComponentContext.class});
        		activateMethod.invoke(m_implementationObject, new Object[]{m_componentContext});
        	}
        	catch(NoSuchMethodException ex) {        		
        	    // We can safely ignore this one
        	    Activator.trace("activate() method not implemented", m_componentMetadata);
        	}
        	catch(IllegalAccessException ex) {
        	    // TODO: Log this exception?
        	    Activator.trace("activate() method cannot be called", m_componentMetadata);
        	}
        	catch(InvocationTargetException ex) {
        	    // TODO: 112.5.8 If the activate method throws an exception, SCR must log an error message
        	    // containing the exception with the Log Service
        	    Activator.exception("The activate method has thrown an exception", m_componentMetadata, ex.getTargetException());
        	}
        }
        
        // Validation occurs before the services are provided, otherwhise the service provider's service may be called
        // by a service requester while it is still VALIDATING
        setState(INSTANCE_VALID);
        
        // 5. Register provided services
        if(m_componentMetadata.getServiceMetadata() != null)
        {
            Activator.trace("registering services", m_componentMetadata);

        	if( m_componentMetadata.isImmediate() == true ) {
	        	// In the case the component is immediate, the implementation object is registered
	        	m_serviceRegistration = m_activator.getBundleContext().registerService(m_componentMetadata.getServiceMetadata().getProvides(), m_implementationObject, m_componentMetadata.getProperties());
	        }else {
	        	// In the case the component is delayed, a factory is registered
	        	m_serviceRegistration = m_activator.getBundleContext().registerService(m_componentMetadata.getServiceMetadata().getProvides(), m_delayedComponentServiceFactory, m_componentMetadata.getProperties());
	        }
        }
    }
    
    /**
     * This method deactivates the manager, performing the following steps
     * 
     * [0. Remove published services from the registry]
     * 1. Call the deactivate() method, if present
     * 2. Unbind any bound services
     * 3. Release references to the component instance and component context 
    **/
    synchronized private void deactivate()
    {
    	// CONCURRENCY NOTE: This method may be called either from application code or by the dependency managers
    	if (m_state != INSTANCE_VALID && m_state != INSTANCE_VALIDATING && m_state != INSTANCE_DESTROYING) {
    		return;
    	}

    	// In case the instance is valid when this is called, the manager is set to an invalidating state
        if (m_state != INSTANCE_DESTROYING)
        {
            setState(INSTANCE_INVALIDATING);
        }
    	
        // 0.- Remove published services from the registry
        if(m_serviceRegistration != null)
        {
	        m_serviceRegistration.unregister();
	        m_serviceRegistration = null;

	        Activator.trace("unregistering the services", m_componentMetadata);
	    }

        // 1.- Call the deactivate method, if present	    
        // Search the deactivate method
		try {
			// It is necessary to check that the implementation Object is not null. This may happen if the component
			// is delayed and its service was never requested.
			if(m_implementationObject != null)
			{
				Method activateMethod = getMethod(m_implementationObject.getClass(), "deactivate", new Class[]{ComponentContext.class});
				activateMethod.invoke(m_implementationObject, new Object[]{m_componentContext});				
			}
		}
		catch(NoSuchMethodException ex) {
            // We can safely ignore this one
			Activator.trace("deactivate() method is not implemented", m_componentMetadata);
		}
		catch(IllegalAccessException ex) {
			// Ignored, but should it be logged?
			Activator.trace("deactivate() method cannot be called", m_componentMetadata);
		}
		catch(InvocationTargetException ex) {
			// TODO: 112.5.12 If the deactivate method throws an exception, SCR must log an error message
			// containing the exception with the Log Service
			Activator.exception("The deactivate method has thrown and exception", m_componentMetadata, ex);
		}

        // 2. Unbind any bound services
        Iterator it = m_dependencyManagers.iterator();

        while (it.hasNext())
        {
            DependencyManager dm = (DependencyManager)it.next();
            dm.unbind();
        }

        // 3. Release references to the component instance and component context
        m_implementationObject = null;
        m_componentContext = null;
        m_delayedComponentServiceFactory = null;

        //Activator.trace("InstanceManager from bundle ["+ m_activator.getBundleContext().getBundle().getBundleId() + "] was invalidated.");

        if (m_state != INSTANCE_DESTROYING)
        {
            setState(INSTANCE_INVALID);
        }
    }

    /**
     * 
     */
    public synchronized void dispose()
    {
        // CONCURRENCY NOTE: This method is only called from the BundleComponentActivator or by application logic
    	// but not by the dependency managers

        // Theoretically this should never be in any state other than VALID or INVALID,
        // because validate is called right after creation.
        if (m_state != INSTANCE_VALID && m_state != INSTANCE_INVALID)
        {
            return;
        }

        boolean deactivationRequired = (m_state == INSTANCE_VALID);

        setState(INSTANCE_DESTROYING);

        // Stop the dependency managers to listen to events...
        Iterator it = m_dependencyManagers.iterator();

        while (it.hasNext())
        {
            DependencyManager dm = (DependencyManager)it.next();
            m_activator.getBundleContext().removeServiceListener(dm);
        }

        // in case the component is disposed when it was VALID, it is necessary to deactivate it first.
        if (deactivationRequired)
        {
            deactivate();
        }

        m_dependencyManagers.clear();

        setState(INSTANCE_DESTROYED);

        m_activator = null;
    }

    //**********************************************************************************************************
    
    /**
    * Get the object that is implementing this descriptor
    *
    * @return the object that implements the services
    */
    public Object getInstance() {
        return m_implementationObject;
    }

    /**
     * 
     */
    public ComponentMetadata getComponentMetadata() {
    	return m_componentMetadata;
    }
    
    /**
     * sets the state of the manager
    **/
    private synchronized void setState(int newState) {
        Activator.trace("State transition : "+m_states[m_state]+" -> "+m_states[newState], m_componentMetadata);
    	
        m_state = newState;
        
        

        if(m_state == INSTANCE_CREATED || m_state == INSTANCE_VALID || m_state == INSTANCE_INVALID || m_state == INSTANCE_DESTROYED)
        {
            //m_activator.fireInstanceChangeEvent(new InstanceChangeEvent(this,m_instanceMetadata,m_state));
        }
    }

/**
 * The DependencyManager task is to listen to service events and to call the * bind/unbind methods on a given object. It is also responsible for requesting * the unregistration of a service in case a dependency is broken.
 */
    class DependencyManager implements ServiceListener
    {
    	// Reference to the metadata
        private ReferenceMetadata m_dependencyMetadata;

        // The bound services <ServiceReference> 
        private Set m_boundServicesRefs = new HashSet();
        
        // A flag that denotes if the dependency is satisfied at any given moment
        private boolean m_isValid;
        
        // A flag that defines if the bind method receives a ServiceReference
        private boolean m_bindUsesServiceReference = false;

        /**
         * Constructor that receives several parameters.
         *
         * @param   dependency  An object that contains data about the dependency
        **/
        private DependencyManager(ReferenceMetadata dependency) throws ClassNotFoundException, NoSuchMethodException
        {
            m_dependencyMetadata = dependency;
            m_isValid = false;

            //m_bindMethod = getTargetMethod(m_dependencyMetadata.getBind(), m_componentMetadata.getImplementationClassName(),m_dependencyMetadata.getInterface());
            //m_unbindMethod = getTargetMethod(m_dependencyMetadata.getUnbind(), m_componentMetadata.getImplementationClassName(),m_dependencyMetadata.getInterface());
        }

        /**
         * initializes a dependency. This method binds all of the service occurrences to the instance object
         *
         * @return true if the operation was successful, false otherwise
        **/
        private boolean bind()
        {
        	/* 
            if(getInstance() == null)
            {
                return false;
            }*/

            // Get service references
            ServiceReference refs[] = getRequiredServiceRefs();

            // If no references were received, we have to check if the dependency
            // is optional, if it is not then the dependency is invalid
            if (refs == null && m_dependencyMetadata.isOptional() == false)
            {
                m_isValid = false;
                return m_isValid;
            }

            m_isValid = true;

            // refs can be null if the dependency is optional
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
                    retval = invokeBindMethod(m_implementationObject, refs[index]);
                    if(retval == false && (max == 1))
                    {
                        // There was an exception when calling the bind method
                        Activator.error("Dependency Manager: Possible exception in the bind method during initialize()");
                        m_isValid = false;
                        //setStateDependency(DependencyChangeEvent.DEPENDENCY_INVALID);
                        return m_isValid;
                    }
                }
            }

            return m_isValid;
        }

        /**
         * Revoke all bindings. This method cannot throw an exception since it must try
         * to complete all that it can
         *
        **/
        private void unbind()
        {
            Object []allrefs = m_boundServicesRefs.toArray();

            if (allrefs == null)
                return;

            for (int i = 0; i < allrefs.length; i++)
            {
                invokeUnbindMethod(m_implementationObject, (ServiceReference)allrefs[i]);
            }
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
        private ServiceReference [] getRequiredServiceRefs()
        {
            try
            {
                ArrayList list=new ArrayList();
                
                ServiceReference temprefs[] = m_activator.getBundleContext().getServiceReferences(m_dependencyMetadata.getInterface(), m_dependencyMetadata.getTarget());

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
                Activator.error("DependencyManager: exception while getting references :"+e);
                return null;
            }
        }

        /**
         * Gets a bind or unbind method according to the policies described in the specification
         *
         * @param methodname The name of the method
         * @param targetClass the class to which the method belongs to
         * @param parameterClassName the name of the class of the parameter that is passed to the method
         * @return the method or null
         * @throws java.lang.ClassNotFoundException if the class was not found
        **/
        private Method getBindingMethod(String methodname, Class targetClass, String parameterClassName)
        {
            Method method = null;
            
            Class parameterClass = null;
            
            // 112.3.1 The method is searched for using the following priority
            // 1. The method's parameter type is org.osgi.framework.ServiceReference
            // 2. The method's parameter type is the type specified by the reference's interface attribute
            // 3. The method's parameter type is assignable from the type specified by the reference's interface attribute
            try{
            	// Case 1
            	
                method = getMethod(targetClass, methodname, new Class[]{ServiceReference.class});
               
                m_bindUsesServiceReference = true;                
            }
            catch(NoSuchMethodException ex){
            	
            	try {
            		// Case2
            		
            		m_bindUsesServiceReference = false;
            		
            		parameterClass = m_activator.getBundleContext().getBundle().loadClass(parameterClassName);
            		
	                method = getMethod(targetClass, methodname, new Class[]{parameterClass});
            	}
                catch(NoSuchMethodException ex2) {
            		
                    // Case 3
                    method = null;
            		
                    // iterate on class hierarchy
                    for ( ; method == null && targetClass != null; targetClass = targetClass.getSuperclass())
                    {
                        // Get all potential bind methods
                        Method candidateBindMethods[]  = targetClass.getDeclaredMethods();
                       
                        // Iterate over them
                        for(int i = 0; method == null && i < candidateBindMethods.length; i++) {
                            Method currentMethod = candidateBindMethods[i];
                           
                            // Get the parameters for the current method
                            Class[] parameters = currentMethod.getParameterTypes();
                           
                            // Select only the methods that receive a single parameter
                            // and a matching name
                            if(parameters.length == 1 && currentMethod.getName().equals(methodname)) {
                               
                                // Get the parameter type
                                Class theParameter = parameters[0];
                               
                                // Check if the parameter type is assignable from the type specified by the reference's interface attribute
                                if(theParameter.isAssignableFrom(parameterClass)) {
                                    
                                    // Final check: it must be public or protected
                                    if (Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers()))
                                    {
                                        if (!method.isAccessible())
                                        {
                                            method.setAccessible(true);
                                        }
                                        method = currentMethod;
                                        
                                    }
                                }
                            }                           
                        }
                    }
            	} 
            	catch(ClassNotFoundException ex2) {
            		Activator.exception("Cannot load class used as parameter "+parameterClassName,m_componentMetadata,ex2);
            	}
            }
                        
            return method;
        }

        /**
         * Call the bind method. In case there is an exception while calling the bind method, the service
         * is not considered to be bound to the instance object
         *
         * @param implementationObject The object to which the service is bound
         * @param ref A ServiceReference with the service that will be bound to the instance object
         * @param storeRef A boolean that indicates if the reference must be stored (this is used for the delayed components)
         * @return true if the call was successful, false otherwise
        **/
        private boolean invokeBindMethod(Object implementationObject, ServiceReference ref) {
        	// The bind method is only invoked if the implementation object is not null. This is valid
        	// for both immediate and delayed components
        	if(implementationObject != null) {
        		
		        try {
		        	// Get the bind method
		            Method bindMethod = getBindingMethod(m_dependencyMetadata.getBind(), implementationObject.getClass(), m_dependencyMetadata.getInterface());
		            
		            if(bindMethod == null){
		            	// 112.3.1 If the method is not found , SCR must log an error
		            	// message with the log service, if present, and ignore the method
		            	// TODO: log error message
		                Activator.trace("bind() method not found", m_componentMetadata);
		            	return false;
		            }
		            
		            // Get the parameter
		            Object parameter;
		            
		            if(m_bindUsesServiceReference == false) {		            	 
		            	parameter = m_activator.getBundleContext().getService(ref);
		            }
		            else {
		            	parameter = ref;
		            }
		            	
		            // Invoke the method
		            bindMethod.invoke(implementationObject, new Object[] {parameter});
		            
		            // Store the reference
		        	m_boundServicesRefs.add(ref);                
		            
		            return true;
		        }
		        catch(IllegalAccessException ex)
		        {
		        	// 112.3.1 If the method is not is not declared protected or public, SCR must log an error
		        	// message with the log service, if present, and ignore the method
		        	// TODO: log error message
		        	return false;
		        }
		        catch(InvocationTargetException ex)
		        {
		        	Activator.exception("DependencyManager : exception while invoking "+m_dependencyMetadata.getBind()+"()", m_componentMetadata, ex);
		            return false;
		        }
        	} else if( implementationObject == null && m_componentMetadata.isImmediate() == false) {
        		// In the case the implementation object is null and the component is delayed
        		// then we still have to store the object that is passed to the bind methods
        		// so that it can be used once the implementation object is created.
        		m_boundServicesRefs.add(ref);
        		return true;
        	} else {
        		// TODO: assert false : this theoretically never happens...
        		return false;
        	}        	
        }

        /**
         * Call the unbind method
         *
         * @param implementationObject The object from which the service is unbound
         * @param ref A service reference corresponding to the service that will be unbound
         * @return true if the call was successful, false otherwise
        **/
        private boolean invokeUnbindMethod(Object implementationObject, ServiceReference ref) {
        	// TODO: assert m_boundServices.contains(ref) == true : "DependencyManager : callUnbindMethod UNBINDING UNKNOWN SERVICE !!!!";	
        	
        	// The unbind method is only invoked if the implementation object is not null. This is valid
        	// for both immediate and delayed components
        	if ( implementationObject != null ) {
	            try
	            {
	            	// TODO: me quede aqui por que el unbind method no funciona
	                Activator.trace("getting unbind: "+m_dependencyMetadata.getUnbind(), m_componentMetadata);
	            	Method unbindMethod = getBindingMethod(m_dependencyMetadata.getUnbind(), implementationObject.getClass(), m_dependencyMetadata.getInterface());
	            	
		        	// Recover the object that is bound from the map.
		            //Object parameter = m_boundServices.get(ref);
	        		Object parameter = null;
	        		
	        		if(m_bindUsesServiceReference == true) {
	        			parameter = ref;
	        		} else {
	        			parameter = m_activator.getBundleContext().getService(ref);
	        		}
		            
	            	if(unbindMethod == null){
	                	// 112.3.1 If the method is not found , SCR must log an error
	                	// message with the log service, if present, and ignore the method
	                	// TODO: log error message
	            	    Activator.trace("unbind() method not found", m_componentMetadata);
	                	return false;
	                }
	
	            	unbindMethod.invoke(implementationObject, new Object [] {parameter});
	            	                
	                m_boundServicesRefs.remove(ref);
	                
	                m_activator.getBundleContext().ungetService(ref);
	                
	                return true;
	            }
	            catch (IllegalAccessException ex) {
	            	// 112.3.1 If the method is not is not declared protected or public, SCR must log an error
	            	// message with the log service, if present, and ignore the method
	            	// TODO: log error message
	            	return false;
	            }
	            catch (InvocationTargetException ex) {
	                Activator.exception("DependencyManager : exception while invoking "+m_dependencyMetadata.getUnbind()+"()", m_componentMetadata, ex);
	            	return false;
	            }
	            
        	} else if( implementationObject == null && m_componentMetadata.isImmediate() == false) {
        		// In the case the implementation object is null and the component is delayed
        		// then we still have to store the object that is passed to the bind methods
        		// so that it can be used once the implementation object is created.
        		m_boundServicesRefs.remove(ref);
        		return true;
        	} else {
        		// TODO: assert false : this theoretically never happens...
        		return false;
        	}
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
            synchronized (ComponentManagerImpl.this)
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
                    if (m_boundServicesRefs.contains(evt.getServiceReference()) == true)
                    {
                        // A static dependency is broken the instance manager will be invalidated
                        if (m_dependencyMetadata.isStatic())
                        {
                            m_isValid = false;
                            //setStateDependency(DependencyChangeEvent.DEPENDENCY_INVALID);
                            try
                            {
                                Activator.trace("Dependency Manager: Static dependency is broken", m_componentMetadata);
                                deactivate();
                                Activator.trace("Dependency Manager: RECREATING", m_componentMetadata);
                                activate();
                            }
                            catch(Exception ex)
                            {
                                Activator.exception("Exception while recreating dependency ",m_componentMetadata, ex);
                            }
                        }
                        // dynamic dependency
                        else
                        {
                            // Release references to the service, call unbinder method
                            // and eventually request service unregistration

                            invokeUnbindMethod(m_implementationObject, evt.getServiceReference());

                            // The only thing we need to do here is check if we can reinitialize
                            // once the bound services becomes zero. This tries to repair dynamic
                            // 1..1 or rebind 0..1, since replacement services may be available.
                            // In the case of aggregates, this will only invalidate them since they
                            // can't be repaired.
                            if (m_boundServicesRefs.size() == 0)
                            {
                                // try to reinitialize
                                if (!bind())
                                {
                                    if (!m_dependencyMetadata.isOptional())
                                    {
                                        Activator.trace("Dependency Manager: Mandatory dependency not fullfilled and no replacements available... unregistering service...", m_componentMetadata);
                                        deactivate();
                                        Activator.trace("Dependency Manager: Recreating", m_componentMetadata);
                                        activate();
                                    }
                                }
                            }
                        }
                    }
                }
                // A service is registering.
                else if (evt.getType() == ServiceEvent.REGISTERED)
                {
                    if (m_boundServicesRefs.contains(evt.getServiceReference()) == true)
                    {
                        // This is a duplicate
                        Activator.trace("DependencyManager : ignoring REGISTERED ServiceEvent (already bound)", m_componentMetadata);
                    }
                    else
                    {
                        m_isValid = true;
                        //setStateDependency(DependencyChangeEvent.DEPENDENCY_VALID);

                        // If the InstanceManager is invalid, a call to validate is made
                        // which will fix everything.
                        if (ComponentManagerImpl.this.m_state != INSTANCE_VALID)
                        {
                            activate();
                        }
                        // Otherwise, this checks for dynamic 0..1, 0..N, and 1..N it never
                        // checks for 1..1 dynamic which is done above by the validate()
                        else if (!m_dependencyMetadata.isStatic())
                        {
                            // For dependency that are aggregates, always bind the service
                            // Otherwise only bind if bind services is zero, which captures the 0..1 case
                            if (m_dependencyMetadata.isMultiple() || m_boundServicesRefs.size() == 0)
                            {
                                invokeBindMethod(m_implementationObject, evt.getServiceReference());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Implementation for the ComponentContext interface
     *
     */
    class ComponentContextImpl implements ComponentContext {

        private Bundle m_usingBundle;

        ComponentContextImpl(Bundle usingBundle)
        {
            m_usingBundle = usingBundle;
        }

    	public Dictionary getProperties() {
    		//TODO: 112.11.3.5 The Dictionary is read-only and cannot be modified
    		return m_componentMetadata.getProperties();
    	}

        public Object locateService(String name) {
            DependencyManager dm = getDependencyManager(name);
            if (dm == null || dm.m_boundServicesRefs.isEmpty())
            {
                return null;
            }
            
            ServiceReference selectedRef;
            if (dm.m_boundServicesRefs.size() == 1)
            {
                // short cut for single bound service
                selectedRef = (ServiceReference) dm.m_boundServicesRefs.iterator().next();
            }
            else
            {
                // is it correct to assume an ordered bound services set ? 
                int maxRanking = Integer.MIN_VALUE;
                long minId = Long.MAX_VALUE;
                selectedRef = null;
                
                Iterator it = dm.m_boundServicesRefs.iterator();
                while (it.hasNext())
                {
                    ServiceReference ref = (ServiceReference) it.next();
                    Integer rank = (Integer) ref.getProperty(Constants.SERVICE_RANKING);
                    int ranking = (rank == null) ? Integer.MIN_VALUE : rank.intValue();
                    long id = ((Long) ref.getProperty(Constants.SERVICE_ID)).longValue();
                    if (maxRanking < ranking || (maxRanking == ranking && id < minId))
                    {
                        maxRanking = ranking;
                        minId = id;
                        selectedRef = ref;
                    }
                }
            }
    
            // this is not realistic, as at least one service is available
            // whose service id is smaller than Long.MAX_VALUE, still be sure
            if (selectedRef == null)
            {
                return null;
            }
            
            // return the service for the selected reference
            return getBundleContext().getService(selectedRef);
   	    }

        public Object locateService(String name, ServiceReference ref) {
            DependencyManager dm = getDependencyManager(name);
            if (dm == null || dm.m_boundServicesRefs.isEmpty())
            {
                return null;
            }
            
            // is it correct to assume an ordered bound services set ? 
            Iterator it = dm.m_boundServicesRefs.iterator();
            while (it.hasNext())
            {
                if (it.next().equals(ref))
                {
                    return getBundleContext().getService(ref);
                }
            }
            
            // no matching name and service reference found
            return null;
    	}

        public Object[] locateServices(String name) {
            DependencyManager dm = getDependencyManager(name);
            if (dm == null || dm.m_boundServicesRefs.isEmpty())
            {
                return null;
            }
            
            Object[] services = new Object[dm.m_boundServicesRefs.size()];
            Iterator it = dm.m_boundServicesRefs.iterator();
            for (int i=0; i < services.length && it.hasNext(); i++)
            {
                ServiceReference ref = (ServiceReference) it.next();
                services[i] = getBundleContext().getService(ref);
            }
            return services;
    	}

        private DependencyManager getDependencyManager(String name) {
            Iterator it = m_dependencyManagers.iterator();
            while (it.hasNext())
            {
                DependencyManager dm = (DependencyManager)it.next();
                
                // if any of the dependency managers is unable to bind (it is invalid), the component is deactivated
                if (name.equals(dm.m_dependencyMetadata.getName()))
                {
                    return dm;
                }
            }
            
            // not found
            return null;
        }
        
    	public BundleContext getBundleContext() {
    		return m_activator.getBundleContext();
    	}

    	public Bundle getUsingBundle() {
            return m_usingBundle;
    	}

    	public ComponentInstance getComponentInstance() {
    		return ComponentManagerImpl.this;
    	}

    	public void enableComponent(String name) {
    	    m_activator.enableComponent(name);
    	}

    	public void disableComponent(String name) {
    	    m_activator.disableComponent(name);
    	}

    	public ServiceReference getServiceReference() {
    		if(m_serviceRegistration != null) {
    			return m_serviceRegistration.getReference();
    		}
    		else {
    			return null;
    		}
    	}
    }
    
    /**
     * This class is a ServiceFactory that is used when a delayed component is created.
     * This class returns the same service object instance for all bundles.
     *
     */
    class DelayedComponentServiceFactory implements ServiceFactory {
    	
    	public Object getService(Bundle bundle, ServiceRegistration registration) {
    		
    	    Activator.trace("DelayedComponentServiceFactory.getService()", m_componentMetadata);
    		// When the getServiceMethod is called, the implementation object must be created
    		// unless another bundle has already retrievd it
            
            if (m_implementationObject == null) {
                m_componentContext = new ComponentContextImpl(null);
                m_implementationObject = createImplementationObject( m_componentContext );
            }
            
            return m_implementationObject;
    	}

    	public void ungetService(Bundle bundle, ServiceRegistration registration, Object object) {
            // nothing to do here, delayed components are deactivated when
            // the component is deactivated and not when any bundle releases
            // the service
    	}
        
        protected Object createImplementationObject(ComponentContext componentContext) {
            Object implementationObject;
            
            // 1. Load the component implementation class
            // 2. Create the component instance and component context
            // If the component is not immediate, this is not done at this moment
            try
            {
                // 112.4.4 The class is retrieved with the loadClass method of the component's bundle
                Class c = m_activator.getBundleContext().getBundle().loadClass(m_componentMetadata.getImplementationClassName());
                
                // 112.4.4 The class must be public and have a public constructor without arguments so component instances
                // may be created by the SCR with the newInstance method on Class
                implementationObject = c.newInstance();
            }
            catch (Exception ex)
            {
                // TODO: manage this exception when implementation object cannot be created
                Activator.exception("Error during instantiation of the implementation object",m_componentMetadata,ex);
                deactivate();
                //invalidate();
                return null;
            }
            
            
            // 3. Bind the target services
            Iterator it = m_dependencyManagers.iterator();

            while ( it.hasNext() )
            {
                DependencyManager dm = (DependencyManager)it.next();
                Iterator bound = dm.m_boundServicesRefs.iterator();
                while ( bound.hasNext() ) {
                    ServiceReference nextRef = (ServiceReference) bound.next();                 
                    dm.invokeBindMethod(implementationObject, nextRef);
                }
            }
            
            // 4. Call the activate method, if present
            // Search for the activate method
            try {
                Method activateMethod = getMethod(implementationObject.getClass(), "activate", new Class[]{ComponentContext.class});
                activateMethod.invoke(implementationObject, new Object[]{componentContext});
            }
            catch(NoSuchMethodException ex) {
                // We can safely ignore this one
                Activator.trace("activate() method is not implemented", m_componentMetadata);
            }
            catch(IllegalAccessException ex) {
                // Ignored, but should it be logged?
                Activator.trace("activate() method cannot be called", m_componentMetadata);
            }
            catch(InvocationTargetException ex) {
                // TODO: 112.5.8 If the activate method throws an exception, SCR must log an error message
                // containing the exception with the Log Service
                Activator.exception("The activate method has thrown an exception", m_componentMetadata, ex);
            }
            
            return implementationObject;
        }
    }

    /**
     * This class is a ServiceFactory that is used when a delayed component is created
     * for a service factory service
     *
     */
    class DelayedServiceFactoryServiceFactory extends DelayedComponentServiceFactory
    {
        
        // we do not have to maintain references to the actual service
        // instances as those are handled by the ServiceManager and given
        // to the ungetService method when the bundle releases the service
        
        // maintain the map of componentContext objects created for the
        // service instances
        private IdentityHashMap componentContexts = new IdentityHashMap();
        
        public Object getService( Bundle bundle, ServiceRegistration registration )
        {

            Activator.trace( "DelayedServiceFactoryServiceFactory.getService()", m_componentMetadata );
            // When the getServiceMethod is called, the implementation object must be created

            // private ComponentContext and implementation instances
            ComponentContext componentContext = new ComponentContextImpl( bundle );
            Object implementationObject = createImplementationObject( componentContext );

            // register the components component context
            componentContexts.put( implementationObject, componentContext );

            return implementationObject;
        }

        public void ungetService( Bundle bundle, ServiceRegistration registration, Object implementationObject )
        {
            Activator.trace( "DelayedServiceFactoryServiceFactory.ungetService()", m_componentMetadata );
            // When the ungetServiceMethod is called, the implementation object must be deactivated

            // private ComponentContext and implementation instances
            ComponentContext componentContext = ( ComponentContext ) componentContexts.remove( implementationObject );
            deactivateImplementationObject( implementationObject, componentContext );
        }
        
        protected void deactivateImplementationObject( Object implementationObject, ComponentContext componentContext )
        {
            // 1. Call the deactivate method, if present
            // Search for the activate method
            try
            {
                Method deactivateMethod = getMethod( implementationObject.getClass(), "deactivate", new Class[]
                    { ComponentContext.class } );
                deactivateMethod.invoke( implementationObject, new Object[]
                    { componentContext } );
            }
            catch ( NoSuchMethodException ex )
            {
                // We can safely ignore this one
                Activator.trace( "deactivate() method is not implemented", m_componentMetadata );
            }
            catch ( IllegalAccessException ex )
            {
                // Ignored, but should it be logged?
                Activator.trace( "deactivate() method cannot be called", m_componentMetadata );
            }
            catch ( InvocationTargetException ex )
            {
                // TODO: 112.5.12 If the deactivate method throws an exception, SCR must log an error message
                // containing the exception with the Log Service and continue
                Activator.exception( "The deactivate method has thrown an exception", m_componentMetadata, ex );
            }

            // 2. Unbind any bound services
            Iterator it = m_dependencyManagers.iterator();

            while ( it.hasNext() )
            {
                DependencyManager dm = ( DependencyManager ) it.next();
                Iterator bound = dm.m_boundServicesRefs.iterator();
                while ( bound.hasNext() )
                {
                    ServiceReference nextRef = ( ServiceReference ) bound.next();
                    dm.invokeUnbindMethod( implementationObject, nextRef );
                }
            }

            // 3. Release all references
            // nothing to do, we keep no references on per-Bundle services
        }
    }

    /**
     * Finds the named public or protected method in the given class or any
     * super class. If such a method is found, its accessibility is enfored by
     * calling the <code>Method.setAccessible</code> method if required and
     * the method is returned. Enforcing accessibility is required to support
     * invocation of protected methods.
     * 
     * @param clazz The <code>Class</code> which provides the method.
     * @param name The name of the method.
     * @param parameterTypes The parameters to the method. Passing
     *      <code>null</code> is equivalent to using an empty array.
     *      
     * @return The named method with enforced accessibility
     * 
     * @throws NoSuchMethodException If no public or protected method with
     *      the given name can be found in the class or any of its super classes.
     */
    private Method getMethod(Class clazz, String name, Class[] parameterTypes)
        throws NoSuchMethodException 
    {
        // try the default mechanism first, which only yields public methods
        try
        {
            return clazz.getMethod(name, parameterTypes); 
        }
        catch (NoSuchMethodException nsme)
        {
            // it is ok to not find a public method, try to find a protected now
        }
        
        // now use method declarations, requiring walking up the class
        // hierarchy manually. this algorithm also returns protected methods
        // which is, what we need here
        for ( ; clazz != null; clazz = clazz.getSuperclass()) 
        {
            try 
            {
                Method method = clazz.getDeclaredMethod(name, parameterTypes);
                
                // only accept a protected method, a public method should
                // have been found above and neither private nor package
                // protected methods are acceptable here
                if (Modifier.isProtected(method.getModifiers())) {
                    method.setAccessible(true);
                    return method;
                }
            }
            catch (NoSuchMethodException nsme)
            {
                // ignore for now
            }
        }
        
        // walked up the complete super class hierarchy and still not found
        // anything, sigh ...
        throw new NoSuchMethodException(name);
    }    
}
