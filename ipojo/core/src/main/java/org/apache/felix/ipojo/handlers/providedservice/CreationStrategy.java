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
package org.apache.felix.ipojo.handlers.providedservice;

import java.lang.reflect.Method;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.InstanceManager;
import org.osgi.framework.ServiceFactory;

/**
 * Creation strategy to creation service object.
 * This class is extended by all service object
 * creation policy.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class CreationStrategy implements ServiceFactory {
    /**
     * Method called when the service is registered. 
     * @param instance the instance registering the service.
     * @param interfaces the exposed service specification interfaces
     * @param props the published properties. 
     */
    public abstract void onPublication(InstanceManager instance, String[] interfaces, Properties props);
    /**
     * Method called when the service in unregistered. 
     */
    public abstract void onUnpublication();
    
    /**
     * Checks if the given method object is the
     * {@link IPOJOServiceFactory#getService(ComponentInstance)}
     * method.
     * @param method the method to check
     * @return <code>true</code> if the method is the getService method
     * <code>false</code> otherwise.
     */
    public static boolean isGetServiceMethod(Method method) {
        Class[] params = method.getParameterTypes();
        return method.getName().equals("getService") 
            && params.length == 1 
            && params[0].getName().equals(ComponentInstance.class.getName());
    }
    
    /**
     * Checks if the given method object is the
     * {@link IPOJOServiceFactory#ungetService(ComponentInstance, Object)}
     * method.
     * @param method the method to check
     * @return <code>true</code> if the method is the ungetService method
     * <code>false</code> otherwise.
     */
    public static boolean isUngetServiceMethod(Method method) {
        Class[] params = method.getParameterTypes();
        return method.getName().equals("ungetService") 
            && params.length == 2 
            && params[0].getName().equals(ComponentInstance.class.getName());
    }
}
