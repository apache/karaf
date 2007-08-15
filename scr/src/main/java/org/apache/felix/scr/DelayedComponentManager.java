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

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * The <code>DelayedComponentManager</code> TODO
 *
 * @author fmeschbe
 * @version $Rev: 538123 $, $Date$
 */
public class DelayedComponentManager extends ImmediateComponentManager implements ServiceFactory
{

    /**
     * @param activator
     * @param metadata
     * @param componentId
     */
    public DelayedComponentManager( BundleComponentActivator activator, ComponentMetadata metadata, long componentId )
    {
        super( activator, metadata, componentId );
    }

    protected void createComponent()
    {
        // nothing to do here for a delayed component, will be done in the
        // getService method for the first bundle acquiring the component
    }

    protected void deleteComponent()
    {
        // only have to delete, if there is actually an instance
        if (this.getInstance() != null) {
            super.deleteComponent();
        }
    }

    protected Object getService()
    {
        return this;
    }

    //---------- ServiceFactory interface -------------------------------------

    public Object getService( Bundle arg0, ServiceRegistration arg1 )
    {
        Activator.trace("DelayedComponentServiceFactory.getService()", this.getComponentMetadata());
        // When the getServiceMethod is called, the implementation object must be created
        // unless another bundle has already retrievd it

        if (this.getInstance() == null) {
            super.createComponent();

            // if component creation failed, we were deactivated and the state
            // is not REGISTERED any more. Otherwise go to standard ACTIVE
            // state now
            if (this.getState() == STATE_REGISTERED)
            {
                this.setState( STATE_ACTIVE );
            }
        }

        return this.getInstance();
    }

    public void ungetService( Bundle arg0, ServiceRegistration arg1, Object arg2 )
    {
        // nothing to do here
    }

}
