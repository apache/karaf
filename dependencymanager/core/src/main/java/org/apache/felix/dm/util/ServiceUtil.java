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
package org.apache.felix.dm.util;

import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * OSGi service utilities.
 */
public class ServiceUtil {
    /**
     * Returns the service ranking of a service, based on its service reference. If
     * the service has a property specifying its ranking, that will be returned. If
     * not, the default ranking of zero will be returned.
     * 
     * @param ref the service reference to determine the ranking for
     * @return the ranking
     */
    public static int getRanking(ServiceReference ref) {
        Integer rank = (Integer) ref.getProperty(Constants.SERVICE_RANKING);
        if (rank != null) {
            return rank.intValue();
        }
        return 0;
    }
    
    /**
     * Returns the service ID of a service, based on its service reference. This
     * method is aware of service aspects as defined by the dependency manager and
     * will return the ID of the orginal service if you give it an aspect.
     * 
     * @param ref the service reference to determine the service ID of
     * @return the service ID
     */
    public static long getServiceId(ServiceReference ref) {
        Long sid = (Long) ref.getProperty(Constants.SERVICE_ID);
        Long aid = (Long) ref.getProperty(DependencyManager.ASPECT);
        if (aid != null) {
            return aid.longValue();
        }
        if (sid != null) {
            return sid.longValue();
        }
        throw new IllegalArgumentException("Invalid service reference, no service ID found");
    }

    /**
     * Determines if the service is an aspect as defined by the dependency manager.
     * Aspects are defined by a property and this method will check for its presence.
     * 
     * @param ref the service reference
     * @return <code>true</code> if it's an aspect, <code>false</code> otherwise
     */
    public static boolean isAspect(ServiceReference ref) {
        Long aid = (Long) ref.getProperty(DependencyManager.ASPECT);
        return (aid != null);
    }
    
    /**
     * Converts a service reference to a string, listing both the bundle it was
     * registered from and all properties.
     * 
     * @param ref the service reference
     * @return a string representation of the service
     */
    public static String toString(ServiceReference ref) {
        if (ref == null) {
            throw new IllegalArgumentException("Service reference cannot be null.");
        }
        StringBuffer buf = new StringBuffer();
        
        buf.append("ServiceReference[" + ref.getBundle().getBundleId() + "]{");
        String[] keys = ref.getPropertyKeys();
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) { 
                buf.append(','); 
            }
            buf.append(keys[i]);
            buf.append('=');
            buf.append(ref.getProperty(keys[i]));
        }
        buf.append("}");
        return buf.toString();
    }
}
