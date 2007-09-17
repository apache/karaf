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


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;


/**
 * The <code>DependencyManager</code> manages the references to services
 * declared by a single <code>&lt;reference&gt;</code element in component
 * descriptor.
 */
class DependencyManager implements ServiceListener
{
    // mask of states ok to send events
    private static final int STATE_MASK = AbstractComponentManager.STATE_UNSATISFIED
        | AbstractComponentManager.STATE_ACTIVATING | AbstractComponentManager.STATE_ACTIVE
        | AbstractComponentManager.STATE_REGISTERED | AbstractComponentManager.STATE_FACTORY;
    
    // the ServiceReference class instance
    private static final Class SERVICE_REFERENCE_CLASS = ServiceReference.class;
    
    // pseudo service to mark a bound service without actual service instance
    private static final Object BOUND_SERVICE_SENTINEL = new Object();

    // the component to which this dependency belongs
    private AbstractComponentManager m_componentManager;

    // Reference to the metadata
    private ReferenceMetadata m_dependencyMetadata;

    // The map of bound services indexed by their ServiceReference
    private Map m_bound;
    
    // the number of matching services registered in the system
    private int m_size;

    // the bind method
    private Method m_bind;
    
    // whether the bind method takes a service reference
    private boolean m_bindUsesReference;
    
    // the unbind method
    private Method m_unbind;
    
    // whether the unbind method takes a service reference
    private boolean m_unbindUsesReference;

    /**
     * Constructor that receives several parameters.
     *
     * @param dependency An object that contains data about the dependency
     */
    DependencyManager( AbstractComponentManager componentManager, ReferenceMetadata dependency )
        throws InvalidSyntaxException
    {
        m_componentManager = componentManager;
        m_dependencyMetadata = dependency;
        m_bound = Collections.synchronizedMap( new HashMap() );

        // register the service listener
        String filterString = "(" + Constants.OBJECTCLASS + "=" + dependency.getInterface() + ")";
        if ( dependency.getTarget() != null )
        {
            filterString = "(&" + filterString + dependency.getTarget() + ")";
        }
        componentManager.getActivator().getBundleContext().addServiceListener( this, filterString );

        // get the current number of registered services available
        ServiceReference refs[] = getServiceReferences();
        m_size = (refs == null) ? 0 : refs.length;
    }


    //---------- ServiceListener interface ------------------------------------

    public void serviceChanged( ServiceEvent event )
    {
        switch ( event.getType() )
        {
            case ServiceEvent.REGISTERED:
                m_size++;
                serviceAdded( event.getServiceReference() );
                break;
            case ServiceEvent.MODIFIED:
                serviceRemoved( event.getServiceReference() );
                serviceAdded( event.getServiceReference() );
                break;
            case ServiceEvent.UNREGISTERING:
                m_size--;
                serviceRemoved( event.getServiceReference() );
                break;
        }
    }


    private void serviceAdded( ServiceReference reference )
    {
        // if the component is currently unsatisfied, it may become satisfied
        // by adding this service, try to activate
        if ( m_componentManager.getState() == AbstractComponentManager.STATE_UNSATISFIED )
        {
            m_componentManager.activate();
        }

        // otherwise check whether the component is in a state to handle the event
        else if ( handleServiceEvent() )
        {
            // if the dependency is static and adding the service has an
            // influence on service binding because the dependency is multiple
            // or optional and unbound, the component needs to be reactivated
            if ( m_dependencyMetadata.isStatic() )
            {
                // only reactivate if the service has an influence on binding
                if ( m_dependencyMetadata.isMultiple() || !isBound() )
                {
                    m_componentManager.reactivate();
                }
            }

            // otherwise bind if we have a bind method and the service needs
            // be bound
            else if ( m_dependencyMetadata.getBind() != null && ( m_dependencyMetadata.isMultiple() || !isBound() ) )
            {
                // bind the service, getting it if required
                invokeBindMethod( m_componentManager.getInstance(), reference );
            }
        }
    }


    private void serviceRemoved( ServiceReference reference )
    {
        // check whether we are bound to that service, do nothing if not
        if ( getBoundService( reference ) == null )
        {
            return;
        }

        if ( handleServiceEvent() )
        {
            // if the dependency is not satisfied anymore, we have to
            // deactivate the component 
            if ( !isSatisfied() )
            {
                m_componentManager.getActivator()
                    .log(
                        LogService.LOG_DEBUG,
                        "Dependency Manager: Deactivating component due to mandatory dependency on "
                            + m_dependencyMetadata.getName() + "/" + m_dependencyMetadata.getInterface()
                            + " not satisfied", m_componentManager.getComponentMetadata(), null );

                // deactivate the component now
                m_componentManager.deactivate();
            }

            // if the dependency is static, we have to reactivate the component
            // to "remove" the dependency
            else if ( m_dependencyMetadata.isStatic() )
            {
                try
                {
                    m_componentManager.getActivator().log(
                        LogService.LOG_DEBUG,
                        "Dependency Manager: Static dependency on " + m_dependencyMetadata.getName() + "/"
                            + m_dependencyMetadata.getInterface() + " is broken",
                        m_componentManager.getComponentMetadata(), null );
                    m_componentManager.reactivate();
                }
                catch ( Exception ex )
                {
                    m_componentManager.getActivator().log( LogService.LOG_ERROR,
                        "Exception while recreating dependency ", m_componentManager.getComponentMetadata(), ex );
                }
            }

            // dynamic dependency, multiple or single but this service is the bound one
            else
            {

                // the component instance to unbind/bind services
                Object instance = m_componentManager.getInstance();

                // call the unbind method if one is defined
                if ( m_dependencyMetadata.getUnbind() != null )
                {
                    invokeUnbindMethod( instance, reference );
                }
                
                // if binding to another service fails for a singleton
                // reference, we have to deactivate the component
                if ( !m_dependencyMetadata.isMultiple() )
                {
                    // in the unexpected case that rebinding fails, we will
                    // deactivate the component
                    if ( !bind( instance ) )
                    {
                        m_componentManager.getActivator().log(
                            LogService.LOG_DEBUG,
                            "Dependency Manager: Deactivating component due to mandatory dependency on "
                                + m_dependencyMetadata.getName() + "/" + m_dependencyMetadata.getInterface()
                                + " not satisfied", m_componentManager.getComponentMetadata(), null );
                        m_componentManager.deactivate();

                    }
                }
            }
        }
    }


    private boolean handleServiceEvent()
    {
        return ( m_componentManager.getState() & STATE_MASK ) != 0;
        //        return state != AbstractComponentManager.INSTANCE_DESTROYING
        //            && state != AbstractComponentManager.INSTANCE_DESTROYED
        //            && state != AbstractComponentManager.INSTANCE_CREATING
        //            && state != AbstractComponentManager.INSTANCE_CREATED;
    }

    //---------- Service tracking support -------------------------------------

    /**
     * Stops using this dependency manager
     */
    void close()
    {
        BundleContext context = m_componentManager.getActivator().getBundleContext();
        context.removeServiceListener( this );

        m_size = 0;

        // unget all services we once got
        ServiceReference[] boundRefs = getBoundServiceReferences();
        if ( boundRefs != null )
        {
            for ( int i = 0; i < boundRefs.length; i++ )
            {
                ungetService( boundRefs[i] );
            }
        }
        
        // drop the method references (to help GC)
        m_bind = null;
        m_unbind = null;
    }


    /**
     * Returns the number of services currently registered in the system,
     * which match the service criteria (interface and optional target filter)
     * configured for this dependency. The number returned by this method has
     * no correlation to the number of services bound to this dependency
     * manager. It is actually the maximum number of services which may be
     * bound to this dependency manager.
     * 
     * @see #isValid()
     */
    int size()
    {
        return m_size;
    }


    /**
     * Returns the first service reference returned by the
     * {@link #getServiceReferences()} method or <code>null</code> if no
     * matching service can be found. 
     */
    ServiceReference getServiceReference()
    {
        ServiceReference[] sr = getServiceReferences();
        return ( sr != null && sr.length > 0 ) ? sr[0] : null;
    }


    /**
     * Returns an array of <code>ServiceReference</code> instances for services
     * implementing the interface and complying to the (optional) target filter
     * declared for this dependency. If no matching service can be found
     * <code>null</code> is returned. If the configured target filter is
     * syntactically incorrect an error message is logged with the LogService
     * and <code>null</code> is returned.
     * <p>
     * This method always directly accesses the framework's service registry
     * and ignores the services bound by this dependency manager.
     */
    ServiceReference[] getServiceReferences()
    {
        try
        {
            return m_componentManager.getActivator().getBundleContext().getServiceReferences(
                m_dependencyMetadata.getInterface(), m_dependencyMetadata.getTarget() );
        }
        catch ( InvalidSyntaxException ise )
        {
            m_componentManager.getActivator().log( LogService.LOG_ERROR,
                "Unexpected problem with filter '" + m_dependencyMetadata.getTarget() + "'",
                m_componentManager.getComponentMetadata(), ise );
            return null;
        }
    }


    /**
     * Returns the service instance for the service reference returned by the
     * {@link #getServiceReference()} method. If this returns a
     * non-<code>null</code> service instance the service is then considered
     * bound to this instance.
     */
    Object getService()
    {
        ServiceReference sr = getServiceReference();
        return ( sr != null ) ? getService( sr ) : null;
    }


    /**
     * Returns an array of service instances for the service references returned
     * by the {@link #getServiceReference()} method. If no services match the
     * criteria configured for this dependency <code>null</code> is returned.
     * All services returned by this method will be considered bound after this
     * method returns.
     */
    Object[] getServices()
    {
        ServiceReference[] sr = getServiceReferences();
        if ( sr == null || sr.length == 0 )
        {
            return null;
        }

        List services = new ArrayList();
        for ( int i = 0; i < sr.length; i++ )
        {
            Object service = getService( sr[i] );
            if ( service != null )
            {
                services.add( service );
            }
        }

        return ( services.size() > 0 ) ? services.toArray() : null;
    }

    
    //---------- bound services maintenance -----------------------------------

    /**
     * Returns an array of <code>ServiceReference</code> instances of all
     * services this instance is bound to.
     */
    private ServiceReference[] getBoundServiceReferences()
    {
        return ( ServiceReference[] ) m_bound.keySet().toArray( new ServiceReference[m_bound.size()] );
    }


    /**
     * Returns <code>true</code> if at least one service has been bound
     */
    private boolean isBound()
    {
        return !m_bound.isEmpty();
    }


    // TODO
    private void bindService( ServiceReference serviceReference )
    {
        m_bound.put( serviceReference, BOUND_SERVICE_SENTINEL );
    }


    /**
     * Returns the bound service represented by the given service reference
     * or <code>null</code> if this is instance is not currently bound to that
     * service.
     * 
     * @param serviceReference The reference to the bound service
     * 
     * @return the service for the reference or the {@link #BOUND_SERVICE_SENTINEL}
     *      if the service is bound or <code>null</code> if the service is not
     *      bound.
     */
    private Object getBoundService( ServiceReference serviceReference )
    {
        return m_bound.get( serviceReference );
    }


    /**
     * Returns the service described by the ServiceReference. If this instance
     * is already bound the given service, that bound service instance is
     * returned. Otherwise the service retrieved from the service registry
     * and kept as a bound service for future use.
     * 
     * @param serviceReference The reference to the service to be returned
     * 
     * @return The requested service or <code>null</code> if no service is
     *      registered for the service reference (any more).
     */
    Object getService( ServiceReference serviceReference )
    {
        // check whether we already have the service and return that one
        Object service = getBoundService( serviceReference );
        if ( service != null && service != BOUND_SERVICE_SENTINEL )
        {
            return service;
        }

        // otherwise acquire the service and keep it
        service = m_componentManager.getActivator().getBundleContext().getService( serviceReference );
        if ( service != null )
        {
            m_bound.put( serviceReference, service );
        }

        // returne the acquired service (may be null of course)
        return service;
    }


    /**
     * Ungets the service described by the ServiceReference and removes it from
     * the list of bound services.
     */
    void ungetService( ServiceReference serviceReference )
    {
        // check we really have this service, do nothing if not
        Object service = m_bound.remove( serviceReference );
        if ( service != null && service != BOUND_SERVICE_SENTINEL )
        {
            m_componentManager.getActivator().getBundleContext().ungetService( serviceReference );
        }
    }


    //---------- DependencyManager core ---------------------------------------

    /**
     * Returns the name of the service reference.
     */
    String getName()
    {
        return m_dependencyMetadata.getName();
    }


    /**
     * Returns <code>true</code> if this dependency manager is satisfied, that
     * is if eithern the dependency is optional or the number of services
     * registered in the framework and available to this dependency manager is
     * not zero.
     */
    boolean isSatisfied()
    {
        return size() > 0 || m_dependencyMetadata.isOptional();
    }


    /**
     * initializes a dependency. This method binds all of the service
     * occurrences to the instance object
     *
     * @return true if the dependency is satisfied and at least the minimum
     *      number of services could be bound. Otherwise false is returned.
     */
    boolean bind( Object instance )
    {
        // If no references were received, we have to check if the dependency
        // is optional, if it is not then the dependency is invalid
        if ( !isSatisfied() )
        {
            return false;
        }

        // if no bind method is configured or if this is a delayed component,
        // we have nothing to do and just signal success
        if ( instance == null || m_dependencyMetadata.getBind() == null )
        {
            return true;
        }

        // Get service references
        ServiceReference refs[] = getServiceReferences();

        // refs can be null if the dependency is optional
        if ( refs == null )
        {
            return true;
        }
        
        // assume success to begin with: if the dependency is optional,
        // we don't care, whether we can bind a service. Otherwise, we
        // require at least one service to be bound, thus we require
        // flag being set in the loop below
        boolean success = m_dependencyMetadata.isOptional();
        
        // number of services to bind
        for ( int index = 0; index < refs.length; index++ )
        {
            // success is if we have the minimal required number of services bound
            if ( invokeBindMethod( instance, refs[index] ) )
            {
                // of course, we have success if the service is bound
                success = true;

                // if the reference is not multiple, we are already done
                if ( !m_dependencyMetadata.isMultiple() )
                {
                    break;
                }
            }
        }

        // success will be true, if the service is optional or if at least
        // one service was available to be bound (regardless of whether the
        // bind method succeeded or not)
        return success;
    }


    /**
     * Revoke all bindings. This method cannot throw an exception since it must
     * try to complete all that it can
     */
    void unbind( Object instance )
    {
        // if the instance is null, we do nothing actually
        // the instance might be null in the delayed component situation.
        // Additionally, we do nothing here in case there is no configured
        // unbind method.
        if ( instance == null || m_dependencyMetadata.getUnbind() == null )
        {
            return;
        }

        ServiceReference[] boundRefs = getBoundServiceReferences();
        if ( boundRefs != null )
        {
            for ( int i = 0; i < boundRefs.length; i++ )
            {
                invokeUnbindMethod( instance, boundRefs[i] );
            }
        }
    }


    /**
     * Gets a bind or unbind method according to the policies described in the
     * specification
     *
     * @param methodname The name of the method
     * @param targetClass the class to which the method belongs to
     * @param parameterClassName the name of the class of the parameter that is
     *            passed to the method
     * @return the method or null
     * @throws java.lang.ClassNotFoundException if the class was not found
     */
    private Method getBindingMethod( String methodname, Class targetClass, String parameterClassName )
    {
        Class parameterClass = null;

        // 112.3.1 The method is searched for using the following priority
        // 1. The method's parameter type is org.osgi.framework.ServiceReference
        // 2. The method's parameter type is the type specified by the
        // reference's interface attribute
        // 3. The method's parameter type is assignable from the type specified
        // by the reference's interface attribute
        try
        {
            // Case 1 - ServiceReference parameter
            return AbstractComponentManager.getMethod( targetClass, methodname, new Class[]
                { SERVICE_REFERENCE_CLASS }, true );
        }
        catch ( NoSuchMethodException ex )
        {

            try
            {
                // Case2 - Service object parameter
                parameterClass = m_componentManager.getActivator().getBundleContext().getBundle().loadClass(
                    parameterClassName );
                return AbstractComponentManager.getMethod( targetClass, methodname, new Class[]
                    { parameterClass }, true );
            }
            catch ( NoSuchMethodException ex2 )
            {

                // Case 3 - Service interface assignement compatible methods

                // Get all potential bind methods
                Method candidateBindMethods[] = targetClass.getDeclaredMethods();

                // Iterate over them
                for ( int i = 0; i < candidateBindMethods.length; i++ )
                {
                    Method method = candidateBindMethods[i];

                    // Get the parameters for the current method
                    Class[] parameters = method.getParameterTypes();

                    // Select only the methods that receive a single
                    // parameter
                    // and a matching name
                    if ( parameters.length == 1 && method.getName().equals( methodname ) )
                    {

                        // Get the parameter type
                        Class theParameter = parameters[0];

                        // Check if the parameter type is ServiceReference
                        // or is assignable from the type specified by the
                        // reference's interface attribute
                        if ( theParameter.isAssignableFrom( parameterClass ) )
                        {

                            // Final check: it must be public or protected
                            if ( Modifier.isPublic( method.getModifiers() )
                                || Modifier.isProtected( method.getModifiers() ) )
                            {
                                if ( !method.isAccessible() )
                                {
                                    method.setAccessible( true );
                                }
                                return method;
                            }
                        }
                    }
                }
            }
            catch ( ClassNotFoundException ex2 )
            {
                m_componentManager.getActivator().log( LogService.LOG_ERROR,
                    "Cannot load class used as parameter " + parameterClassName,
                    m_componentManager.getComponentMetadata(), ex2 );
            }
        }

        // if we get here, we have no method, so check the super class
        targetClass = targetClass.getSuperclass();
        return ( targetClass != null ) ? getBindingMethod( methodname, targetClass, parameterClassName ) : null;
    }


    /**
     * Calls the bind method. In case there is an exception while calling the
     * bind method, the service is not considered to be bound to the instance
     * object
     * <p>
     * If the reference is singular and a service has already been bound to the
     * component this method has no effect and just returns <code>true</code>.
     *
     * @param implementationObject The object to which the service is bound
     * @param ref A ServiceReference with the service that will be bound to the
     *            instance object
     * @return true if the service should be considered bound. If no bind
     *      method is found or the method call fails, <code>true</code> is
     *      returned. <code>false</code> is only returned if the service must
     *      be handed over to the bind method but the service cannot be
     *      retrieved using the service reference.
     */
    private boolean invokeBindMethod( Object implementationObject, ServiceReference ref )
    {
        // The bind method is only invoked if the implementation object is not
        // null. This is valid for both immediate and delayed components
        if ( implementationObject != null )
        {
            try
            {
                // Get the bind method
                m_componentManager.getActivator().log( LogService.LOG_DEBUG,
                    "getting bind: " + m_dependencyMetadata.getBind(), m_componentManager.getComponentMetadata(), null );
                if (m_bind == null) {
                    m_bind = getBindingMethod( m_dependencyMetadata.getBind(), implementationObject.getClass(),
                        m_dependencyMetadata.getInterface() );

                    // 112.3.1 If the method is not found , SCR must log an error
                    // message with the log service, if present, and ignore the
                    // method
                    if ( m_bind == null )
                    {
                        m_componentManager.getActivator().log( LogService.LOG_ERROR, "bind() method not found",
                            m_componentManager.getComponentMetadata(), null );
                        return true;
                    }
                    
                    // cache whether the bind method takes a reference
                    m_bindUsesReference = SERVICE_REFERENCE_CLASS.equals( m_bind.getParameterTypes()[0] );
                }

                // Get the parameter
                Object parameter;
                if ( m_bindUsesReference )
                {
                    parameter = ref;
                    
                    // mark this service as bound using the special sentinel
                    bindService( ref );
                }
                else
                {
                    // get the service, fail binding if the service is not
                    // available (any more)
                    parameter = getService( ref );
                    if ( parameter == null )
                    {
                        m_componentManager.getActivator().log( LogService.LOG_INFO,
                            "Dependency Manager: Service " + ref + " has already gone, not binding",
                            m_componentManager.getComponentMetadata(), null );
                        return false;
                    }
                }

                // Invoke the method
                m_bind.invoke( implementationObject, new Object[]
                    { parameter } );

                m_componentManager.getActivator().log( LogService.LOG_DEBUG, "bound: " + getName(),
                    m_componentManager.getComponentMetadata(), null );

                return true;
            }
            catch ( IllegalAccessException ex )
            {
                // 112.3.1 If the method is not is not declared protected or
                // public, SCR must log an error message with the log service,
                // if present, and ignore the method
                m_componentManager.getActivator().log( LogService.LOG_ERROR, "bind() method cannot be called",
                    m_componentManager.getComponentMetadata(), ex );
                return true;
            }
            catch ( InvocationTargetException ex )
            {
                // 112.5.7 If a bind method throws an exception, SCR must log an
                // error message containing the exception [...]
                m_componentManager.getActivator().log( LogService.LOG_ERROR,
                    "DependencyManager : exception while invoking " + m_dependencyMetadata.getBind() + "()",
                    m_componentManager.getComponentMetadata(), ex );
                return true;
            }
        }
        else if ( implementationObject == null && m_componentManager.getComponentMetadata().isImmediate() == false )
        {
            return true;
        }
        else
        {
            // this is not expected: if the component is immediate the
            // implementationObject is not null (asserted by the caller)
            return false;
        }
    }


    /**
     * Calls the unbind method.
     * <p>
     * If the reference is singular and the given service is not the one bound
     * to the component this method has no effect and just returns
     * <code>true</code>.
     *
     * @param implementationObject The object from which the service is unbound
     * @param ref A service reference corresponding to the service that will be
     *            unbound
     * @return true if the call was successful, false otherwise
     */
    private boolean invokeUnbindMethod( Object implementationObject, ServiceReference ref )
    {
        // The unbind method is only invoked if the implementation object is not
        // null. This is valid for both immediate and delayed components
        if ( implementationObject != null )
        {
            try
            {
                // Get the bind method
                m_componentManager.getActivator().log( LogService.LOG_DEBUG,
                    "getting unbind: " + m_dependencyMetadata.getUnbind(), m_componentManager.getComponentMetadata(),
                    null );
                if ( m_unbind == null )
                {
                    m_unbind = getBindingMethod( m_dependencyMetadata.getUnbind(), implementationObject.getClass(),
                        m_dependencyMetadata.getInterface() );

                    if ( m_unbind == null )
                    {
                        // 112.3.1 If the method is not found, SCR must log an error
                        // message with the log service, if present, and ignore the
                        // method
                        m_componentManager.getActivator().log( LogService.LOG_ERROR, "unbind() method not found",
                            m_componentManager.getComponentMetadata(), null );
                        return true;
                    }
                    // cache whether the unbind method takes a reference
                    m_unbindUsesReference = SERVICE_REFERENCE_CLASS.equals( m_unbind.getParameterTypes()[0] );
                }

                // Get the parameter
                Object parameter = null;
                if ( m_unbindUsesReference )
                {
                    parameter = ref;
                }
                else
                {
                    parameter = getService( ref );
                    if ( parameter == null )
                    {
                        m_componentManager.getActivator().log( LogService.LOG_INFO,
                            "Dependency Manager: Service " + ref + " has already gone, not unbinding",
                            m_componentManager.getComponentMetadata(), null );
                        return false;
                    }
                }

                m_unbind.invoke( implementationObject, new Object[]
                    { parameter } );

                m_componentManager.getActivator().log( LogService.LOG_DEBUG, "unbound: " + getName(),
                    m_componentManager.getComponentMetadata(), null );

                return true;
            }
            catch ( IllegalAccessException ex )
            {
                // 112.3.1 If the method is not is not declared protected or
                // public, SCR must log an error message with the log service,
                // if present, and ignore the method
                m_componentManager.getActivator().log( LogService.LOG_ERROR, "unbind() method cannot be called",
                    m_componentManager.getComponentMetadata(), ex );
                return false;
            }
            catch ( InvocationTargetException ex )
            {
                // 112.5.13 If an unbind method throws an exception, SCR must
                // log an error message containing the exception [...]
                m_componentManager.getActivator().log( LogService.LOG_ERROR,
                    "DependencyManager : exception while invoking " + m_dependencyMetadata.getUnbind() + "()",
                    m_componentManager.getComponentMetadata(), ex.getCause() );
                return true;
            }
            finally
            {
                // ensure the service is not cached anymore
                ungetService( ref );
            }

        }
        else if ( implementationObject == null && m_componentManager.getComponentMetadata().isImmediate() == false )
        {
            return true;
        }
        else
        {
            // this is not expected: if the component is immediate the
            // implementationObject is not null (asserted by the caller)
            return false;
        }
    }

}
