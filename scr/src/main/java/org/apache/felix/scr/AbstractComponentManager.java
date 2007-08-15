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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentInstance;


/**
 * The default ComponentManager. Objects of this class are responsible for managing
 * implementation object's lifecycle.
 *
 */
abstract class AbstractComponentManager implements ComponentManager, ComponentInstance
{
    // manager has been newly created or disabled
    static final int STATE_DISABLED = 1;

    // manager has just been enabled and is going to be activated
    static final int STATE_ENABLED = 2;

    // manager has been enabled but not satisfied
    static final int STATE_UNSATISFIED = 4;

    // manager is currently activating
    static final int STATE_ACTIVATING = 8;

    // manager is now active
    static final int STATE_ACTIVE = 16;

    // manager for a delayed component has been registered (not active yet)
    static final int STATE_REGISTERED = 32;

    // manager for a component factory has been registered
    static final int STATE_FACTORY = 64;

	// manager is current deactivating
    static final int STATE_DEACTIVATING = 128;

    // manager has been destroyed and may not be used anymore
    static final int STATE_DESTROYED = 256;

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

    /**
     * The constructor receives both the activator and the metadata
     *
     * @param activator
     * @param metadata
     */
    protected AbstractComponentManager(BundleComponentActivator activator, ComponentMetadata metadata)
    {
        this.m_activator = activator;
        this.m_componentMetadata = metadata;

        this.m_state = STATE_DISABLED;
        this.m_dependencyManagers = new ArrayList();

        Activator.trace("Component created", this.m_componentMetadata);
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
    public final void enable() {
        this.getActivator().schedule( new Runnable()
        {
            public void run()
            {
                AbstractComponentManager.this.enableInternal();
            }
        } );
    }

    /**
     * Activates this component if satisfied. If any of the dependencies is
     * not met, the component is not activated and remains unsatisifed.
     * <p>
     * This method schedules the activation for asynchronous execution.
     */
    public final void activate() {
        this.getActivator().schedule( new Runnable()
        {
            public void run()
            {
                AbstractComponentManager.this.activateInternal();
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
        Activator.trace( "Deactivating and Activating to reconfigure", this.m_componentMetadata );
        this.reactivate();
    }

    /**
     * Cycles this component by deactivating it and - if still satisfied -
     * activating it again.
     * <p>
     * This method schedules the reactivation for asynchronous execution.
     */
    public final void reactivate() {
        this.getActivator().schedule( new Runnable()
        {
            public void run()
            {
                AbstractComponentManager.this.deactivateInternal();
                Activator.trace( "Dependency Manager: RECREATING", AbstractComponentManager.this.m_componentMetadata );
                AbstractComponentManager.this.activateInternal();
            }
        } );
    }

    /**
     * Deactivates the component.
     * <p>
     * This method schedules the deactivation for asynchronous execution.
     */
    public final void deactivate() {
        this.getActivator().schedule( new Runnable()
        {
            public void run()
            {
                AbstractComponentManager.this.deactivateInternal();
            }
        } );
    }

    /**
     * Disables this component and - if active - first deactivates it. The
     * component may be reenabled by calling the {@link #enable()} method.
     * <p>
     * This method schedules the disablement for asynchronous execution.
     */
    public final void disable() {
        this.getActivator().schedule( new Runnable()
        {
            public void run()
            {
                AbstractComponentManager.this.disableInternal();
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
    public final void dispose() {
        this.disposeInternal();
    }

    //---------- internal immediate state change methods ----------------------
    // these methods must only be called from a separate thread by calling
    // the respective asynchronous (public) method

    /**
     * Enable this component
     *
     * @return true if enabling was successful
     */
    private void enableInternal() {

        if (this.getState() == STATE_DESTROYED)
        {
            Activator.error( "Destroyed Component cannot be enabled", this.m_componentMetadata );
            return;
        }
        else if (this.getState() != STATE_DISABLED)
        {
            Activator.trace( "Component is already enabled", this.m_componentMetadata );
            return;
        }

        Activator.trace("Enabling component", this.m_componentMetadata);

    	try
    	{
	        // If this component has got dependencies, create dependency managers for each one of them.
	        if (this.m_componentMetadata.getDependencies().size() != 0)
	        {
	            Iterator dependencyit = this.m_componentMetadata.getDependencies().iterator();

	            while(dependencyit.hasNext())
	            {
	                ReferenceMetadata currentdependency = (ReferenceMetadata)dependencyit.next();

	                DependencyManager depmanager = new DependencyManager(this, currentdependency);

	                this.m_dependencyManagers.add(depmanager);
	            }
	        }

            // enter enabled state before trying to activate
	        this.setState( STATE_ENABLED );

            Activator.trace("Component enabled", this.m_componentMetadata);

            // immediately activate the compopnent, no need to schedule again
	        this.activateInternal();
    	}
    	catch(Exception ex)
    	{
    		Activator.exception( "Failed enabling Component", this.m_componentMetadata, ex );

            // ensure we get back to DISABLED state
            // immediately disable, no need to schedule again
            this.disableInternal();
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
         // CONCURRENCY NOTE: This method is called either by the enable()
         //     method or by the dependency managers or the reconfigure() method
         if ( (this.getState() & (STATE_ENABLED|STATE_UNSATISFIED)) == 0)
         {
             // This state can only be entered from the ENABLED (in the enable()
             // method) or UNSATISFIED (missing references) states
             return;
         }

         // go to the activating state
         this.setState(STATE_ACTIVATING);

         Activator.trace("Activating component", this.m_componentMetadata);

         // Before creating the implementation object, we are going to
         // test if all the mandatory dependencies are satisfied
         Iterator it = this.m_dependencyManagers.iterator();
         while (it.hasNext())
         {
             DependencyManager dm = (DependencyManager)it.next();
             if (!dm.isValid())
             {
                 // at least one dependency is not satisfied
                 Activator.trace( "Dependency not satisfied: " + dm.getName(), this.m_componentMetadata );
                 this.setState(STATE_UNSATISFIED);
                 return;
             }
         }

         // 1. Load the component implementation class
         // 2. Create the component instance and component context
         // 3. Bind the target services
         // 4. Call the activate method, if present
         this.createComponent();

         // Validation occurs before the services are provided, otherwhise the
         // service provider's service may be called by a service requester
         // while it is still ACTIVATING
         this.setState(this.getSatisfiedState());

         // 5. Register provided services
         this.m_serviceRegistration = this.registerComponentService();

         Activator.trace("Component activated", this.m_componentMetadata);
     }

     /**
      * This method deactivates the manager, performing the following steps
      *
      * [0. Remove published services from the registry]
      * 1. Call the deactivate() method, if present
      * 2. Unbind any bound services
      * 3. Release references to the component instance and component context
     **/
     private void deactivateInternal()
     {
         // CONCURRENCY NOTE: This method may be called either from application
         // code or by the dependency managers or reconfiguration
         if ((this.getState() & (STATE_ACTIVATING|STATE_ACTIVE|STATE_REGISTERED|STATE_FACTORY)) == 0)
         {
             // This state can only be entered from the ACTIVATING (if activation
             // fails), ACTIVE, REGISTERED or FACTORY states
             return;
         }

         // start deactivation by resetting the state
         this.setState( STATE_DEACTIVATING );

         Activator.trace("Deactivating component", this.m_componentMetadata);

         // 0.- Remove published services from the registry
         this.unregisterComponentService();

         // 1.- Call the deactivate method, if present
         // 2. Unbind any bound services
         // 3. Release references to the component instance and component context
         this.deleteComponent();

         //Activator.trace("InstanceManager from bundle ["+ m_activator.getBundleContext().getBundle().getBundleId() + "] was invalidated.");

         // reset to state UNSATISFIED
         this.setState( STATE_UNSATISFIED );

         Activator.trace("Component deactivated", this.m_componentMetadata);
     }

     private void disableInternal()
     {
         // CONCURRENCY NOTE: This method is only called from the BundleComponentActivator or by application logic
         // but not by the dependency managers

         // deactivate first, this does nothing if not active/registered/factory
         this.deactivateInternal();

         Activator.trace("Disabling component", this.m_componentMetadata);

         // close all service listeners now, they are recreated on enable
         // Stop the dependency managers to listen to events...
         Iterator it = this.m_dependencyManagers.iterator();
         while (it.hasNext())
         {
             DependencyManager dm = (DependencyManager)it.next();
             dm.close();
         }
         this.m_dependencyManagers.clear();

         // we are now disabled, ready for re-enablement or complete destroyal
         this.setState( STATE_DISABLED );

         Activator.trace("Component disabled", this.m_componentMetadata);
     }

     /**
      *
      */
     private void disposeInternal()
     {
         // CONCURRENCY NOTE: This method is only called from the BundleComponentActivator or by application logic
        // but not by the dependency managers

         // disable first to clean up correctly
         this.disableInternal();

         // this component must not be used any more
         this.setState( STATE_DESTROYED );

         // release references (except component metadata for logging purposes)
         this.m_activator = null;
         this.m_dependencyManagers = null;

         Activator.trace("Component disposed", this.m_componentMetadata);
     }

     //---------- Component handling methods ----------------------------------

     /**
      * Method is called by {@link #activate()} in STATE_ACTIVATING or by
      * {@link DelayedComponentManager#getService(Bundle, ServiceRegistration)}
      * in STATE_REGISTERED.
      */
     protected abstract void createComponent();

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
     private int getSatisfiedState() {
         if (this.m_componentMetadata.isFactory())
         {
             return STATE_FACTORY;
         }
         else if (this.m_componentMetadata.isImmediate())
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
         if ( this.getComponentMetadata().getServiceMetadata() != null )
         {
             Activator.trace( "registering services", this.getComponentMetadata() );

             // get a copy of the component properties as service properties
             Dictionary serviceProperties = this.copyTo( null, this.getProperties() );

             return this.getActivator().getBundleContext().registerService(
                 this.getComponentMetadata().getServiceMetadata().getProvides(), this.getService(), serviceProperties );
         }

         return null;
     }

     protected void unregisterComponentService()
     {
         if ( this.m_serviceRegistration != null )
         {
             this.m_serviceRegistration.unregister();
             this.m_serviceRegistration = null;

             Activator.trace( "unregistering the services", this.getComponentMetadata() );
         }
     }

    //**********************************************************************************************************

    BundleComponentActivator getActivator() {
        return this.m_activator;
    }

    Iterator getDependencyManagers() {
        return this.m_dependencyManagers.iterator();
    }

    DependencyManager getDependencyManager( String name )
    {
        Iterator it = this.getDependencyManagers();
        while ( it.hasNext() )
        {
            DependencyManager dm = ( DependencyManager ) it.next();
            if ( name.equals( dm.getName() ) )
            {
                // only return the dm if it has service references
                return ( dm.size() > 0 ) ? dm : null;
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
    protected abstract Dictionary getProperties();

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

    ServiceReference getServiceReference() {
        return ( this.m_serviceRegistration != null ) ? this.m_serviceRegistration.getReference() : null;
    }

    /**
     *
     */
    public ComponentMetadata getComponentMetadata() {
    	return this.m_componentMetadata;
    }

    int getState() {
        return this.m_state;
    }

    /**
     * sets the state of the manager
    **/
    protected synchronized void setState(int newState) {
        Activator.trace( "State transition : " + this.stateToString( this.m_state ) + " -> " + this.stateToString( newState ),
            this.m_componentMetadata );

        this.m_state = newState;
    }

    public String stateToString(int state) {
        switch (state) {
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
                return String.valueOf(state);
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
    static Method getMethod(Class clazz, String name, Class[] parameterTypes)
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
