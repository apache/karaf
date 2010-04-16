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
package org.apache.felix.ipojo.util;

import java.security.Permission;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServicePermission;


/**
 * Methods checking security permissions.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class SecurityHelper {
    
    
    /**
     * Gets a bundle context to register the given services.
     * This method can be used only if iPOJO is able to
     * registers the services (so for ManagedServiceFactory,
     * Factory and Architecture)
     * @param itfs the service interfaces
     * @param comp the component bundle context
     * @param ipojo the ipojo bundle context
     * @return <code>comp</code> if the bundle has enough permission
     * to register the service, <code>ipojo</code> otherwise.
     */
    public static BundleContext selectContextToRegisterServices(String[] itfs,
            BundleContext comp, BundleContext ipojo) {
        if (System.getSecurityManager() != null) {
            for (int i = 0; i < itfs.length; i++) {
                final Permission perm = new ServicePermission(itfs[i],
                        ServicePermission.REGISTER);
                if (!comp.getBundle().hasPermission(perm)) {
                    return ipojo;
                }
            }
            
        }
        return comp;
    }
    
    /**
     * Gets a bundle context to register the given service.
     * This method can be used only if iPOJO is able to
     * registers the service (so for ManagedServiceFactory,
     * Factory and Architecture)
     * @param itf the service interface
     * @param comp the component bundle context
     * @param ipojo the ipojo bundle context
     * @return <code>comp</code> if the bundle has enough permission
     * to register the service, <code>ipojo</code> otherwise.
     */
    public static BundleContext selectContextToRegisterService(String itf,
            BundleContext comp, BundleContext ipojo) {
        if (System.getSecurityManager() != null) {
            final Permission perm = new ServicePermission(itf,
                    ServicePermission.REGISTER);
            if (!comp.getBundle().hasPermission(perm)) {
                return ipojo;
            }
        }
        return comp;
    }
    
    /**
     * Gets a bundle context to get the given service.
     * This method can be used only if iPOJO is able to
     * get the service (so for ManagedServiceFactory,
     * Factory, Architecture and LogService)
     * @param itf the service interface
     * @param comp the component bundle context
     * @param ipojo the ipojo bundle context
     * @return <code>comp</code> if the bundle has enough permission
     * to get the service, <code>ipojo</code> otherwise.
     */
    public static BundleContext selectContextToGetService(String itf,
            BundleContext comp, BundleContext ipojo) {
        if (System.getSecurityManager() != null) {
            final Permission perm = new ServicePermission(itf,
                    ServicePermission.GET);
            if (!comp.getBundle().hasPermission(perm)) {
                return ipojo;
            }
        }
        return comp;
    }
    
    /**
     * Checks if the component bundle context has enough permission
     * to get the given service.
     * @param itf the service interface
     * @param comp the component bundle context
     * @return <code>true</code> if the bundle has enough permission
     * to get the service, <code>false</code> otherwise.
     */
    public static boolean hasPermissionToGetService(String itf,
            BundleContext comp) {
        if (System.getSecurityManager() != null) {
            final Permission perm = new ServicePermission(itf,
                    ServicePermission.GET);
            return comp.getBundle().hasPermission(perm);
        }
        return true;
    } 
    
    /**
     * Checks if the component bundle context has enough permission
     * to get the given services.
     * @param itfs the service interfaces
     * @param comp the component bundle context
     * @return <code>true</code> if the bundle has enough permission
     * to get the services, <code>false</code> otherwise.
     */
    public static boolean hasPermissionToGetServices(String[] itfs,
            BundleContext comp) {
        if (System.getSecurityManager() != null) {
            for (int i = 0; i < itfs.length; i++) {
                final Permission perm = new ServicePermission(itfs[i],
                        ServicePermission.GET);
                if (!comp.getBundle().hasPermission(perm)) {
                    return false;
                }
            }
        }
        return true;
    } 
    
    /**
     * Checks if the component bundle context has enough permission
     * to register the given service.
     * @param itf the service interface
     * @param comp the component bundle context
     * @return <code>true</code> if the bundle has enough permission
     * to register the service, <code>false</code> otherwise.
     */
    public static boolean hasPermissionToRegisterService(String itf,
            BundleContext comp) {
        if (System.getSecurityManager() != null) {
            final Permission perm = new ServicePermission(itf,
                    ServicePermission.REGISTER);
            return comp.getBundle().hasPermission(perm);
        }
        return true;
    }
    
    /**
     * Checks if the component bundle context has enough permission
     * to register the given services.
     * @param itfs the service interfaces
     * @param comp the component bundle context
     * @return <code>true</code> if the bundle has enough permission
     * to register the services, <code>false</code> otherwise.
     */
    public static boolean hasPermissionToRegisterServices(String[] itfs,
            BundleContext comp) {
        if (System.getSecurityManager() != null) {
            for (int i = 0; i < itfs.length; i++) {
                final Permission perm = new ServicePermission(itfs[i],
                        ServicePermission.REGISTER);
                if (!comp.getBundle().hasPermission(perm)) {
                    return false;
                }
            }
        }
        return true;
    }

}
