/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.servicebinder;

import org.osgi.framework.BundleContext;

import java.util.List;

/**
 * The ServiceBinderContext is passed to the objects that implement the services
 * if they implement a constructor that receives a reference of this type. Through
 * this interface, they can access the BundleContext along with the list ob
 * binder instances located on the same bundle.
 *
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public interface ServiceBinderContext
{
    /**
     *
     *@return the Bundle Context of the bundle where the receiver of the context is located
    **/
    BundleContext getBundleContext();

    /**
     *
     *
    **/
    List getInstanceReferences();

    /**
     *
     *
    **/
    InstanceReference getInstanceReference();
}
