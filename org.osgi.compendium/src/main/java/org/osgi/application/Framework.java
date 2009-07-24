/*
 * Copyright (c) OSGi Alliance (2005, 2008). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.application;

import java.util.Hashtable;

/**
 * Using this class, OSGi-aware applications can obtain their
 * {@link ApplicationContext}.
 * 
 * @version $Revision: 5673 $
 */
public final class Framework {

    private Framework() { }
    
    private static Hashtable appContextHash;
    
    /**
     * This method needs an argument, an object that represents the application instance. 
     * An application consists of a set of object, however there is a single object, which 
     * is used by the corresponding application container to manage the lifecycle on the 
     * application instance. The lifetime of this object equals the lifetime of 
     * the application instance; therefore, it is suitable to represent the instance. 
     * <P>
     * The returned {@link ApplicationContext} object is singleton for the 
     * specified application instance. Subsequent calls to this method with the same 
     * application instance must return the same context object
     * 
     * @param applicationInstance is the activator object of an application instance
     * @throws java.lang.NullPointerException If <code>applicationInstance</code>
     *     is <code>null</code>      
     * @throws java.lang.IllegalArgumentException if  called with an object that is not 
     *     the activator object of an application.
     * @return the {@link ApplicationContext} of the specified application instance.
     */
    public static ApplicationContext getApplicationContext(Object applicationInstance) {
    	  if( applicationInstance == null )
    		  throw new NullPointerException( "Instance cannot be null!" );
    	  ApplicationContext appContext = (ApplicationContext)appContextHash.get( applicationInstance );
    	  if( appContext == null )
    		  throw new IllegalArgumentException( "ApplicationContext not found!" );
        return appContext;        
    }
}
