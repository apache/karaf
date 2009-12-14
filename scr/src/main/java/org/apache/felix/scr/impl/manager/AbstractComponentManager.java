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

import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.ComponentActivatorTask;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.apache.felix.scr.impl.metadata.ServiceMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.log.LogService;

/**
 * The default ComponentManager. Objects of this class are responsible for managing
 * implementation object's lifecycle.
 *
 */
public abstract class AbstractComponentManager implements Component
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
     * The constructor receives both the activator and the metadata
     *
     * @param activator
     * @param metadata
     */
    protected AbstractComponentManager( BundleComponentActivator activator, ComponentMetadata metadata )
    {
        m_activator = activator;
        m_componentMetadata = metadata;

        // for some testing, the activator may be null
        m_componentId = ( activator != null ) ? activator.registerComponentId( this ) : -1;

        m_state = Disabled.getInstance();
        loadDependencyManagers( metadata );

        // dump component details
        if ( isLogEnabled( LogService.LOG_DEBUG ) )
        {
            log(
                LogService.LOG_DEBUG,
                "Component {0} created: DS={1}, implementation={2}, immediate={3}, default-enabled={4}, factory={5}, configuration-policy={6}, activate={7}, deactivate={8}, modified={9}",
                new Object[]
                    { metadata.getName(), new Integer( metadata.getNamespaceCode() ),
                        metadata.getImplementationClassName(), Boolean.valueOf( metadata.isImmediate() ),
                        Boolean.valueOf( metadata.isEnabled() ), metadata.getFactoryIdentifier(),
                        metadata.getConfigurationPolicy(), metadata.getActivate(), metadata.getDeactivate(),
                        metadata.getModified() }, null );

            if ( metadata.getServiceMetadata() != null )
            {
                log( LogService.LOG_DEBUG, "Component {0} Services: servicefactory={1}, services={2}", new Object[]
                    { metadata.getName(), Boolean.valueOf( metadata.getServiceMetadata().isServiceFactory() ),
                        Arrays.asList( metadata.getServiceMetadata().getProvides() ) }, null );
            }

            if ( metadata.getProperties() != null )
            {
                log( LogService.LOG_DEBUG, "Component {0} Properties: {1}", new Object[]
                    { metadata.getName(), metadata.getProperties() }, null );
            }
        }
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

    /**
     * Get the object that is implementing this descriptor
     *
     * @return the object that implements the services
     */
    abstract Object getInstance();

    // supports the ComponentInstance.dispose() method
    void dispose()
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


    public String getActivate()
    {
        return m_componentMetadata.getActivate();
    }


    public boolean isActivateDeclared()
    {
        return m_componentMetadata.isActivateDeclared();
    }


    public String getDeactivate()
    {
        return m_componentMetadata.getDeactivate();
    }


    public boolean isDeactivateDeclared()
    {
        return m_componentMetadata.isDeactivateDeclared();
    }


    public String getModified()
    {
        return m_componentMetadata.getModified();
    }


    public String getConfigurationPolicy()
    {
        return m_componentMetadata.getConfigurationPolicy();
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

    final void enableInternal()
    {
        m_state.enable( this );
    }

    final void activateInternal()
    {
        m_state.activate( this );
    }

    final void deactivateInternal( int reason )
    {
        m_state.deactivate( this, reason );
    }

    final void disableInternal()
    {
        m_state.disable( this );
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
    final void disposeInternal( int reason )
    {
        m_state.deactivate( this, reason );
        m_state.disable( this );
        m_state.dispose( this );
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
            if ( getInstance() != null )
            {
                return FactoryInstance.getInstance();
            }
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
            log( LogService.LOG_DEBUG, "registering services", null );

            // get a copy of the component properties as service properties
            final Dictionary serviceProperties = getServiceProperties();

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
            log( LogService.LOG_DEBUG, "Unregistering the services", null );

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
        // for some testing, the activator may be null
        if ( m_activator != null )
        {
            m_activator.unregisterComponentId( this );
            m_activator = null;
        }

        m_dependencyManagers.clear();
    }


    /**
     * Returns <code>true</code> if logging for the given level is enabled.
     */
    public boolean isLogEnabled( int level )
    {
        BundleComponentActivator activator = getActivator();
        if ( activator != null )
        {
            return activator.isLogEnabled( level );
        }

        // bundle activator has already been removed, so no logging
        return false;
    }


    public void log( int level, String message, Throwable ex )
    {
        BundleComponentActivator activator = getActivator();
        if ( activator != null )
        {
            activator.log( level, message, getComponentMetadata(), ex );
        }
    }

    public void log( int level, String message, Object[] arguments, Throwable ex )
    {
        BundleComponentActivator activator = getActivator();
        if ( activator != null )
        {
            activator.log( level, message, arguments, getComponentMetadata(), ex );
        }
    }

    /**
     * Activates this component if satisfied. If any of the dependencies is
     * not met, the component is not activated and remains unsatisfied.
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


    public String toString()
    {
        return "Component: " + getName() + " (" + getId() + ")";
    }


    private boolean hasServiceRegistrationPermissions()
    {
        boolean allowed = true;
        if ( System.getSecurityManager() != null )
        {
            final ServiceMetadata serviceMetadata = getComponentMetadata().getServiceMetadata();
            if ( serviceMetadata != null )
            {
                final String[] services = serviceMetadata.getProvides();
                if ( services != null && services.length > 0 )
                {
                    final Bundle bundle = getBundle();
                    for ( int i = 0; i < services.length; i++ )
                    {
                        final Permission perm = new ServicePermission( services[i], ServicePermission.REGISTER );
                        if ( !bundle.hasPermission( perm ) )
                        {
                            log( LogService.LOG_INFO, "Permission to register service {0} is denied", new Object[]
                                { services[i] }, null );
                            allowed = false;
                        }
                    }
                }
            }
        }

        // no security manager or no services to register
        return allowed;
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

    protected boolean verifyDependencyManagers( Dictionary properties )
    {
        // indicates whether all dependencies are satisfied
        boolean satisfied = true;

        Iterator it = getDependencyManagers();
        while ( it.hasNext() )
        {
            DependencyManager dm = ( DependencyManager ) it.next();

            // ensure the target filter is correctly set
            dm.setTargetFilter( properties );

            if ( !dm.hasGetPermission() )
            {
                // bundle has no service get permission
                log( LogService.LOG_INFO, "No permission to get dependency: {0}", new Object[]
                    { dm.getName() }, null );
                satisfied = false;
            }
            else if ( !dm.isSatisfied() )
            {
                // bundle would have permission but there are not enough services
                log( LogService.LOG_INFO, "Dependency not satisfied: {0}", new Object[]
                    { dm.getName() }, null );
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

    private void disableDependencyManagers()
    {
        Iterator it = getDependencyManagers();
        while ( it.hasNext() )
        {
            DependencyManager dm = (DependencyManager) it.next();
            dm.disable();
        }
    }

    public abstract boolean hasConfiguration();

    public abstract Dictionary getProperties();

    /**
     * Returns the subset of component properties to be used as service
     * properties. These properties are all component properties where property
     * name does not start with dot (.), properties which are considered
     * private.
     */
    public Dictionary getServiceProperties()
    {
        return copyTo( null, getProperties(), false);
    }

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
    protected static Dictionary copyTo( Dictionary target, Dictionary source )
    {
        return copyTo( target, source, true );
    }

    /**
     * Copies the properties from the <code>source</code> <code>Dictionary</code>
     * into the <code>target</code> <code>Dictionary</code> except for private
     * properties (whose name has a leading dot) which are only copied if the
     * <code>allProps</code> parameter is <code>true</code>.
     *
     * @param target    The <code>Dictionary</code> into which to copy the
     *                  properties. If <code>null</code> a new <code>Hashtable</code> is
     *                  created.
     * @param source    The <code>Dictionary</code> providing the properties to
     *                  copy. If <code>null</code> or empty, nothing is copied.
     * @param allProps  Whether all properties (<code>true</code>) or only the
     *                  public properties (<code>false</code>) are to be copied.
     *
     * @return The <code>target</code> is returned, which may be empty if
     *         <code>source</code> is <code>null</code> or empty and
     *         <code>target</code> was <code>null</code> or all properties are
     *         private and had not to be copied
     */
    protected static Dictionary copyTo( Dictionary target, final Dictionary source, final boolean allProps )
    {
        if ( target == null )
        {
            target = new Hashtable();
        }

        if ( source != null && !source.isEmpty() )
        {
            for ( Enumeration ce = source.keys(); ce.hasMoreElements(); )
            {
                // cast is save, because key must be a string as per the spec
                String key = ( String ) ce.nextElement();
                if ( allProps || key.charAt( 0 ) != '.' )
                {
                    target.put( key, source.get( key ) );
                }
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
     */
    void changeState( State newState )
    {
        log( LogService.LOG_DEBUG, "State transition : {0} -> {1}", new Object[]
            { m_state, newState }, null );
        m_state = newState;
    }

    //--------- State classes

    /**
     * There are 12 states in all. They are: Disabled, Unsatisfied,
     * Registered, Factory, Active, Disposed, as well as the transient states
     * Enabling, Activating, Deactivating, Disabling, and Disposing.
     * The Registered, Factory, FactoryInstance and Active states are the
     * "Satisfied" state in concept. The tansient states will be changed to
     * other states automatically when work is done.
     * <p>
     * The transition cases are listed below.
     * <ul>
     * <li>Disabled -(enable/ENABLING) -> Unsatisifed</li>
     * <li>Disabled -(dispose/DISPOSING)-> Disposed</li>
     * <li>Unsatisfied -(activate/ACTIVATING, SUCCESS) -> Satisfied(Registered, Factory or Active)</li>
     * <li>Unsatisfied -(activate/ACTIVATING, FAIL) -> Unsatisfied</li>
     * <li>Unsatisfied -(disable/DISABLING) -> Disabled</li>
     * <li>Registered -(getService, SUCCESS) -> Active</li>
     * <li>Registered -(getService, FAIL) -> Unsatisfied</li>
     * <li>Satisfied -(deactivate/DEACTIVATING)-> Unsatisfied</li>
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


        Object getService( DelayedComponentManager dcm )
        {
            log( dcm, "getService" );
            return null;
        }


        void ungetService( DelayedComponentManager dcm )
        {
            log( dcm, "ungetService" );
        }


        void enable( AbstractComponentManager acm )
        {
            log( acm, "enable" );
        }


        void activate( AbstractComponentManager acm )
        {
            log( acm, "activate" );
        }


        void deactivate( AbstractComponentManager acm, int reason )
        {
            log( acm, "deactivate (reason: " + reason + ")" );
        }


        void disable( AbstractComponentManager acm )
        {
            log( acm, "disable" );
        }


        void dispose( AbstractComponentManager acm )
        {
            log( acm, "dispose" );
        }


        private void log( AbstractComponentManager acm, String event )
        {
            acm.log( LogService.LOG_DEBUG, "Current state: {0}, Event: {1}", new Object[]
                { m_name, event }, null );
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


        void enable( AbstractComponentManager acm )
        {
            acm.changeState( Enabling.getInstance() );

            try
            {
                acm.enableDependencyManagers();
                acm.changeState( Unsatisfied.getInstance() );
                acm.log( LogService.LOG_DEBUG, "Component enabled", null );
            }
            catch ( InvalidSyntaxException ise )
            {
                // one of the reference target filters is invalid, fail
                acm.log( LogService.LOG_ERROR, "Failed enabling Component", ise );
                acm.disableDependencyManagers();
                acm.changeState( Disabled.getInstance() );
            }
        }


        void dispose( AbstractComponentManager acm )
        {
            acm.changeState( Disposing.getInstance() );
            acm.log( LogService.LOG_DEBUG, "Disposing component", null );
            acm.clear();
            acm.changeState( Disposed.getInstance() );

            acm.log( LogService.LOG_DEBUG, "Component disposed", null );
        }
    }

    protected static final class Enabling extends State
    {
        private static final Enabling m_inst = new Enabling();


        private Enabling()
        {
            super( "Enabling", STATE_ENABLING );
        }


        static State getInstance()
        {
            return m_inst;
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


        void activate( AbstractComponentManager acm )
        {
            acm.changeState( Activating.getInstance() );

            acm.log( LogService.LOG_DEBUG, "Activating component", null );

            // Before creating the implementation object, we are going to
            // test if we have configuration if such is required
            if ( !acm.hasConfiguration() && acm.getComponentMetadata().isConfigurationRequired() )
            {
                acm.log( LogService.LOG_INFO, "Missing required configuration, cannot activate", null );
                acm.changeState( Unsatisfied.getInstance() );
                return;
            }

            // Before creating the implementation object, we are going to
            // test if all the mandatory dependencies are satisfied
            if ( !acm.verifyDependencyManagers( acm.getProperties() ) )
            {
                acm.log( LogService.LOG_INFO, "Not all dependencies satisified, cannot activate", null );
                acm.changeState( Unsatisfied.getInstance() );
                return;
            }

            // Before creating the implementation object, we are going to
            // test that the bundle has enough permissions to register services
            if ( !acm.hasServiceRegistrationPermissions() )
            {
                acm.log( LogService.LOG_INFO, "Component is not permitted to register all services, cannot activate",
                    null );
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
                acm.log( LogService.LOG_ERROR, "Component instance could not be created, activation failed", null );

                // set state to unsatisfied
                acm.changeState( Unsatisfied.getInstance() );
                return;
            }

            acm.changeState( acm.getSatisfiedState() );

            acm.registerComponentService();
        }


        void disable( AbstractComponentManager acm )
        {
            acm.changeState( Disabling.getInstance() );

            acm.log( LogService.LOG_DEBUG, "Disabling component", null );

            // dispose and recreate dependency managers
            acm.disableDependencyManagers();

            // we are now disabled, ready for re-enablement or complete destroyal
            acm.changeState( Disabled.getInstance() );

            acm.log( LogService.LOG_DEBUG, "Component disabled", null );
        }
    }

    protected static final class Activating extends State
    {
        private static final Activating m_inst = new Activating();


        private Activating()
        {
            super( "Activating", STATE_ACTIVATING );
        }


        static State getInstance()
        {
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


        void deactivate( AbstractComponentManager acm, int reason )
        {
            acm.changeState( Deactivating.getInstance() );

            acm.log( LogService.LOG_DEBUG, "Deactivating component", null );

            // catch any problems from deleting the component to prevent the
            // component to remain in the deactivating state !
            try
            {
                acm.unregisterComponentService();
                acm.deleteComponent( reason );
            }
            catch ( Throwable t )
            {
                acm.log( LogService.LOG_WARNING, "Component deactivation threw an exception", t );
            }

            acm.changeState( Unsatisfied.getInstance() );
            acm.log( LogService.LOG_DEBUG, "Component deactivated", null );
        }
    }

    /**
     * The <code>Active</code> state is the satisified state of an immediate
     * component after activation. Dealyed and service factory components switch
     * to this state from the {@link Registered} state once the service
     * object has (first) been requested.
     */
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


        void ungetService( DelayedComponentManager dcm )
        {
            dcm.deleteComponent( ComponentConstants.DEACTIVATION_REASON_UNSPECIFIED );
            dcm.changeState( Registered.getInstance() );
        }
    }

    /**
     * The <code>Registered</code> state is the statisfied state of a delayed or
     * service factory component before the actual service instance is
     * (first) retrieved. After getting the actualo service instance the
     * component switches to the {@link Active} state.
     */
    protected static final class Registered extends Satisfied
    {
        private static final Registered m_inst = new Registered();


        private Registered()
        {
            super( "Registered", STATE_REGISTERED );
        }


        static State getInstance()
        {
            return m_inst;
        }


        Object getService( DelayedComponentManager dcm )
        {
            if ( dcm.createRealComponent() )
            {
                dcm.changeState( Active.getInstance() );
                return dcm.getInstance();
            }

            // component could not really be created. This may be temporary
            // so we stay in the registered state but ensure the component
            // instance is deleted
            try
            {
                dcm.deleteComponent( ComponentConstants.DEACTIVATION_REASON_UNSPECIFIED );
            }
            catch ( Throwable t )
            {
                dcm.log( LogService.LOG_DEBUG, "Cannot delete incomplete component instance. Ignoring.", t );
            }

            // no service can be returned (be prepared for more logging !!)
            return null;
        }
    }

    /**
     * The <code>Factory</code> state is the satisfied state of component
     * factory components.
     */
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


    /**
     * The <code>FactoryInstance</code> state is the satisfied state of
     * instances of component factory components created with the
     * <code>ComponentFactory.newInstance</code> method. This state acts the
     * same as the {@link Active} state except that the
     * {@link #deactivate(AbstractComponentManager, int)} switches to the
     * real {@link Active} state before actually disposing off the component
     * because component factory instances are never reactivated after
     * deactivated due to not being satisified any longer. See section 112.5.5,
     * Factory Component, for full details.
     */
    protected static final class FactoryInstance extends Satisfied
    {
        private static final FactoryInstance m_inst = new FactoryInstance();


        private FactoryInstance()
        {
            super( "Active", STATE_ACTIVE );
        }


        static State getInstance()
        {
            return m_inst;
        }


        void deactivate( AbstractComponentManager acm, int reason )
        {
            acm.changeState( Active.getInstance() );
            acm.dispose( reason );
        }
    }

    protected static final class Deactivating extends State
    {
        private static final Deactivating m_inst = new Deactivating();


        private Deactivating()
        {
            super( "Deactivating", STATE_DEACTIVATING );
        }


        static State getInstance()
        {
            return m_inst;
        }
    }

    protected static final class Disabling extends State
    {
        private static final Disabling m_inst = new Disabling();


        private Disabling()
        {
            super( "Disabling", STATE_DISABLING );
        }


        static State getInstance()
        {
            return m_inst;
        }
    }

    protected static final class Disposing extends State
    {
        private static final Disposing m_inst = new Disposing();


        private Disposing()
        {
            super( "Disposing", STATE_DISPOSING );
        }


        static State getInstance()
        {
            return m_inst;
        }
    }

    protected static final class Disposed extends State
    {
        private static final Disposed m_inst = new Disposed();


        private Disposed()
        {
            super( "Disposed", STATE_DISPOSED );
        }


        static State getInstance()
        {
            return m_inst;
        }
    }
}
