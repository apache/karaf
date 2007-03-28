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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The <code>DependencyManager</code> extends the <code>ServiceTracker</code>
 * overwriting the {@link #addingService(ServiceReference)} and
 * {@link #removedService(ServiceReference, Object)} methods to manage the
 * a declared reference of a service component.
 */
class DependencyManager implements ServiceListener
{
    // mask of states ok to send events
    private static final int STATE_MASK = AbstractComponentManager.STATE_UNSATISFIED
        | AbstractComponentManager.STATE_ACTIVATING | AbstractComponentManager.STATE_ACTIVE
        | AbstractComponentManager.STATE_REGISTERED | AbstractComponentManager.STATE_FACTORY;

    // the component to which this dependency belongs
    private AbstractComponentManager m_componentManager;

    // Reference to the metadata
    private ReferenceMetadata m_dependencyMetadata;

    // A flag that defines if the bind method receives a ServiceReference
    private boolean m_bindUsesServiceReference;

    private Map m_tracked;


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
        m_bindUsesServiceReference = false;
        m_tracked = new HashMap();

        // register the service listener
        String filterString = "(" + Constants.OBJECTCLASS + "=" + dependency.getInterface() + ")";
        if ( dependency.getTarget() != null )
        {
            filterString = "(&" + filterString + dependency.getTarget() + ")";
        }
        componentManager.getActivator().getBundleContext().addServiceListener( this, filterString );

        // initial registration of services
        ServiceReference refs[] = componentManager.getActivator().getBundleContext().getServiceReferences( null,
            filterString );
        for ( int i = 0; refs != null && i < refs.length; i++ )
        {
            addingService( refs[i] );
        }
    }


    //---------- ServiceListener interface ------------------------------------

    public void serviceChanged( ServiceEvent event )
    {
        switch ( event.getType() )
        {
            case ServiceEvent.REGISTERED:
                addingService( event.getServiceReference() );
                break;
            case ServiceEvent.MODIFIED:
                removedService( event.getServiceReference() );
                addingService( event.getServiceReference() );
                break;
            case ServiceEvent.UNREGISTERING:
                removedService( event.getServiceReference() );
                break;
        }
    }


    //---------- Service tracking support -------------------------------------

    /**
     * Stops using this dependency manager
     */
    void close()
    {
        BundleContext context = m_componentManager.getActivator().getBundleContext();
        context.removeServiceListener( this );

        synchronized ( m_tracked )
        {
            for ( Iterator ri = m_tracked.keySet().iterator(); ri.hasNext(); )
            {
                ServiceReference sr = ( ServiceReference ) ri.next();
                context.ungetService( sr );
                ri.remove();
            }
        }
    }


    /**
     * Returns the number of services currently tracked
     */
    int size()
    {
        synchronized ( m_tracked )
        {
            return m_tracked.size();
        }
    }


    /**
     * Returns a single (unspecified) service reference
     */
    ServiceReference getServiceReference()
    {
        synchronized ( m_tracked )
        {
            if ( m_tracked.size() > 0 )
            {
                return ( ServiceReference ) m_tracked.keySet().iterator().next();
            }

            return null;
        }
    }


    /**
     * Returns an array of service references of the currently tracked
     * services
     */
    ServiceReference[] getServiceReferences()
    {
        synchronized ( m_tracked )
        {
            if ( m_tracked.size() > 0 )
            {
                return ( ServiceReference[] ) m_tracked.keySet().toArray( new ServiceReference[m_tracked.size()] );
            }

            return null;
        }
    }


    /**
     * Returns the service described by the ServiceReference
     */
    Object getService( ServiceReference serviceReference )
    {
        synchronized ( m_tracked )
        {
            return m_tracked.get( serviceReference );
        }
    }


    /**
     * Returns a single service instance
     */
    Object getService()
    {
        synchronized ( m_tracked )
        {
            if ( m_tracked.size() > 0 )
            {
                return m_tracked.values().iterator().next();
            }

            return null;
        }
    }


    /**
     * Returns an array of service references of the currently tracked
     * services
     */
    Object[] getServices()
    {
        synchronized ( m_tracked )
        {
            if ( m_tracked.size() > 0 )
            {
                return m_tracked.values().toArray( new ServiceReference[m_tracked.size()] );
            }

            return null;
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
     * Returns <code>true</code> if we have at least one service reference or
     * the dependency is optional.
     */
    boolean isValid()
    {
        return size() > 0 || m_dependencyMetadata.isOptional();
    }


    /**
     * initializes a dependency. This method binds all of the service
     * occurrences to the instance object
     * 
     * @return true if the operation was successful, false otherwise
     */
    boolean bind( Object instance )
    {
        // If no references were received, we have to check if the dependency
        // is optional, if it is not then the dependency is invalid
        if ( !isValid() )
        {
            return false;
        }

        // if the instance is null, we do nothing actually but assume success
        // the instance might be null in the delayed component situation
        if ( instance == null )
        {
            return true;
        }

        // Get service references
        ServiceReference refs[] = getServiceReferences();

        // refs can be null if the dependency is optional
        if ( refs != null )
        {
            int max = 1;
            boolean retval = true;

            if ( m_dependencyMetadata.isMultiple() == true )
            {
                max = refs.length;
            }

            for ( int index = 0; index < max; index++ )
            {
                retval = invokeBindMethod( instance, refs[index], getService( refs[index] ) );
                if ( retval == false && ( max == 1 ) )
                {
                    // There was an exception when calling the bind method
                    Activator.error( "Dependency Manager: Possible exception in the bind method during initialize()",
                        m_componentManager.getComponentMetadata() );
                    return false;
                }
            }
        }

        return true;
    }


    /**
     * Revoke all bindings. This method cannot throw an exception since it must
     * try to complete all that it can
     */
    void unbind( Object instance )
    {
        // if the instance is null, we do nothing actually
        // the instance might be null in the delayed component situation
        if ( instance == null )
        {
            return;
        }

        ServiceReference[] allrefs = getServiceReferences();

        if ( allrefs == null )
            return;

        for ( int i = 0; i < allrefs.length; i++ )
        {
            invokeUnbindMethod( instance, allrefs[i], getService( allrefs[i] ) );
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
        Method method = null;

        Class parameterClass = null;

        // 112.3.1 The method is searched for using the following priority
        // 1. The method's parameter type is org.osgi.framework.ServiceReference
        // 2. The method's parameter type is the type specified by the
        // reference's interface attribute
        // 3. The method's parameter type is assignable from the type specified
        // by the reference's interface attribute
        try
        {
            // Case 1

            method = AbstractComponentManager.getMethod( targetClass, methodname, new Class[]
                { ServiceReference.class } );

            m_bindUsesServiceReference = true;
        }
        catch ( NoSuchMethodException ex )
        {

            try
            {
                // Case2

                m_bindUsesServiceReference = false;

                parameterClass = m_componentManager.getActivator().getBundleContext().getBundle().loadClass(
                    parameterClassName );

                method = AbstractComponentManager.getMethod( targetClass, methodname, new Class[]
                    { parameterClass } );
            }
            catch ( NoSuchMethodException ex2 )
            {

                // Case 3
                method = null;

                // iterate on class hierarchy
                for ( ; method == null && targetClass != null; targetClass = targetClass.getSuperclass() )
                {
                    // Get all potential bind methods
                    Method candidateBindMethods[] = targetClass.getDeclaredMethods();

                    // Iterate over them
                    for ( int i = 0; method == null && i < candidateBindMethods.length; i++ )
                    {
                        Method currentMethod = candidateBindMethods[i];

                        // Get the parameters for the current method
                        Class[] parameters = currentMethod.getParameterTypes();

                        // Select only the methods that receive a single
                        // parameter
                        // and a matching name
                        if ( parameters.length == 1 && currentMethod.getName().equals( methodname ) )
                        {

                            // Get the parameter type
                            Class theParameter = parameters[0];

                            // Check if the parameter type is assignable from
                            // the type specified by the reference's interface
                            // attribute
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
                                    method = currentMethod;

                                }
                            }
                        }
                    }
                }
            }
            catch ( ClassNotFoundException ex2 )
            {
                Activator.exception( "Cannot load class used as parameter " + parameterClassName, m_componentManager
                    .getComponentMetadata(), ex2 );
            }
        }

        return method;
    }


    /**
     * Call the bind method. In case there is an exception while calling the
     * bind method, the service is not considered to be bound to the instance
     * object
     * 
     * @param implementationObject The object to which the service is bound
     * @param ref A ServiceReference with the service that will be bound to the
     *            instance object
     * @param storeRef A boolean that indicates if the reference must be stored
     *            (this is used for the delayed components)
     * @return true if the call was successful, false otherwise
     */
    private boolean invokeBindMethod( Object implementationObject, ServiceReference ref, Object service )
    {
        // The bind method is only invoked if the implementation object is not
        // null. This is valid
        // for both immediate and delayed components
        if ( implementationObject != null )
        {

            try
            {
                // Get the bind method
                Activator.trace( "getting bind: " + m_dependencyMetadata.getBind(), m_componentManager
                    .getComponentMetadata() );
                Method bindMethod = getBindingMethod( m_dependencyMetadata.getBind(), implementationObject.getClass(),
                    m_dependencyMetadata.getInterface() );

                if ( bindMethod == null )
                {
                    // 112.3.1 If the method is not found , SCR must log an
                    // error
                    // message with the log service, if present, and ignore the
                    // method
                    Activator.error( "bind() method not found", m_componentManager.getComponentMetadata() );
                    return false;
                }

                // Get the parameter
                Object parameter;

                if ( m_bindUsesServiceReference == false )
                {
                    parameter = service;
                }
                else
                {
                    parameter = ref;
                }

                // Invoke the method
                bindMethod.invoke( implementationObject, new Object[]
                    { parameter } );

                Activator.trace( "bound: " + getName(), m_componentManager.getComponentMetadata() );

                return true;
            }
            catch ( IllegalAccessException ex )
            {
                // 112.3.1 If the method is not is not declared protected or
                // public, SCR must log an error
                // message with the log service, if present, and ignore the
                // method
                Activator.exception( "bind() method cannot be called", m_componentManager.getComponentMetadata(), ex );
                return false;
            }
            catch ( InvocationTargetException ex )
            {
                Activator.exception( "DependencyManager : exception while invoking " + m_dependencyMetadata.getBind()
                    + "()", m_componentManager.getComponentMetadata(), ex );
                return false;
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
     * Call the unbind method
     * 
     * @param implementationObject The object from which the service is unbound
     * @param ref A service reference corresponding to the service that will be
     *            unbound
     * @return true if the call was successful, false otherwise
     */
    private boolean invokeUnbindMethod( Object implementationObject, ServiceReference ref, Object service )
    {
        // The unbind method is only invoked if the implementation object is not
        // null. This is valid for both immediate and delayed components
        if ( implementationObject != null )
        {
            try
            {
                Activator.trace( "getting unbind: " + m_dependencyMetadata.getUnbind(), m_componentManager
                    .getComponentMetadata() );
                Method unbindMethod = getBindingMethod( m_dependencyMetadata.getUnbind(), implementationObject
                    .getClass(), m_dependencyMetadata.getInterface() );

                // Recover the object that is bound from the map.
                // Object parameter = m_boundServices.get(ref);
                Object parameter = null;

                if ( m_bindUsesServiceReference == true )
                {
                    parameter = ref;
                }
                else
                {
                    parameter = service;
                }

                if ( unbindMethod == null )
                {
                    // 112.3.1 If the method is not found , SCR must log an
                    // error
                    // message with the log service, if present, and ignore the
                    // method
                    Activator.error( "unbind() method not found", m_componentManager.getComponentMetadata() );
                    return false;
                }

                unbindMethod.invoke( implementationObject, new Object[]
                    { parameter } );

                Activator.trace( "unbound: " + getName(), m_componentManager.getComponentMetadata() );

                return true;
            }
            catch ( IllegalAccessException ex )
            {
                // 112.3.1 If the method is not is not declared protected or
                // public, SCR must log an error
                // message with the log service, if present, and ignore the
                // method
                Activator.exception( "unbind() method cannot be called", m_componentManager.getComponentMetadata(), ex );
                return false;
            }
            catch ( InvocationTargetException ex )
            {
                Activator.exception( "DependencyManager : exception while invoking " + m_dependencyMetadata.getUnbind()
                    + "()", m_componentManager.getComponentMetadata(), ex );
                return false;
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


    private void addingService( ServiceReference reference )
    {
        // get the service and keep it here (for now or later)
        Object service = m_componentManager.getActivator().getBundleContext().getService( reference );
        synchronized ( m_tracked )
        {
            m_tracked.put( reference, service );
        }

        // forward the event if in event hanlding state
        if ( handleServiceEvent() )
        {

            // the component is UNSATISFIED if enabled but any of the references
            // have been missing when activate was running the last time or
            // the component has been deactivated
            if ( m_componentManager.getState() == AbstractComponentManager.STATE_UNSATISFIED )
            {
                m_componentManager.activate();
            }

            // Otherwise, this checks for dynamic 0..1, 0..N, and 1..N
            // it never
            // checks for 1..1 dynamic which is done above by the
            // validate()
            else if ( !m_dependencyMetadata.isStatic() )
            {
                // For dependency that are aggregates, always bind the
                // service
                // Otherwise only bind if bind services is zero, which
                // captures the 0..1 case
                // (size is still zero as we are called for the first service)
                if ( m_dependencyMetadata.isMultiple() || size() == 0 )
                {
                    invokeBindMethod( m_componentManager.getInstance(), reference, service );
                }
            }
        }
    }


    public void removedService( ServiceReference reference )
    {
        // remove the service from the internal registry, ignore if not cached
        Object service;
        synchronized ( m_tracked )
        {
            service = m_tracked.remove( reference );
        }

        // do nothing in the unlikely case that we do not have it cached
        if ( service == null )
        {
            return;
        }

        if ( handleServiceEvent() )
        {
            // A static dependency is broken the instance manager will
            // be invalidated
            if ( m_dependencyMetadata.isStatic() )
            {
                // setStateDependency(DependencyChangeEvent.DEPENDENCY_INVALID);
                try
                {
                    Activator.trace( "Dependency Manager: Static dependency is broken", m_componentManager
                        .getComponentMetadata() );
                    m_componentManager.reactivate();
                }
                catch ( Exception ex )
                {
                    Activator.exception( "Exception while recreating dependency ", m_componentManager
                        .getComponentMetadata(), ex );
                }
            }
            // dynamic dependency
            else
            {
                // Release references to the service, call unbinder
                // method
                // and eventually request service unregistration
                Object instance = m_componentManager.getInstance();
                invokeUnbindMethod( instance, reference, service );

                // The only thing we need to do here is check if we can
                // reinitialize
                // once the bound services becomes zero. This tries to
                // repair dynamic
                // 1..1 or rebind 0..1, since replacement services may
                // be available.
                // In the case of aggregates, this will only invalidate
                // them since they
                // can't be repaired.
                if ( size() == 0 )
                {
                    // try to reinitialize
                    if ( !bind( instance ) )
                    {
                        if ( !m_dependencyMetadata.isOptional() )
                        {
                            Activator
                                .trace(
                                    "Dependency Manager: Mandatory dependency not fullfilled and no replacements available... unregistering service...",
                                    m_componentManager.getComponentMetadata() );
                            m_componentManager.reactivate();
                        }
                    }
                }
            }
        }

        // finally unget the service
        m_componentManager.getActivator().getBundleContext().ungetService( reference );
    }


    private boolean handleServiceEvent()
    {
        return ( m_componentManager.getState() & STATE_MASK ) != 0;
        //        return state != AbstractComponentManager.INSTANCE_DESTROYING
        //            && state != AbstractComponentManager.INSTANCE_DESTROYED
        //            && state != AbstractComponentManager.INSTANCE_CREATING 
        //            && state != AbstractComponentManager.INSTANCE_CREATED;
    }
}
