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
package org.apache.felix.scr.impl;


import java.lang.reflect.*;
import java.util.*;

import org.apache.felix.scr.Reference;
import org.osgi.framework.*;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.log.LogService;


/**
 * The default ComponentManager. Objects of this class are responsible for managing
 * implementation object's lifecycle.
 *
 */
abstract class AbstractComponentManager implements ComponentManager, ComponentInstance
{
    // the ID of this component
    private long m_componentId;

    // The state of this instance manager
    private int m_state;

    // The metadata
    private ComponentMetadata m_componentMetadata;

    // The dependency managers that manage every dependency
    private List m_dependencyManagers;

    // A reference to the BundleComponentActivator
    private BundleComponentActivator m_activator;

    // The ServiceRegistration
    private ServiceRegistration m_serviceRegistration;

    // lock object used by service registration locking
    private Object serviceRegistrationLock = new Object();

    // the field set to the owner of the lock
    private Thread serviceRegistrationLockOwner;

    /**
     * The constructor receives both the activator and the metadata
     *
     * @param activator
     * @param metadata
     */
    protected AbstractComponentManager( BundleComponentActivator activator, ComponentMetadata metadata, long componentId )
    {
        m_activator = activator;
        m_componentMetadata = metadata;
        m_componentId = componentId;

        m_state = STATE_DISABLED;
        m_dependencyManagers = new ArrayList();

        log( LogService.LOG_DEBUG, "Component created", m_componentMetadata, null );
    }


    //---------- Asynchronous frontend to state change methods ----------------

    /**
     * Enables this component and - if satisfied - also activates it. If
     * enabling the component fails for any reason, the component ends up
     * disabled.
     * <p>
     * This method ignores the <i>enabled</i> flag of the component metadata
     * and just enables as requested.
     * <p>
     * This method schedules the enablement for asynchronous execution.
     */
    public final void enable()
    {
        getActivator().schedule( new Runnable()
        {
            public void run()
            {
                enableInternal();
            }
        } );
    }


    /**
     * Enables this component and - if satisfied - also activates it
     * synchronously or asynchronously. If enabling the component fails for
     * any reason, the component ends up disabled.
     * <p>
     * This method ignores the <i>enabled</i> flag of the component metadata
     * and just enables as requested.
     * 
     * @param synchronous If <code>true</code> the component is immediately
     *      enabled synchronously. Otherwise the component enabled is scheduled
     *      for asynchronous enabled by calling {@link #enable()}.
     */
    protected final void enable( boolean synchronous )
    {
        if ( synchronous )
        {
            enableInternal();
        }
        else
        {
            enable();
        }
    }


    /**
     * Activates this component if satisfied. If any of the dependencies is
     * not met, the component is not activated and remains unsatisifed.
     * <p>
     * This method schedules the activation for asynchronous execution.
     */
    public final void activate()
    {
        getActivator().schedule( new Runnable()
        {
            public void run()
            {
                activateInternal();
            }
        } );
    }


    /**
     * Reconfigures this component by deactivating and activating it. During
     * activation the new configuration data is retrieved from the Configuration
     * Admin Service.
     */
    public final void reconfigure()
    {
        log( LogService.LOG_DEBUG, "Deactivating and Activating to reconfigure", m_componentMetadata, null );
        reactivateAsynchronous();
    }


    /**
     * Cycles this component by deactivating it and - if still satisfied -
     * activating it again.
     * <p>
     * This method immediately deactivates the component to prevent action
     * with old configuration/references and schedules the reactivation for
     * asynchronous execution.
     */
    public final void reactivate()
    {
        // synchronously deactivate and schedule activation asynchronously
        deactivate();
        
        getActivator().schedule( new Runnable()
        {
            public void run()
            {
                activateInternal();
            }
        } );
    }
    
    
    /**
     * Cycles this component by deactivating it and - if still satisfied -
     * activating it again asynchronously.
     * <p>
     * This method schedules the deactivation and reactivation for asynchronous
     * execution.
     */
    public final void reactivateAsynchronous()
    {
        getActivator().schedule( new Runnable()
        {
            public void run()
            {
                deactivateInternal();
                activateInternal();
            }
        } );
    }


    /**
     * Deactivates the component.
     * <p>
     * This method unlike other state change methods immediately takes
     * action and deactivates the component. The reason for this is, that this
     * method is called when a required service is not available any more and
     * hence the component cannot work. The exception to this is, that the
     * deactivation is scheduled for asynchronous execution if the component
     * is currently activating.
     * <p>
     * We must not immediately deactivate while the component is activating
     * because we might create a deadlock: If this method is called from the
     * framework service event thread some locks may be held. If at the same
     * time the activation tries to access referenced services the framework
     * lock will be tried to be obtained. On the other hand the activation
     * holds a lock on this instance and the deactivation tries to get that
     * lock.
     */
    public final void deactivate()
    {
        if ( getState() == STATE_ACTIVATING )
        {
            log( LogService.LOG_INFO,
                "Asynchronously deactivating the component to prevent a deadlock while it is being activated",
                m_componentMetadata, null );
            getActivator().schedule( new Runnable()
            {
                public void run()
                {
                    deactivateInternal();
                }
            } );
        }
        else
        {
            deactivateInternal();
        }
    }


    /**
     * Disables this component and - if active - first deactivates it. The
     * component may be reenabled by calling the {@link #enable()} method.
     * <p>
     * This method schedules the disablement for asynchronous execution.
     */
    public final void disable()
    {
        getActivator().schedule( new Runnable()
        {
            public void run()
            {
                disableInternal();
            }
        } );
    }


    /**
     * Disposes off this component deactivating and disabling it first as
     * required. After disposing off the component, it may not be used anymore.
     * <p>
     * This method unlike the other state change methods immediately takes
     * action and disposes the component. The reason for this is, that this
     * method has to actually complete before other actions like bundle stopping
     * may continue.
     */
    public void dispose()
    {
        disposeInternal();
    }


    //---------- Component interface ------------------------------------------

    public long getId()
    {
        return m_componentId;
    }


    public String getName()
    {
        return m_componentMetadata.getName();
    }


    public Bundle getBundle()
    {
        return getActivator().getBundleContext().getBundle();
    }


    public String getClassName()
    {
        return m_componentMetadata.getImplementationClassName();
    }


    public String getFactory()
    {
        return m_componentMetadata.getFactoryIdentifier();
    }


    public Reference[] getReferences()
    {
        if ( m_dependencyManagers != null && m_dependencyManagers.size() > 0 )
        {
            return ( org.apache.felix.scr.Reference[] ) m_dependencyManagers
                .toArray( new Reference[m_dependencyManagers.size()] );
        }

        return null;
    }


    public boolean isImmediate()
    {
        return m_componentMetadata.isImmediate();

    }


    public boolean isDefaultEnabled()
    {
        return m_componentMetadata.isEnabled();
    }


    public boolean isServiceFactory()
    {
        return m_componentMetadata.getServiceMetadata() != null
            && m_componentMetadata.getServiceMetadata().isServiceFactory();
    }


    public String[] getServices()
    {
        if ( m_componentMetadata.getServiceMetadata() != null )
        {
            return m_componentMetadata.getServiceMetadata().getProvides();
        }

        return null;
    }


    //---------- internal immediate state change methods ----------------------
    // these methods must only be called from a separate thread by calling
    // the respective asynchronous (public) method

    /**
     * Enable this component
     *
     */
    private void enableInternal()
    {

        if ( getState() == STATE_DESTROYED )
        {
            log( LogService.LOG_ERROR, "Destroyed Component cannot be enabled", m_componentMetadata, null );
            return;
        }
        else if ( getState() != STATE_DISABLED )
        {
            log( LogService.LOG_DEBUG, "Component is already enabled", m_componentMetadata, null );
            return;
        }

        log( LogService.LOG_DEBUG, "Enabling component", m_componentMetadata, null );

        try
        {
            // If this component has got dependencies, create dependency managers for each one of them.
            if ( m_componentMetadata.getDependencies().size() != 0 )
            {
                Iterator dependencyit = m_componentMetadata.getDependencies().iterator();

                while ( dependencyit.hasNext() )
                {
                    ReferenceMetadata currentdependency = ( ReferenceMetadata ) dependencyit.next();

                    DependencyManager depmanager = new DependencyManager( this, currentdependency );

                    m_dependencyManagers.add( depmanager );
                }
            }

            // enter enabled state before trying to activate
            setState( STATE_ENABLED );

            log( LogService.LOG_DEBUG, "Component enabled", m_componentMetadata, null );

            // immediately activate the compopnent, no need to schedule again
            activateInternal();
        }
        catch ( Exception ex )
        {
            log( LogService.LOG_ERROR, "Failed enabling Component", m_componentMetadata, ex );

            // ensure we get back to DISABLED state
            // immediately disable, no need to schedule again
            disableInternal();
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
    private void activateInternal()
    {
        synchronized ( this )
        {
            // CONCURRENCY NOTE: This method is only called from within the
            //     ComponentActorThread to enable, activate or reactivate the
            //     component. Still we use the setStateConditional to not create
            //     a race condition with the deactivateInternal method
            if ( !setStateConditional( STATE_ENABLED | STATE_UNSATISFIED, STATE_ACTIVATING ) )
            {
                return;
            }

            // we cannot activate if the component activator is shutting down
            if ( !isActive() )
            {
                log( LogService.LOG_DEBUG, "Component cannot be activated because the Activator is being disposed",
                    m_componentMetadata, null );
                setState( STATE_UNSATISFIED );
                return;
            }

            log( LogService.LOG_DEBUG, "Activating component", m_componentMetadata, null );

            // Before creating the implementation object, we are going to
            // test if all the mandatory dependencies are satisfied
            Dictionary properties = getProperties();
            Iterator it = m_dependencyManagers.iterator();
            while ( it.hasNext() )
            {
                DependencyManager dm = ( DependencyManager ) it.next();

                // ensure the target filter is correctly set
                dm.setTargetFilter( properties );

                // check whether the service is satisfied
                if ( !dm.isSatisfied() )
                {
                    // at least one dependency is not satisfied
                    log( LogService.LOG_INFO, "Dependency not satisfied: " + dm.getName(), m_componentMetadata, null );
                    setState( STATE_UNSATISFIED );
                }

                // if at least one dependency is missing, we cannot continue and
                // have to return
                if ( getState() == STATE_UNSATISFIED )
                {
                    return;
                }
            }

            // 1. Load the component implementation class
            // 2. Create the component instance and component context
            // 3. Bind the target services
            // 4. Call the activate method, if present
            if ( !createComponent() )
            {
                // component creation failed, not active now
                log( LogService.LOG_ERROR, "Component instance could not be created, activation failed",
                    m_componentMetadata, null );

                // set state to unsatisfied
                setState( STATE_UNSATISFIED );

                return;
            }

            // set the service registration guard before we actually set our
            // state to satisfied. If we would set this after setting the
            // state to satisified, there would be a theoretical window
            // between this state setting and service locking
            lockServiceRegistration();
            
            // Validation occurs before the services are provided, otherwhise the
            // service provider's service may be called by a service requester
            // while it is still ACTIVATING
            setState( getSatisfiedState() );
        }

        // 5. Register provided services
        // call this outside of the synchronization to prevent a possible
        // deadlock if service registration tries to lock the framework or
        // owning bundle during registration (see FELIX-384)
        try
        {
            m_serviceRegistration = registerComponentService();
            log( LogService.LOG_DEBUG, "Component activated", m_componentMetadata, null );
        }
        catch ( IllegalStateException ise )
        {
            // thrown by service registration if the bundle is stopping
            // we just log this at debug level but ignore it
            log( LogService.LOG_DEBUG, "Component activation failed while registering the service",
                m_componentMetadata, ise );
        }
        finally
        {
            // reset the service registration guard
            unlockServiceRegistration();
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
    private synchronized void deactivateInternal()
    {
        // CONCURRENCY NOTE: This method may be called either from the
        //     ComponentActorThread to handle application induced disabling or
        //     as a result of an unsatisfied service dependency leading to
        //     component deactivation. We therefore have to guard against
        //     paralell state changes.
        if ( !setStateConditional( STATE_ACTIVATING | STATE_ACTIVE | STATE_REGISTERED | STATE_FACTORY,
            STATE_DEACTIVATING ) )
        {
            return;
        }

        log( LogService.LOG_DEBUG, "Deactivating component", m_componentMetadata, null );

        // 0.- Remove published services from the registry
        unregisterComponentService();

        // 1.- Call the deactivate method, if present
        // 2. Unbind any bound services
        // 3. Release references to the component instance and component context
        deleteComponent();

        //Activator.trace("InstanceManager from bundle ["+ m_activator.getBundleContext().getBundle().getBundleId() + "] was invalidated.");

        // reset to state UNSATISFIED
        setState( STATE_UNSATISFIED );

        log( LogService.LOG_DEBUG, "Component deactivated", m_componentMetadata, null );
    }


    private void disableInternal()
    {
        // CONCURRENCY NOTE: This method is only called from the BundleComponentActivator or by application logic
        // but not by the dependency managers
        
        // if the component is already disabled or destroyed, we have
        // nothing to do
        if ( ( getState() & ( STATE_DISABLED | STATE_DESTROYED ) ) != 0 )
        {
            log( LogService.LOG_DEBUG, "Component already disabled", m_componentMetadata, null );
            return;
        }

        // deactivate first, this does nothing if not active/registered/factory
        deactivateInternal();

        log( LogService.LOG_DEBUG, "Disabling component", m_componentMetadata, null );

        // close all service listeners now, they are recreated on enable
        // Stop the dependency managers to listen to events...
        Iterator it = m_dependencyManagers.iterator();
        while ( it.hasNext() )
        {
            DependencyManager dm = ( DependencyManager ) it.next();
            dm.close();
        }
        m_dependencyManagers.clear();

        // we are now disabled, ready for re-enablement or complete destroyal
        setState( STATE_DISABLED );

        log( LogService.LOG_DEBUG, "Component disabled", m_componentMetadata, null );
    }


    /**
     *
     */
    private void disposeInternal()
    {
        // CONCURRENCY NOTE: This method is only called from the BundleComponentActivator or by application logic
        // but not by the dependency managers

        // if the component is already destroyed, we have nothing to do
        if ( getState() == STATE_DESTROYED )
        {
            log( LogService.LOG_DEBUG, "Component already destroyed", m_componentMetadata, null );
            return;
        }

        // disable first to clean up correctly
        disableInternal();

        // this component must not be used any more
        setState( STATE_DESTROYED );

        log( LogService.LOG_DEBUG, "Component disposed", m_componentMetadata, null );

        // release references (except component metadata for logging purposes)
        m_activator = null;
        m_dependencyManagers = null;
    }


    //---------- Component handling methods ----------------------------------

    /**
    * Method is called by {@link #activate()} in STATE_ACTIVATING or by
    * {@link DelayedComponentManager#getService(Bundle, ServiceRegistration)}
    * in STATE_REGISTERED.
    *
    * @return <code>true</code> if creation of the component succeeded. If
    *       <code>false</code> is returned, the cause should have been logged.
    */
    protected abstract boolean createComponent();


    /**
     * Method is called by {@link #deactivate()} in STATE_DEACTIVATING
     *
     */
    protected abstract void deleteComponent();


    /**
     * Returns the service object to be registered if the service element is
     * specified.
     * <p>
     * Extensions of this class may overwrite this method to return a
     * ServiceFactory to register in the case of a delayed or a service
     * factory component.
     */
    protected abstract Object getService();


    /**
     * Returns the state value to set, when the component is satisfied. The
     * return value depends on the kind of the component:
     * <dl>
     * <dt>Immediate</dt><dd><code>STATE_ACTIVE</code></dd>
     * <dt>Delayed</dt><dd><code>STATE_REGISTERED</code></dd>
     * <dt>Component Factory</dt><dd><code>STATE_FACTORY</code></dd>
     * </dl>
     *
     * @return
     */
    private int getSatisfiedState()
    {
        if ( m_componentMetadata.isFactory() )
        {
            return STATE_FACTORY;
        }
        else if ( m_componentMetadata.isImmediate() )
        {
            return STATE_ACTIVE;
        }
        else
        {
            return STATE_REGISTERED;
        }
    }


    // 5. Register provided services
    protected ServiceRegistration registerComponentService()
    {
        if ( getComponentMetadata().getServiceMetadata() != null )
        {
            log( LogService.LOG_DEBUG, "registering services", getComponentMetadata(), null );

            // get a copy of the component properties as service properties
            Dictionary serviceProperties = copyTo( null, getProperties() );

            return getActivator().getBundleContext().registerService(
                getComponentMetadata().getServiceMetadata().getProvides(), getService(), serviceProperties );
        }

        return null;
    }


    protected void unregisterComponentService()
    {
        // outside of try-finally to not trigger inadvertend unlock
        lockServiceRegistration();

        try
        {
            if ( m_serviceRegistration != null )
            {
                m_serviceRegistration.unregister();
                m_serviceRegistration = null;

                log( LogService.LOG_DEBUG, "unregistering the services", getComponentMetadata(), null );
            }
        }
        finally
        {
            unlockServiceRegistration();
        }
    }

    //----------- Service Registration Locking --------------------------------
    // Implementation note for service registration locking
    // ----------------------------------------------------
    // The activateInternal method sets the component state to satisfied (aka
    // ready) before the component is registered as a service and the internal
    // service registration field is set.
    // If now in the time between setting the state to satisified and the
    // service registration field being set, the component is deactivated --
    // possibly through reconfiguration -- the service may not be unregistered
    // because the field is not set, but re-activation of the component may
    // register the service again thus resulting in two services being
    // registered, the active and the deactivated. Which of both is being used
    // is framework implementation dependent but chances are, the wrong is
    // used resulting in system failure.
    // To fix this, all access to the service registration field is guarded by
    // a lock. Only if a thread is able to set the lock flag, can the service
    // registration field be accessed.
    // To circumvent the above mentioned situation, the activateInternal method
    // locks the field _before_ setting the component satisfied. This prevents
    // the deactivateInternal method from deactivating the component until the
    // service has been registered and the lock been freed. Only then can the
    // deactivateInternal method start its deactivation task by unregistering
    // the service.
    // See FELIX-550 for more information.
    

    // locks service registration by waiting for the registration to not
    // be locked and then locking it
    private void lockServiceRegistration()
    {
        synchronized ( serviceRegistrationLock )
        {
            if ( serviceRegistrationLockOwner != null )
            {
                log( LogService.LOG_INFO, "Waiting for Service Registration owned by " + serviceRegistrationLockOwner,
                    m_componentMetadata, null );

                int waitGuard = 10;
                while ( serviceRegistrationLockOwner != null && waitGuard > 0 )
                {
                    // wait at most one second
                    try
                    {
                        serviceRegistrationLock.wait( 1000L );
                    }
                    catch ( InterruptedException ie )
                    {
                        // don't care
                    }
                    waitGuard--;
                }

                // timedout waiting for the service registration lock
                if ( waitGuard <= 0 )
                {
                    throw new IllegalStateException( "Cannot get Service Registration, owned by "
                        + serviceRegistrationLockOwner );
                }

                log( LogService.LOG_INFO, "Service Registration is now ready", m_componentMetadata, null );
            }

            serviceRegistrationLockOwner = Thread.currentThread();
        }
    }


    // unlocks the service registration. This should only be called by the
    // thread actually holding the lock.
    private void unlockServiceRegistration()
    {
        synchronized ( serviceRegistrationLock )
        {
            Thread current = Thread.currentThread();
            if ( serviceRegistrationLockOwner == current )
            {
                serviceRegistrationLockOwner = null;

                // notify threads waiting to lock service registration
                serviceRegistrationLock.notifyAll();
            }
            else
            {
                log( LogService.LOG_DEBUG, "Not releasing Service Registration by " + current + ", owner is "
                    + serviceRegistrationLockOwner, m_componentMetadata, null );
            }
        }
    }

    //**********************************************************************************************************

    BundleComponentActivator getActivator()
    {
        return m_activator;
    }


    void log( int level, String message, ComponentMetadata metadata, Throwable ex )
    {
        BundleComponentActivator activator = getActivator();
        if ( activator != null )
        {
            activator.log( level, message, metadata, ex );
        }
    }


    /**
     * Returns <code>true</code> if this instance has not been disposed off
     * yet and the BundleComponentActivator is still active. If the Bundle
     * Component Activator is being disposed off as a result of stopping the
     * owning bundle, this method returns <code>false</code>.
     */
    private boolean isActive()
    {
        BundleComponentActivator bca = getActivator();
        return bca != null && bca.isActive();
    }


    Iterator getDependencyManagers()
    {
        return m_dependencyManagers.iterator();
    }


    DependencyManager getDependencyManager( String name )
    {
        Iterator it = getDependencyManagers();
        while ( it.hasNext() )
        {
            DependencyManager dm = ( DependencyManager ) it.next();
            if ( name.equals( dm.getName() ) )
            {
                return dm;
            }
        }

        // not found
        return null;
    }


    /**
    * Get the object that is implementing this descriptor
    *
    * @return the object that implements the services
    */
    public abstract Object getInstance();


    public abstract Dictionary getProperties();


    /**
     * Copies the properties from the <code>source</code> <code>Dictionary</code>
     * into the <code>target</code> <code>Dictionary</code>.
     *
     * @param target The <code>Dictionary</code> into which to copy the
     *      properties. If <code>null</code> a new <code>Hashtable</code> is
     *      created.
     * @param source The <code>Dictionary</code> providing the properties to
     *      copy. If <code>null</code> or empty, nothing is copied.
     *
     * @return The <code>target</code> is returned, which may be empty if
     *      <code>source</code> is <code>null</code> or empty and
     *      <code>target</code> was <code>null</code>.
     */
    protected Dictionary copyTo( Dictionary target, Dictionary source )
    {
        if ( target == null )
        {
            target = new Hashtable();
        }

        if ( source != null && !source.isEmpty() )
        {
            for ( Enumeration ce = source.keys(); ce.hasMoreElements(); )
            {
                Object key = ce.nextElement();
                target.put( key, source.get( key ) );
            }
        }

        return target;
    }


    ServiceReference getServiceReference()
    {
        // outside of try-finally to not trigger inadvertend unlock
        lockServiceRegistration();

        try
        {
            return ( m_serviceRegistration != null ) ? m_serviceRegistration.getReference() : null;
        }
        finally
        {
            unlockServiceRegistration();
        }
    }


    /**
     *
     */
    public ComponentMetadata getComponentMetadata()
    {
        return m_componentMetadata;
    }


    public int getState()
    {
        return m_state;
    }


    /**
     * sets the state of the manager
    **/
    protected synchronized void setState( int newState )
    {
        log( LogService.LOG_DEBUG,
            "State transition : " + stateToString( m_state ) + " -> " + stateToString( newState ), m_componentMetadata,
            null );

        m_state = newState;
    }


    /**
     * If the state is currently one of the <code>requiredStates</code>, the
     * state is set to <code>newState</code> and <code>true</code> is returned.
     * Otherwise the state is not changed and <code>false</code> is returned.
     * <p>
     * This method atomically checks the current state and sets the new state.
     *
     * @param requiredStates The set of states required for the state change to
     *          happen.
     * @param newState The new state to go into.
     * @return <code>true</code> if the state was one of the required states and
     *          the new state has now been entered.
     */
    protected synchronized boolean setStateConditional( int requiredStates, int newState )
    {
        if ( ( getState() & requiredStates ) != 0 )
        {
            setState( newState );
            return true;
        }

        return false;
    }


    public String stateToString( int state )
    {
        switch ( state )
        {
            case STATE_DESTROYED:
                return "Destroyed";
            case STATE_DISABLED:
                return "Disabled";
            case STATE_ENABLED:
                return "Enabled";
            case STATE_UNSATISFIED:
                return "Unsatisfied";
            case STATE_ACTIVATING:
                return "Activating";
            case STATE_ACTIVE:
                return "Active";
            case STATE_REGISTERED:
                return "Registered";
            case STATE_FACTORY:
                return "Factory";
            case STATE_DEACTIVATING:
                return "Deactivating";
            default:
                return String.valueOf( state );
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
     * @param only Whether to only look at the declared methods of the given
     *      class or also inspect the super classes.
     *
     * @return The named method with enforced accessibility
     *
     * @throws NoSuchMethodException If no public or protected method with
     *      the given name can be found in the class or any of its super classes.
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to access the desired method.
     */
    static Method getMethod( Class clazz, String name, Class[] parameterTypes, boolean only )
        throws NoSuchMethodException, InvocationTargetException
    {
        for ( ; clazz != null; clazz = clazz.getSuperclass() )
        {
            try
            {
                // find the declared method in this class
                Method method = clazz.getDeclaredMethod( name, parameterTypes );

                // accept public and protected methods only and ensure accessibility
                if ( Modifier.isPublic( method.getModifiers() ) || Modifier.isProtected( method.getModifiers() ) )
                {
                    method.setAccessible( true );
                    return method;
                }

                // if only the clazz is to be scanned terminate here
                if ( only )
                {
                    break;
                }
            }
            catch ( NoSuchMethodException nsme )
            {
                // ignore for now
            }
            catch ( Throwable throwable )
            {
                // unexpected problem accessing the method, don't let everything
                // blow up in this situation, just throw a declared exception
                throw new InvocationTargetException( throwable, "Unexpected problem trying to get method " + name );
            }
        }

        // walked up the complete super class hierarchy and still not found
        // anything, sigh ...
        throw new NoSuchMethodException( name );
    }

}
