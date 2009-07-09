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
package org.apache.felix.scr.impl.manager;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.ComponentActivatorTask;
import org.apache.felix.scr.impl.ComponentRegistry;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.log.LogService;

/**
 * The default ComponentManager. Objects of this class are responsible for managing
 * implementation object's lifecycle.
 *
 */
public abstract class AbstractComponentManager implements Component, ComponentInstance
{
    // the ID of this component
    private long m_componentId;

    // The state of this instance manager
    // methods accessing this field should be synchronized unless there is a
    // good reason to not be synchronized
    private volatile State m_state;

    // The metadata
    private ComponentMetadata m_componentMetadata;

    // The dependency managers that manage every dependency
    private List m_dependencyManagers;

    // A reference to the BundleComponentActivator
    private BundleComponentActivator m_activator;

    // The ServiceRegistration
    private volatile ServiceRegistration m_serviceRegistration;

    /**
     * There are 9 states in all. They are: Disabled, Enabled, Unsatisfied,
     * Registered, Factory, Active, Destroyed, Activating and Deactivating.
     * State Enabled is right State Unsatisfied. State Registered, Factory
     * and Active are the "Satisfied" state in concept. State Activating and
     * Deactivating are transition states. They will be changed to other state
     * automatically when work is done.
     * <p>
     * The transition cases are listed below.
     * <ul>
     * <li>Disabled -(enable)-> Enabled</li>
     * <li>Disabled -(dispose)-> Destoryed</li>
     * <li>Enabled -(activate, SUCCESS)-> Satisfied(Registered, Factory or Active)</li>
     * <li>Enabled -(activate, FAIL)-> Unsatisfied</li>
     * <li>Enabled -(disable)-> Disabled</li>
     * <li>Enabled -(dispose)-> Destroyed</li>
     * <li>Unsatisfied -(activate, SUCCESS)-> Satisfied(Registered, Factory or Active)</li>
     * <li>Unsatisfied -(activate, FAIL)-> Unsatisfied</li>
     * <li>Unsatisfied -(disable)-> Disabled</li>
     * <li>Unsatisfied -(dispose)-> Destroyed</li>
     * <li>Registered -(getService, SUCCESS)-> Active</li>
     * <li>Registered -(getService, FAIL)-> Unsatisfied</li>
     * <li>Satisfied -(deactivate)-> Unsatisfied</li>
     * </ul>
     */
    protected static abstract class State
    {
        private final String m_name;
        private final int m_state;

        protected State( String name, int state )
        {
            m_name = name;
            m_state = state;
        }

        public String toString()
        {
            return m_name;
        }

        int getState()
        {
            return m_state;
        }

        ServiceReference getServiceReference( AbstractComponentManager acm )
        {
            return null;
        }

        void enableInternal( AbstractComponentManager acm )
        {
            acm.log( LogService.LOG_DEBUG,
                    "Current state: " + m_name + ", Event: enable",
                    acm.getComponentMetadata(), null );
        }

        void disableInternal( AbstractComponentManager acm )
        {
            acm.log( LogService.LOG_DEBUG,
                    "Current state: " + m_name + ", Event: disable",
                    acm.getComponentMetadata(), null );
        }

        void activateInternal( AbstractComponentManager acm )
        {
            acm.log(LogService.LOG_DEBUG,
                    "Current state: " + m_name + ", Event: activate",
                    acm.getComponentMetadata(), null);
        }

        void deactivateInternal( AbstractComponentManager acm, int reason )
        {
            acm.log( LogService.LOG_DEBUG, "Current state: " + m_name + ", Event: deactivate (reason: " + reason + ")",
                acm.getComponentMetadata(), null );
        }

        void disposeInternal( AbstractComponentManager acm )
        {
            acm.log( LogService.LOG_DEBUG,
                    "Current state: " + m_name + ", Event: dispose",
                    acm.getComponentMetadata(), null );
        }

        Object getService( DelayedComponentManager dcm )
        {
            dcm.log( LogService.LOG_DEBUG,
                    "Current state: " + m_name + ", Event: getService",
                    dcm.getComponentMetadata(), null );
            return null;
        }
    }

    protected static final class Destroyed extends State
    {
        private static final Destroyed m_inst = new Destroyed();

        private Destroyed()
        {
            super( "Destroyed", STATE_DESTROYED );
        }

        static State getInstance()
        {
            return m_inst;
        }
    }

    protected static final class Disabled extends State
    {
        private static final Disabled m_inst = new Disabled();

        private Disabled()
        {
            super( "Disabled", STATE_DISABLED );
        }

        static State getInstance()
        {
            return m_inst;
        }

        void enableInternal( AbstractComponentManager acm )
        {
            acm.changeState( Enabled.getInstance() );

            acm.log( LogService.LOG_DEBUG, "Component enabled", acm.getComponentMetadata(), null );
        }

        void disposeInternal( AbstractComponentManager acm )
        {
            acm.clear();
            acm.changeState( Destroyed.getInstance() );
        }
    }

    protected static final class Enabled extends State
    {
        private static final Enabled m_inst = new Enabled();

        private Enabled()
        {
            super( "Enabled", STATE_ENABLED );
        }

        static State getInstance()
        {
            return m_inst;
        }

        void activateInternal( AbstractComponentManager acm )
        {
            ComponentMetadata componentMetadata = acm.getComponentMetadata();

            try
            {
                acm.enableDependencyManagers();
                Unsatisfied.getInstance().activateInternal(acm);
            }
            catch (Exception e)
            {
                acm.log( LogService.LOG_ERROR, "Failed enabling Component", componentMetadata, e );
                acm.disposeDependencyManagers();
                acm.loadDependencyManagers( acm.getComponentMetadata() );
            }
        }

        void disableInternal( AbstractComponentManager acm )
        {
            acm.changeState( Disabled.getInstance() );

            acm.log( LogService.LOG_DEBUG, "Component disabled", acm.getComponentMetadata(), null );
        }

        void disposeInternal( AbstractComponentManager acm )
        {
            acm.clear();
            acm.changeState( Destroyed.getInstance() );

            acm.log( LogService.LOG_DEBUG, "Component disposed", acm.getComponentMetadata(), null );
        }
    }

    protected static final class Unsatisfied extends State
    {
        private static final Unsatisfied m_inst = new Unsatisfied();

        private Unsatisfied()
        {
            super( "Unsatisfied", STATE_UNSATISFIED );
        }

        static State getInstance()
        {
            return m_inst;
        }

        void activateInternal( AbstractComponentManager acm )
        {
            acm.changeState( Activating.getInstance() );

            ComponentMetadata componentMetadata = acm.getComponentMetadata();
            acm.log( LogService.LOG_DEBUG, "Activating component", componentMetadata, null );

            // Before creating the implementation object, we are going to
            // test if all the mandatory dependencies are satisfied
            if ( !acm.verifyDependencyManagers( acm.getProperties()) )
            {
                acm.log( LogService.LOG_INFO, "Not all dependencies satisified, cannot activate", componentMetadata, null );
                acm.changeState( Unsatisfied.getInstance() );
                return;
            }

            // 1. Load the component implementation class
            // 2. Create the component instance and component context
            // 3. Bind the target services
            // 4. Call the activate method, if present
            if ( !acm.createComponent() )
            {
                // component creation failed, not active now
                acm.log( LogService.LOG_ERROR, "Component instance could not be created, activation failed",
                        componentMetadata, null );

                // set state to unsatisfied
                acm.changeState( Unsatisfied.getInstance() );
                return;
            }

            acm.changeState( acm.getSatisfiedState() );

            acm.registerComponentService();
        }

        void disableInternal( AbstractComponentManager acm )
        {
            ComponentMetadata componentMetadata = acm.getComponentMetadata();
            acm.log( LogService.LOG_DEBUG, "Disabling component", componentMetadata, null );

            // dispose and recreate dependency managers
            acm.disposeDependencyManagers();
            acm.loadDependencyManagers( componentMetadata );

            // we are now disabled, ready for re-enablement or complete destroyal
            acm.changeState( Disabled.getInstance() );

            acm.log( LogService.LOG_DEBUG, "Component disabled", componentMetadata, null );
        }

        void disposeInternal( AbstractComponentManager acm )
        {
            acm.disposeDependencyManagers();
            acm.clear();
            acm.changeState( Destroyed.getInstance() );

            acm.log( LogService.LOG_DEBUG, "Component disposed", acm.getComponentMetadata(), null );
        }
    }

    protected static final class Activating extends State
    {
        private static final Activating m_inst = new Activating();

        private Activating() {
            super( "Activating", STATE_ACTIVATING );
        }

        static State getInstance()
        {
            return m_inst;
        }
    }

    protected static final class Deactivating extends State
    {
        private static final Deactivating m_inst = new Deactivating();

        private Deactivating() {
            super( "Deactivating", STATE_DEACTIVATING );
        }

        static State getInstance() {
            return m_inst;
        }
    }

    protected static abstract class Satisfied extends State
    {
        protected Satisfied( String name, int state )
        {
            super( name, state );
        }

        ServiceReference getServiceReference( AbstractComponentManager acm )
        {
            ServiceRegistration sr = acm.getServiceRegistration();
            return sr == null ? null : sr.getReference();
        }

        void deactivateInternal( AbstractComponentManager acm, int reason )
        {
            acm.changeState(Deactivating.getInstance());

            ComponentMetadata componentMetadata = acm.getComponentMetadata();
            acm.log( LogService.LOG_DEBUG, "Deactivating component", componentMetadata, null );

            // catch any problems from deleting the component to prevent the
            // component to remain in the deactivating state !
            try
            {
                acm.unregisterComponentService();
                acm.deleteComponent( reason );
            }
            catch ( Throwable t )
            {
                acm.log( LogService.LOG_WARNING, "Component deactivation threw an exception", componentMetadata, t );
            }

            acm.changeState( Unsatisfied.getInstance() );
            acm.log( LogService.LOG_DEBUG, "Component deactivated", componentMetadata, null );
        }
    }

    protected static final class Registered extends Satisfied
    {
        private static final Registered m_inst = new Registered();

        private Registered() {
            super( "Registered", STATE_REGISTERED );
        }

        static State getInstance()
        {
            return m_inst;
        }

        Object getService(DelayedComponentManager dcm)
        {
            if ( dcm.createRealComponent() )
            {
                dcm.changeState( Active.getInstance() );
                return dcm.getInstance();
            }
            else
            {
                deactivateInternal( dcm, ComponentConstants.DEACTIVATION_REASON_UNSPECIFIED );
                return null;
            }
        }
    }

    protected static final class Factory extends Satisfied
    {
        private static final Factory m_inst = new Factory();

        private Factory()
        {
            super( "Factory", STATE_FACTORY );
        }

        static State getInstance()
        {
            return m_inst;
        }
    }

    protected static final class Active extends Satisfied
    {
        private static final Active m_inst = new Active();

        private Active()
        {
            super( "Active", STATE_ACTIVE );
        }

        static State getInstance()
        {
            return m_inst;
        }

        Object getService( DelayedComponentManager dcm )
        {
            return dcm.getInstance();
        }
    }

    /**
     * The constructor receives both the activator and the metadata
     *
     * @param activator
     * @param metadata
     * @param componentId
     */
    protected AbstractComponentManager( BundleComponentActivator activator, ComponentMetadata metadata,
        ComponentRegistry componentRegistry )
    {
        m_activator = activator;
        m_componentMetadata = metadata;
        m_componentId = componentRegistry.createComponentId();

        m_state = Disabled.getInstance();
        loadDependencyManagers( metadata );

        log( LogService.LOG_DEBUG, "Component created", metadata, null );
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
        enableInternal();

        getActivator().schedule( new ComponentActivatorTask( "Enable", this )
        {
            public void doRun()
            {
                activateInternal();
            }
        });
    }

    /**
     * Disables this component and - if active - first deactivates it. The
     * component may be reenabled by calling the {@link #enable()} method.
     * <p>
     * This method schedules the disablement for asynchronous execution.
     */
    public final void disable()
    {
        getActivator().schedule( new ComponentActivatorTask( "Disable", this )
        {
            public void doRun()
            {
                deactivateInternal( ComponentConstants.DEACTIVATION_REASON_DISABLED );
                disableInternal();
            }
        });
    }

    // implements the ComponentInstance.dispose() method
    public void dispose()
    {
        dispose( ComponentConstants.DEACTIVATION_REASON_DISPOSED );
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
    public void dispose( int reason )
    {
        disposeInternal( reason );
    }

    //---------- Component interface ------------------------------------------
    public long getId()
    {
        return m_componentId;
    }

    public String getName() {
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
            return (Reference[]) m_dependencyManagers.toArray(
                    new Reference[m_dependencyManagers.size()] );
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

    //-------------- atomic transition methods -------------------------------
    /**
     * Disposes off this component deactivating and disabling it first as
     * required. After disposing off the component, it may not be used anymore.
     * <p>
     * This method unlike the other state change methods immediately takes
     * action and disposes the component. The reason for this is, that this
     * method has to actually complete before other actions like bundle stopping
     * may continue.
     */
    synchronized final void enableInternal()
    {
        m_state.enableInternal( this );
    }

    synchronized final void disableInternal()
    {
        m_state.disableInternal( this );
    }

    synchronized final void activateInternal()
    {
        m_state.activateInternal( this );
    }

    synchronized final void deactivateInternal( int reason )
    {
        m_state.deactivateInternal( this, reason );
    }

    synchronized final void disposeInternal( int reason )
    {
        m_state.deactivateInternal( this, reason );
        // For the sake of the performance(no need to loadDependencyManagers again),
        // the disable transition is integrated into the destroy transition.
        // That is to say, state "Enabled" goes directly into state "Desctroyed"
        // in the event "dipose".
        m_state.disposeInternal( this );
    }


    final ServiceReference getServiceReference()
    {
        // This method is not synchronized even though it accesses the state.
        // The reason for this is that we just want to have the state return
        // the service reference which comes from the service registration.
        // The only thing that may happen is that the service registration is
        // still set on this instance but the service has already been
        // unregistered. In this case an IllegalStateException may be thrown
        // which we just catch and ignore returning null
        State state = m_state;
        try
        {
            return state.getServiceReference( this );
        }
        catch ( IllegalStateException ise )
        {
            // may be thrown if the service has already been unregistered but
            // the service registration is still set on this component manager
            // we ignore this exception and assume there is no service reference
        }

        return null;
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

    protected abstract void deleteComponent( int reason );

    /**
     * Returns the service object to be registered if the service element is
     * specified.
     * <p>
     * Extensions of this class may overwrite this method to return a
     * ServiceFactory to register in the case of a delayed or a service
     * factory component.
     *
     * @return
     */
    protected abstract Object getService();


    final State getSatisfiedState()
    {
        if ( m_componentMetadata.isFactory() )
        {
            return Factory.getInstance();
        }
        else if ( m_componentMetadata.isImmediate() )
        {
            return Active.getInstance();
        }
        else
        {
            return Registered.getInstance();
        }
    }


    protected ServiceRegistration registerService()
    {
        if ( getComponentMetadata().getServiceMetadata() != null )
        {
            log( LogService.LOG_DEBUG, "registering services", m_componentMetadata, null );

            // get a copy of the component properties as service properties
            Dictionary serviceProperties = copyTo( null, getProperties() );

            return getActivator().getBundleContext().registerService(
                    getComponentMetadata().getServiceMetadata().getProvides(),
                    getService(), serviceProperties );
        }

        return null;
    }

    // 5. Register provided services
    protected void registerComponentService()
    {
        m_serviceRegistration = registerService();
    }

    protected final void unregisterComponentService()
    {

        if ( m_serviceRegistration != null )
        {
            log( LogService.LOG_DEBUG, "unregistering the services", m_componentMetadata, null );

            m_serviceRegistration.unregister();
            m_serviceRegistration = null;
        }
    }

    //**********************************************************************************************************
    public BundleComponentActivator getActivator()
    {
        return m_activator;
    }

    final ServiceRegistration getServiceRegistration()
    {
        return m_serviceRegistration;
    }

    void clear()
    {
        m_activator = null;
        m_dependencyManagers.clear();
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
     * Activates this component if satisfied. If any of the dependencies is
     * not met, the component is not activated and remains unsatisifed.
     * <p>
     * This method schedules the activation for asynchronous execution.
     */
    public final void activate()
    {
        getActivator().schedule( new ComponentActivatorTask( "Activate", this ) {
            public void doRun()
            {
                activateInternal();
            }
        });
    }

    /**
     * Reconfigures this component by deactivating and activating it. During
     * activation the new configuration data is retrieved from the Configuration
     * Admin Service.
     */
    public final void reconfigure( final int reason )
    {
        log( LogService.LOG_DEBUG, "Deactivating and Activating to reconfigure", m_componentMetadata, null );
        reactivate( reason );
    }

    /**
     * Cycles this component by deactivating it and - if still satisfied -
     * activating it again asynchronously.
     * <p>
     * This method schedules the deactivation and reactivation for asynchronous
     * execution.
     */
    public final void reactivate( final int reason )
    {
        getActivator().schedule( new ComponentActivatorTask( "Reactivate", this )
        {

            public void doRun()
            {
                deactivateInternal( reason );
                activateInternal();
            }
        } );
    }

    public String toString() {
        return "Component: " + getName() + " (" + getId() + ")";
    }

    private void loadDependencyManagers( ComponentMetadata metadata )
    {
        List depMgrList = new ArrayList();

        // If this component has got dependencies, create dependency managers for each one of them.
        if ( metadata.getDependencies().size() != 0 )
        {
            Iterator dependencyit = metadata.getDependencies().iterator();

            while ( dependencyit.hasNext() )
            {
                ReferenceMetadata currentdependency = (ReferenceMetadata) dependencyit.next();

                DependencyManager depmanager = new DependencyManager( this, currentdependency );

                depMgrList.add( depmanager );
            }
        }

        m_dependencyManagers = depMgrList;
    }

    private void enableDependencyManagers() throws InvalidSyntaxException
    {
        Iterator it = getDependencyManagers();
        while ( it.hasNext() )
        {
            DependencyManager dm = (DependencyManager) it.next();
            dm.enable();
        }
    }

    private boolean verifyDependencyManagers( Dictionary properties )
    {
        // indicates whether all dependencies are satisfied
        boolean satisfied = true;

        Iterator it = getDependencyManagers();
        while ( it.hasNext() )
        {
            DependencyManager dm = (DependencyManager) it.next();

            // ensure the target filter is correctly set
            dm.setTargetFilter( properties );

            // check whether the service is satisfied
            if ( !dm.isSatisfied() )
            {
                // at least one dependency is not satisfied
                log( LogService.LOG_INFO, "Dependency not satisfied: " + dm.getName(), m_componentMetadata, null );
                satisfied = false;
            }
        }

        return satisfied;
    }

    Iterator getDependencyManagers()
    {
        return m_dependencyManagers.iterator();
    }

    DependencyManager getDependencyManager(String name)
    {
        Iterator it = getDependencyManagers();
        while ( it.hasNext() )
        {
            DependencyManager dm = (DependencyManager) it.next();
            if ( name.equals(dm.getName()) )
            {
                return dm;
            }
        }

        // not found
        return null;
    }

    private void disposeDependencyManagers()
    {
        Iterator it = getDependencyManagers();
        while ( it.hasNext() )
        {
            DependencyManager dm = (DependencyManager) it.next();
            dm.dispose();
        }
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

    /**
     *
     */
    public ComponentMetadata getComponentMetadata()
    {
        return m_componentMetadata;
    }

    public int getState()
    {
        return m_state.getState();
    }

    protected State state()
    {
        return m_state;
    }

    /**
     * sets the state of the manager
     **/
    void changeState( State newState )
    {
        log( LogService.LOG_DEBUG,
                "State transition : " + m_state + " -> " + newState,
                m_componentMetadata, null );

        m_state = newState;
    }
}
