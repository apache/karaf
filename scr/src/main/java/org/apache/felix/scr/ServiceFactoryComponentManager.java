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


import java.util.IdentityHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;


/**
 * The <code>ServiceFactoryComponentManager</code> TODO
 *
 * @author fmeschbe
 * @version $Rev$, $Date$
 */
public class ServiceFactoryComponentManager extends ImmediateComponentManager implements ServiceFactory
{

    // we do not have to maintain references to the actual service
    // instances as those are handled by the ServiceManager and given
    // to the ungetService method when the bundle releases the service

    // maintain the map of componentContext objects created for the
    // service instances
    private IdentityHashMap componentContexts = new IdentityHashMap();


    /**
     * @param activator
     * @param metadata
     */
    public ServiceFactoryComponentManager( BundleComponentActivator activator, ComponentMetadata metadata,
        long componentId )
    {
        super( activator, metadata, componentId );
    }


    /* (non-Javadoc)
     * @see org.apache.felix.scr.AbstractComponentManager#createComponent()
     */
    protected void createComponent()
    {
        // nothing to do, this is handled by getService
    }


    /* (non-Javadoc)
     * @see org.apache.felix.scr.AbstractComponentManager#deleteComponent()
     */
    protected void deleteComponent()
    {
        // nothing to do, this is handled by ungetService
    }


    protected Object getService()
    {
        return this;
    }


    /* (non-Javadoc)
     * @see org.apache.felix.scr.AbstractComponentManager#getInstance()
     */
    public Object getInstance()
    {
        // this method is not expected to be called as the base call is
        // overwritten in the BundleComponentContext class
        return null;
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
     */
    public Object getService( Bundle bundle, ServiceRegistration registration )
    {
        Activator.trace( "DelayedServiceFactoryServiceFactory.getService()", getComponentMetadata() );
        // When the getServiceMethod is called, the implementation object must be created

        // private ComponentContext and implementation instances
        BundleComponentContext componentContext = new BundleComponentContext( this, bundle );
        Object implementationObject = createImplementationObject( componentContext );

        // register the components component context if successfull
        if (implementationObject != null) {
            componentContext.setImplementationObject( implementationObject );
            componentContexts.put( implementationObject, componentContext );

            // if this is the first use of this component, switch to ACTIVE state
            if (getState() == STATE_REGISTERED)
            {
                setState( STATE_ACTIVE );
            }
        }
        
        return implementationObject;
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration, java.lang.Object)
     */
    public void ungetService( Bundle bundle, ServiceRegistration registration, Object service )
    {
        Activator.trace( "DelayedServiceFactoryServiceFactory.ungetService()", getComponentMetadata() );
        // When the ungetServiceMethod is called, the implementation object must be deactivated

        // private ComponentContext and implementation instances
        ComponentContext componentContext = ( ComponentContext ) componentContexts.remove( service );
        deactivateImplementationObject( service, componentContext );
        
        // if this was the last use of the component, go back to REGISTERED state
        if ( componentContexts.isEmpty() && getState() == STATE_ACTIVE )
        {
            setState( STATE_REGISTERED );
        }
    }

    private static class BundleComponentContext extends ComponentContextImpl implements ComponentInstance {
        
        private Bundle m_usingBundle;
        private Object m_implementationObject;

        BundleComponentContext(AbstractComponentManager componentManager, Bundle usingBundle) {
            super(componentManager);
            
            m_usingBundle = usingBundle;
        }
        
        private void setImplementationObject( Object implementationObject )
        {
            m_implementationObject = implementationObject;
        }
        
        public Bundle getUsingBundle()
        {
            return m_usingBundle;
        }
        
        public ComponentInstance getComponentInstance()
        {
            return this;
        }
        
        //---------- ComponentInstance interface ------------------------------
        
        public Object getInstance()
        {
            return m_implementationObject;
        }
        
        public void dispose()
        {
            getComponentManager().dispose();
        }
    }
}
