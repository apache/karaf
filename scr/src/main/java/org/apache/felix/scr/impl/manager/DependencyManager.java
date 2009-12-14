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
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.impl.helper.BindMethod;
import org.apache.felix.scr.impl.helper.UnbindMethod;
import org.apache.felix.scr.impl.helper.UpdatedMethod;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.log.LogService;


/**
 * The <code>DependencyManager</code> manages the references to services
 * declared by a single <code>&lt;reference&gt;</code element in component
 * descriptor.
 */
public class DependencyManager implements ServiceListener, Reference
{
    // mask of states ok to send events
    private static final int STATE_MASK = Component.STATE_UNSATISFIED | Component.STATE_ACTIVATING
        | Component.STATE_ACTIVE | Component.STATE_REGISTERED | Component.STATE_FACTORY;

    // pseudo service to mark a bound service without actual service instance
    private static final Object BOUND_SERVICE_SENTINEL = new Object();

    // the component to which this dependency belongs
    private final AbstractComponentManager m_componentManager;

    // Reference to the metadata
    private final ReferenceMetadata m_dependencyMetadata;

    // The map of bound services indexed by their ServiceReference
    private final Map m_bound;

    // the number of matching services registered in the system
    private int m_size;

    // the object on which the bind/undind methods are to be called
    private transient Object m_componentInstance;

    // the bind method
    private BindMethod m_bind;

    // the updated method
    private UpdatedMethod m_updated;

    // the unbind method
    private UnbindMethod m_unbind;

    // the target service filter string
    private String m_target;

    // the target service filter
    private Filter m_targetFilter;


    /**
     * Constructor that receives several parameters.
     *
     * @param dependency An object that contains data about the dependency
     */
    DependencyManager( AbstractComponentManager componentManager, ReferenceMetadata dependency )
    {
        m_componentManager = componentManager;
        m_dependencyMetadata = dependency;
        m_bound = Collections.synchronizedMap( new HashMap() );

        // setup the target filter from component descriptor
        setTargetFilter( m_dependencyMetadata.getTarget() );

        // dump the reference information if DEBUG is enabled
        if ( m_componentManager.isLogEnabled( LogService.LOG_DEBUG ) )
        {
            m_componentManager
                .log(
                    LogService.LOG_DEBUG,
                    "Dependency Manager {0} created: interface={1}, filter={2}, policy={3}, cardinality={4}, bind={5}, unbind={6}",
                    new Object[]
                        { getName(), dependency.getInterface(), dependency.getTarget(), dependency.getPolicy(),
                            dependency.getCardinality(), dependency.getBind(), dependency.getUnbind() }, null );
        }
    }

    /**
     * Initialize binding methods.
     */
    private void initBindingMethods()
    {
        m_bind = new BindMethod( m_componentManager,
                                 m_dependencyMetadata.getBind(),
                                 m_componentInstance.getClass(),
                                 m_dependencyMetadata.getName(),
                                 m_dependencyMetadata.getInterface()
        );
        m_updated = new UpdatedMethod( m_componentManager,
                m_dependencyMetadata.getUpdated(),
                m_componentInstance.getClass(),
                m_dependencyMetadata.getName(),
                m_dependencyMetadata.getInterface()
        );
        m_unbind = new UnbindMethod( m_componentManager,
            m_dependencyMetadata.getUnbind(),
            m_componentInstance.getClass(),
            m_dependencyMetadata.getName(),
            m_dependencyMetadata.getInterface()
        );
    }



    //---------- ServiceListener interface ------------------------------------

    /**
     * Called when a registered service changes state. In the case of service
     * modification the service is assumed to be removed and added again.
     */
    public void serviceChanged( ServiceEvent event )
    {
        final ServiceReference ref = event.getServiceReference();
        final String serviceString = "Service " + m_dependencyMetadata.getInterface() + "/"
            + ref.getProperty( Constants.SERVICE_ID );

        switch ( event.getType() )
        {
            case ServiceEvent.REGISTERED:
                m_componentManager.log( LogService.LOG_DEBUG, "Dependency Manager: Adding {0}", new Object[]
                    { serviceString }, null );

                // consider the service if the filter matches
                if ( targetFilterMatch( ref ) )
                {
                    m_size++;
                    serviceAdded( ref );
                }
                else
                {
                    m_componentManager.log( LogService.LOG_DEBUG,
                        "Dependency Manager: Ignoring added Service for {0} : does not match target filter {1}",
                        new Object[]
                            { m_dependencyMetadata.getName(), getTarget() }, null );
                }
                break;

            case ServiceEvent.MODIFIED:
                m_componentManager.log( LogService.LOG_DEBUG, "Dependency Manager: Updating {0}", new Object[]
                    { serviceString }, null );

                if ( getBoundService( ref ) == null )
                {
                    // service not currently bound --- what to do ?
                    // if static
                    //    if inactive and target match: activate
                    // if dynamic
                    //    if multiple and target match: bind
                    if ( targetFilterMatch( ref ) )
                    {
                        // new filter match, so increase the counter
                        m_size++;

                        if ( isStatic() )
                        {
                            // if static reference: activate if currentl unsatisifed, otherwise no influence
                            if ( m_componentManager.getState() == AbstractComponentManager.STATE_UNSATISFIED )
                            {
                                m_componentManager.log( LogService.LOG_INFO,
                                    "Dependency Manager: Service {0} registered, activate component", new Object[]
                                        { m_dependencyMetadata.getName() }, null );

                                m_componentManager.activate();
                            }
                        }
                        else if ( isMultiple() )
                        {
                            // if dynamic and multiple reference, bind, otherwise ignore
                            serviceAdded( ref );
                        }
                    }
                }
                else if ( !targetFilterMatch( ref ) )
                {
                    // service reference does not match target any more, remove
                    m_size--;
                    serviceRemoved( ref );
                }
                else
                {
                    // update the service binding due to the new properties
                    update( ref );
                }

                break;

            case ServiceEvent.UNREGISTERING:
                m_componentManager.log( LogService.LOG_DEBUG, "Dependency Manager: Removing {0}", new Object[]
                    { serviceString }, null );

                // manage the service counter if the filter matchs
                if ( targetFilterMatch( ref ) )
                {
                    m_size--;
                }
                else
                {
                    m_componentManager
                        .log(
                            LogService.LOG_DEBUG,
                            "Dependency Manager: Not counting Service for {0} : Service {1} does not match target filter {2}",
                            new Object[]
                                { m_dependencyMetadata.getName(), ref.getProperty( Constants.SERVICE_ID ), getTarget() },
                            null );
                }

                // remove the service ignoring the filter match because if the
                // service is bound, it has to be removed no matter what
                serviceRemoved( ref );

                break;
        }
    }


    /**
     * Called by the {@link #serviceChanged(ServiceEvent)} method if a new
     * service is registered with the system or if a registered service has been
     * modified.
     * <p>
     * Depending on the component state and dependency configuration, the
     * component may be activated, re-activated or the service just be provided.
     *
     * @param reference The reference to the service newly registered or
     *      modified.
     */
    private void serviceAdded( ServiceReference reference )
    {
        // if the component is currently unsatisfied, it may become satisfied
        // by adding this service, try to activate (also schedule activation
        // if the component is pending deactivation)
        if ( m_componentManager.getState() == AbstractComponentManager.STATE_UNSATISFIED )
        {
            m_componentManager.log( LogService.LOG_INFO,
                "Dependency Manager: Service {0} registered, activate component", new Object[]
                    { m_dependencyMetadata.getName() }, null );

            m_componentManager.activate();
        }

        // otherwise check whether the component is in a state to handle the event
        else if ( handleServiceEvent() )
        {

            // FELIX-1413: if the dependency is static and the component is
            // satisfied (active) added services are not considered until
            // the component is reactivated for other reasons.
            if ( m_dependencyMetadata.isStatic() )
            {
                m_componentManager.log( LogService.LOG_DEBUG,
                    "Dependency Manager: Added service {0} is ignored for static reference", new Object[]
                        { m_dependencyMetadata.getName() }, null );
            }

            // otherwise bind if we have a bind method and the service needs
            // be bound
            else if ( m_dependencyMetadata.getBind() != null )
            {
                // multiple bindings or not bound at all yet
                if ( m_dependencyMetadata.isMultiple() || !isBound() )
                {
                    // bind the service, getting it if required
                    invokeBindMethod( reference );
                }
            }
        }

        else
        {
            m_componentManager.log( LogService.LOG_DEBUG,
                "Dependency Manager: Ignoring service addition, wrong state {0}", new Object[]
                    { m_componentManager.state() }, null );
        }
    }


    /**
     * Called by the {@link #serviceChanged(ServiceEvent)} method if an existing
     * service is unregistered from the system or if a registered service has
     * been modified.
     * <p>
     * Depending on the component state and dependency configuration, the
     * component may be deactivated, re-activated, the service just be unbound
     * with or without a replacement service.
     *
     * @param reference The reference to the service unregistering or being
     *      modified.
     */
    private void serviceRemoved( ServiceReference reference )
    {
        // if the dependency is not satisfied anymore, we have to
        // deactivate the component
        if ( !isSatisfied() )
        {
            m_componentManager.log( LogService.LOG_DEBUG,
                "Dependency Manager: Deactivating component due to mandatory dependency on {0}/{1} not satisfied",
                new Object[]
                    { m_dependencyMetadata.getName(), m_dependencyMetadata.getInterface() }, null );

            // deactivate the component now
            m_componentManager.deactivateInternal( ComponentConstants.DEACTIVATION_REASON_REFERENCE );
        }

        // check whether we are bound to that service, do nothing if not
        if ( getBoundService( reference ) == null )
        {
            m_componentManager.log( LogService.LOG_DEBUG,
                "Dependency Manager: Ignoring removed Service for {0} : Service {1} not bound", new Object[]
                    { m_dependencyMetadata.getName(), reference.getProperty( Constants.SERVICE_ID ) }, null );
        }

        // otherwise check whether the component is in a state to handle the event
        else if ( handleServiceEvent() )
        {
            // if the dependency is static, we have to reactivate the component
            // to "remove" the dependency
            if ( m_dependencyMetadata.isStatic() )
            {
                try
                {
                    m_componentManager.log( LogService.LOG_DEBUG,
                        "Dependency Manager: Static dependency on {0}/{1} is broken", new Object[]
                            { m_dependencyMetadata.getName(), m_dependencyMetadata.getInterface() }, null );
                    m_componentManager.deactivateInternal( ComponentConstants.DEACTIVATION_REASON_REFERENCE );
                    m_componentManager.activate();
                }
                catch ( Exception ex )
                {
                    m_componentManager.log( LogService.LOG_ERROR, "Exception while recreating dependency ", ex );
                }
            }

            // dynamic dependency, multiple or single but this service is the bound one
            else
            {

                // try to bind a replacement service first if this is a unary
                // cardinality reference and a replacement is available.
                if ( !m_dependencyMetadata.isMultiple() )
                {
                    // if the dependency is mandatory and no replacement is
                    // available, bind returns false and we deactivate
                    if ( !bind() )
                    {
                        m_componentManager
                            .log(
                                LogService.LOG_DEBUG,
                                "Dependency Manager: Deactivating component due to mandatory dependency on {0}/{1} not satisfied",
                                new Object[]
                                    { m_dependencyMetadata.getName(), m_dependencyMetadata.getInterface() }, null );
                        m_componentManager.deactivateInternal( ComponentConstants.DEACTIVATION_REASON_REFERENCE );
                    }
                }

                // call the unbind method if one is defined
                if ( m_dependencyMetadata.getUnbind() != null )
                {
                    invokeUnbindMethod( reference );
                }

                // make sure the service is returned
                ungetService( reference );
            }
        }

        else
        {
            m_componentManager.log( LogService.LOG_DEBUG,
                "Dependency Manager: Ignoring service removal, wrong state {0}", new Object[]
                    { m_componentManager.state() }, null );
        }
    }


    private boolean handleServiceEvent()
    {
        return ( m_componentManager.getState() & STATE_MASK ) != 0;
    }


    //---------- Reference interface ------------------------------------------

    public String getServiceName()
    {
        return m_dependencyMetadata.getInterface();
    }


    public ServiceReference[] getServiceReferences()
    {
        return getBoundServiceReferences();
    }


    public boolean isOptional()
    {
        return m_dependencyMetadata.isOptional();
    }


    public boolean isMultiple()
    {
        return m_dependencyMetadata.isMultiple();
    }


    public boolean isStatic()
    {
        return m_dependencyMetadata.isStatic();
    }


    public String getBindMethodName()
    {
        return m_dependencyMetadata.getBind();
    }


    public String getUnbindMethodName()
    {
        return m_dependencyMetadata.getUnbind();
    }


    public String getUpdatedMethodName()
    {
        return m_dependencyMetadata.getUpdated();
    }


    //---------- Service tracking support -------------------------------------

    /**
     * Enables this dependency manager by starting to listen for service
     * events.
     * @throws InvalidSyntaxException if the target filter is invalid
     */
    void enable() throws InvalidSyntaxException
    {
        if ( hasGetPermission() )
        {
            // get the current number of registered services available
            ServiceReference refs[] = getFrameworkServiceReferences();
            m_size = ( refs == null ) ? 0 : refs.length;

            // register the service listener
            String filterString = "(" + Constants.OBJECTCLASS + "=" + m_dependencyMetadata.getInterface() + ")";
            m_componentManager.getActivator().getBundleContext().addServiceListener( this, filterString );

            m_componentManager.log( LogService.LOG_DEBUG,
                "Registered for service events, currently {0} service(s) match the filter", new Object[]
                    { new Integer( m_size ) }, null );
        }
        else
        {
            // no services available
            m_size = 0;

            m_componentManager.log( LogService.LOG_DEBUG,
                "Not registered for service events since the bundle has no permission to get service {0}", new Object[]
                    { m_dependencyMetadata.getInterface() }, null );
        }
    }


    /**
     * Disposes off this dependency manager by removing as a service listener
     * and ungetting all services, which are still kept in the list of our
     * bound services. This list will not be empty if the service lookup
     * method is used by the component to access the service.
     */
    void disable()
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

        // reset the target filter from component descriptor
        setTargetFilter( m_dependencyMetadata.getTarget() );
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
    ServiceReference[] getFrameworkServiceReferences()
    {
        return getFrameworkServiceReferences( getTarget() );
    }


    private ServiceReference[] getFrameworkServiceReferences( String targetFilter )
    {
        if ( hasGetPermission() )
        {
            try
            {
                return m_componentManager.getActivator().getBundleContext().getServiceReferences(
                    m_dependencyMetadata.getInterface(), targetFilter );
            }
            catch ( InvalidSyntaxException ise )
            {
                m_componentManager.log( LogService.LOG_ERROR, "Unexpected problem with filter ''{0}''", new Object[]
                    { targetFilter }, ise );
                return null;
            }
        }

        m_componentManager.log( LogService.LOG_DEBUG, "No permission to access the services", null );
        return null;
    }


    /**
     * Returns a <code>ServiceReference</code> instances for a service
     * implementing the interface and complying to the (optional) target filter
     * declared for this dependency. If no matching service can be found
     * <code>null</code> is returned. If the configured target filter is
     * syntactically incorrect an error message is logged with the LogService
     * and <code>null</code> is returned. If multiple matching services are
     * registered the service with the highest service.ranking value is
     * returned. If multiple matching services have the same service.ranking
     * value, the service with the lowest service.id is returned.
     * <p>
     * This method always directly accesses the framework's service registry
     * and ignores the services bound by this dependency manager.
     */
    ServiceReference getFrameworkServiceReference()
    {
        // get the framework registered services and short cut
        ServiceReference[] refs = getFrameworkServiceReferences();
        if ( refs == null )
        {
            return null;
        }
        else if ( refs.length == 1 )
        {
            return refs[0];
        }


        // find the service with the highest ranking
        ServiceReference selectedRef = refs[0];
        for ( int i = 1; i < refs.length; i++ )
        {
            ServiceReference ref = refs[i];
            if ( ref.compareTo( selectedRef ) > 0 )
            {
                selectedRef = ref;
            }
        }

        return selectedRef;
    }


    /**
     * Returns the service instance for the service reference returned by the
     * {@link #getFrameworkServiceReference()} method. If this returns a
     * non-<code>null</code> service instance the service is then considered
     * bound to this instance.
     */
    Object getService()
    {
        ServiceReference sr = getFrameworkServiceReference();
        return ( sr != null ) ? getService( sr ) : null;
    }


    /**
     * Returns an array of service instances for the service references returned
     * by the {@link #getFrameworkServiceReferences()} method. If no services
     * match the criteria configured for this dependency <code>null</code> is
     * returned. All services returned by this method will be considered bound
     * after this method returns.
     */
    Object[] getServices()
    {
        ServiceReference[] sr = getFrameworkServiceReferences();
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
     * services this instance is bound to or <code>null</code> if no services
     * are actually bound.
     */
    private ServiceReference[] getBoundServiceReferences()
    {
        if ( m_bound.isEmpty() )
        {
            return null;
        }

        return ( ServiceReference[] ) m_bound.keySet().toArray( new ServiceReference[m_bound.size()] );
    }


    /**
     * Returns <code>true</code> if at least one service has been bound
     */
    private boolean isBound()
    {
        return !m_bound.isEmpty();
    }


    /**
     * Adds the {@link #BOUND_SERVICE_SENTINEL} object as a pseudo service to
     * the map of bound services. This method allows keeping track of services
     * which have been bound but not retrieved from the service registry, which
     * is the case if the bind method is called with a ServiceReference instead
     * of the service object itself.
     * <p>
     * We have to keep track of all services for which we called the bind
     * method to be able to call the unbind method in case the service is
     * unregistered.
     *
     * @param serviceReference The reference to the service being marked as
     *      bound.
     */
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
    public String getName()
    {
        return m_dependencyMetadata.getName();
    }


    /**
     * Returns <code>true</code> if this dependency manager is satisfied, that
     * is if eithern the dependency is optional or the number of services
     * registered in the framework and available to this dependency manager is
     * not zero.
     */
    public boolean isSatisfied()
    {
        return size() > 0 || m_dependencyMetadata.isOptional();
    }


    /**
     * Returns <code>true</code> if the component providing bundle has permission
     * to get the service described by this reference.
     */
    public boolean hasGetPermission()
    {
        if ( System.getSecurityManager() != null )
        {
            Permission perm = new ServicePermission( getServiceName(), ServicePermission.GET );
            return m_componentManager.getBundle().hasPermission( perm );
        }

        // no security manager, hence permission given
        return true;
    }


    boolean open( Object instance )
    {
        m_componentInstance = instance;
        initBindingMethods();
        return bind();
    }


    /**
     * Revoke all bindings. This method cannot throw an exception since it must
     * try to complete all that it can
     */
    void close( )
    {
        try
        {
            unbind( getBoundServiceReferences() );
        }
        finally
        {
            m_componentInstance = null;
            m_bind = null;
            m_unbind = null;
            m_bound.clear();

        }
    }


    /**
     * initializes a dependency. This method binds all of the service
     * occurrences to the instance object
     *
     * @return true if the dependency is satisfied and at least the minimum
     *      number of services could be bound. Otherwise false is returned.
     */
    private boolean bind()
    {
        // If no references were received, we have to check if the dependency
        // is optional, if it is not then the dependency is invalid
        if ( !isSatisfied() )
        {
            return false;
        }

        // if no bind method is configured or if this is a delayed component,
        // we have nothing to do and just signal success
        if ( m_componentInstance == null || m_dependencyMetadata.getBind() == null )
        {
            return true;
        }

        // assume success to begin with: if the dependency is optional,
        // we don't care, whether we can bind a service. Otherwise, we
        // require at least one service to be bound, thus we require
        // flag being set in the loop below
        boolean success = m_dependencyMetadata.isOptional();

        // Get service reference(s)
        if ( m_dependencyMetadata.isMultiple() )
        {
            // bind all registered services
            ServiceReference[] refs = getFrameworkServiceReferences();
            if ( refs != null )
            {
                for ( int index = 0; index < refs.length; index++ )
                {
                    // success is if we have the minimal required number of services bound
                    if ( invokeBindMethod( refs[index] ) )
                    {
                        // of course, we have success if the service is bound
                        success = true;
                    }
                }
            }
        }
        else
        {
            // bind best matching service
            ServiceReference ref = getFrameworkServiceReference();
            if ( ref != null && invokeBindMethod( ref ) )
            {
                // of course, we have success if the service is bound
                success = true;
            }
        }

        // success will be true, if the service is optional or if at least
        // one service was available to be bound (regardless of whether the
        // bind method succeeded or not)
        return success;
    }


    /**
     * Handles an update in the service reference properties of a bound service.
     * <p>
     * This just calls the {@link #invokeUpdatedMethod(ServiceReference)}
     * method if the method has been configured in the component metadata. If
     * the method is not configured, this method does nothing.
     *
     * @param ref The <code>ServiceReference</code> representing the updated
     *      service.
     */
    private void update( final ServiceReference ref )
    {
        if ( m_dependencyMetadata.getUpdated() != null )
        {
            invokeUpdatedMethod( ref );
        }
    }


    /**
     * Revoke the given bindings. This method cannot throw an exception since
     * it must try to complete all that it can
     */
    private void unbind( ServiceReference[] boundRefs )
    {
        if ( boundRefs != null )
        {
            // only invoke the unbind method if there is an instance (might be null
            // in the delayed component situation) and the unbind method is declared.
            boolean doUnbind = m_componentInstance != null && m_dependencyMetadata.getUnbind() != null;

            for ( int i = 0; i < boundRefs.length; i++ )
            {
                if ( doUnbind )
                {
                    invokeUnbindMethod( boundRefs[i] );
                }

                // unget the service, we call it here since there might be a
                // bind method (or the locateService method might have been
                // called) but there is no unbind method to actually unbind
                // the service (see FELIX-832)
                ungetService( boundRefs[i] );
            }
        }
    }


    /**
     * Calls the bind method. In case there is an exception while calling the
     * bind method, the service is not considered to be bound to the instance
     * object
     * <p>
     * If the reference is singular and a service has already been bound to the
     * component this method has no effect and just returns <code>true</code>.
     *
     * @param ref A ServiceReference with the service that will be bound to the
     *            instance object
     * @return true if the service should be considered bound. If no bind
     *      method is found or the method call fails, <code>true</code> is
     *      returned. <code>false</code> is only returned if the service must
     *      be handed over to the bind method but the service cannot be
     *      retrieved using the service reference.
     */
    private boolean invokeBindMethod( final ServiceReference ref )
    {
        // The bind method is only invoked if the implementation object is not
        // null. This is valid for both immediate and delayed components
        if( m_componentInstance != null )
        {
            return m_bind.invoke(
                m_componentInstance,
                new BindMethod.Service()
                {
                    public ServiceReference getReference()
                    {
                        bindService( ref );
                        return ref;
                    }

                    public Object getInstance()
                    {
                        return getService( ref );
                    }
                }
            );
        }
        else if ( !m_componentManager.getComponentMetadata().isImmediate() )
        {
            m_componentManager.log( LogService.LOG_DEBUG,
                "DependencyManager : Delayed component not yet created, assuming bind method call succeeded",
                null );

            return true;
        }
        else if ( m_componentManager.getState() == AbstractComponentManager.STATE_ACTIVATING )
        {
            // when activating the method, events may be handled before the
            // open(Object) method has been called on this instance. This is
            // not a problem, because the open(Object) method will catch up
            // this services any way
            m_componentManager.log( LogService.LOG_DEBUG, "DependencyManager : Not yet open for activating component",
                null );

            return true;
        }
        else
        {
            // this is not expected: if the component is immediate the
            // implementationObject is not null (asserted by the caller)

            m_componentManager.log( LogService.LOG_ERROR,
                "DependencyManager : Immediate component not yet created, bind method cannot be called",
                null );

            return false;
        }
    }


    /**
     * Calls the updated method.
     *
     * @param ref A service reference corresponding to the service whose service
     *      registration properties have been updated
     */
    private void invokeUpdatedMethod( final ServiceReference ref )
    {
        // The updated method is only invoked if the implementation object is not
        // null. This is valid for both immediate and delayed components
        if ( m_componentInstance != null )
        {
            m_updated.invoke( m_componentInstance, new BindMethod.Service()
            {
                public ServiceReference getReference()
                {
                    return ref;
                }


                public Object getInstance()
                {
                    return getService( ref );
                }
            } );
        }
        else
        {
            // don't care whether we can or cannot call the unbind method
            // if the component instance has already been cleared by the
            // close() method
            m_componentManager.log( LogService.LOG_DEBUG,
                "DependencyManager : Component not set, no need to call updated method", null );
        }
    }


    /**
     * Calls the unbind method.
     * <p>
     * If the reference is singular and the given service is not the one bound
     * to the component this method has no effect and just returns
     * <code>true</code>.
     *
     * @param ref A service reference corresponding to the service that will be
     *            unbound
     */
    private void invokeUnbindMethod( final ServiceReference ref )
    {
        // The unbind method is only invoked if the implementation object is not
        // null. This is valid for both immediate and delayed components
        if ( m_componentInstance != null )
        {
            m_unbind.invoke(
                m_componentInstance,
                new BindMethod.Service()
                {
                    public ServiceReference getReference()
                    {
                        return ref;
                    }

                    public Object getInstance()
                    {
                        return getService( ref );
                    }
                }
            );
        }
        else
        {
            // don't care whether we can or cannot call the unbind method
            // if the component instance has already been cleared by the
            // close() method
            m_componentManager.log( LogService.LOG_DEBUG,
                "DependencyManager : Component not set, no need to call unbind method", null );
        }
    }


    //------------- Service target filter support -----------------------------

    /**
     * Returns <code>true</code> if the <code>properties</code> can be
     * dynamically applied to the component to which the dependency manager
     * belongs.
     * <p>
     * This method applies the following heuristics (in the given order):
     * <ol>
     * <li>If there is no change in the target filter for this dependency, the
     * properties can be applied</li>
     * <li>If the dependency is static and there are changes in the target
     * filter we cannot dynamically apply the configuration because the filter
     * may (assume they do for simplicity here) cause the bindings to change.</li>
     * <li>If there is still at least one service matching the new target filter
     * we can apply the configuration because the depdency is dynamic.</li>
     * <li>If there are no more services matching the filter, we can still
     * apply the configuration if the dependency is optional.</li>
     * <li>Ultimately, if all other checks do not apply we cannot dynamically
     * apply.</li>
     * </ol>
     */
    boolean canUpdateDynamically( Dictionary properties )
    {
        // 1. no target filter change
        final String newTarget = ( String ) properties.get( m_dependencyMetadata.getTargetPropertyName() );
        final String currentTarget = getTarget();
        if ( ( currentTarget == null && newTarget == null )
            || ( currentTarget != null && currentTarget.equals( newTarget ) ) )
        {
            // can update if target filter is not changed, since there is
            // no change is service binding
            return true;
        }
        // invariant: target filter change

        // 2. if static policy, cannot update dynamically
        // (for simplicity assuming change in target service binding)
        if ( m_dependencyMetadata.isStatic() )
        {
            // cannot update if services are statically bound and the target
            // filter is modified, since there is (potentially at least)
            // a change is service bindings
            return false;
        }
        // invariant: target filter change + dynamic policy

        // 3. check target services matching the new filter
        ServiceReference[] refs = getFrameworkServiceReferences( newTarget );
        if ( refs != null && refs.length > 0 )
        {
            // can update since there is at least on service matching the
            // new target filter and the services may be exchanged dynamically
            return true;
        }
        // invariant: target filter change + dynamic policy + no more matching service

        // 4. check optionality
        if ( m_dependencyMetadata.isOptional() )
        {
            // can update since even if no service matches the new filter, this
            // makes no difference because the dependency is optional
            return true;
        }
        // invariant: target filter change + dynamic policy + no more matching service + required

        // 5. cannot dynamically update because the target filter results in
        // no more applicable services which is not acceptable
        return false;
    }


    /**
     * Sets the target filter from target filter property contained in the
     * properties. The filter is taken from a property whose name is derived
     * from the dependency name and the suffix <code>.target</code> as defined
     * for target properties on page 302 of the Declarative Services
     * Specification, section 112.6.
     *
     * @param properties The properties containing the optional target service
     *      filter property
     */
    void setTargetFilter( Dictionary properties )
    {
        setTargetFilter( ( String ) properties.get( m_dependencyMetadata.getTargetPropertyName() ) );
    }


    /**
     * Sets the target filter of this dependency to the new filter value. If the
     * new target filter is the same as the old target filter, this method has
     * not effect. Otherwise any services currently bound but not matching the
     * new filter are unbound. Likewise any registered services not currently
     * bound but matching the new filter are bound.
     *
     * @param target The new target filter to be set. This may be
     *      <code>null</code> if no target filtering is to be used.
     */
    private void setTargetFilter( String target )
    {
        // do nothing if target filter does not change
        if ( ( m_target == null && target == null ) || ( m_target != null && m_target.equals( target ) ) )
        {
            return;
        }

        m_target = target;
        if ( target != null )
        {
            try
            {
                m_targetFilter = m_componentManager.getActivator().getBundleContext().createFilter( target );
            }
            catch ( InvalidSyntaxException ise )
            {
                // log
                m_targetFilter = null;
            }
        }
        else
        {
            m_targetFilter = null;
        }

        // check for services to be removed
        if ( m_targetFilter != null )
        {
            ServiceReference[] refs = getBoundServiceReferences();
            if ( refs != null )
            {
                for ( int i = 0; i < refs.length; i++ )
                {
                    if ( !m_targetFilter.match( refs[i] ) )
                    {
                        // might want to do this asynchronously ??
                        serviceRemoved( refs[i] );
                    }
                }
            }
        }

        // check for new services to be added and set the number of
        // matching services
        ServiceReference[] refs = getFrameworkServiceReferences();
        if ( refs != null )
        {
            for ( int i = 0; i < refs.length; i++ )
            {
                if ( getBoundService( refs[i] ) == null )
                {
                    // might want to do this asynchronously ??
                    serviceAdded( refs[i] );
                }
            }
            m_size = refs.length;
        }
        else
        {
            // no services currently match the filter
            m_size = 0;
        }
    }


    /**
     * Returns the target filter of this dependency as a string or
     * <code>null</code> if this dependency has no target filter set.
     *
     * @return The target filter of this dependency or <code>null</code> if
     *      none is set.
     */
    public String getTarget()
    {
        return m_target;
    }


    /**
     * Checks whether the service references matches the target filter of this
     * dependency.
     *
     * @param ref The service reference to check
     * @return <code>true</code> if this dependency has no target filter or if
     *      the target filter matches the service reference.
     */
    private boolean targetFilterMatch( ServiceReference ref )
    {
        return m_targetFilter == null || m_targetFilter.match( ref );
    }
}
