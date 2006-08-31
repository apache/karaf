/*
 *   Copyright 2005 The Apache Software Foundation
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

package org.apache.felix.jmood.core;

import java.util.Hashtable;
public interface ManagedServiceMBean {

    /**
     * 
     * @return The registering bundle symbolic name
     */
    public abstract String getBundle();
    
    /**
     * 
     * @return a hashtable containing the same properties contained in
     * the dictionary object used when registering the service. These include
     * the standard mandatory service.id and objectClass properties as
     * defined in the <i>org.osgi.framework.Constants</i> interface
     * @see org.osgi.framework.Constants
     */
    public abstract Hashtable getProperties();

    /**
     * 
     * @return the symbolic names of the bundles using the service
     */
    public abstract String[] getUsingBundles();
    
    public String[] getServiceInterfaces();


}